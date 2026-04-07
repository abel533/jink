package io.mybatis.jink;

import io.mybatis.jink.component.Component;
import io.mybatis.jink.component.Focusable;
import io.mybatis.jink.component.FocusManager;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.dom.ElementNode;
import io.mybatis.jink.input.Key;
import io.mybatis.jink.input.KeyParser;
import io.mybatis.jink.layout.FlexLayout;
import io.mybatis.jink.render.NodeRenderer;
import io.mybatis.jink.render.TerminalWriter;
import io.mybatis.jink.render.VirtualScreen;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.Attributes;
import org.jline.utils.NonBlockingReader;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Jink 框架主入口。
 * 对应 ink 的 render() 函数。
 * <p>
 * 使用示例:
 * <pre>
 * // 渲染为字符串（测试用）
 * String output = Ink.renderToString(Box.of(Text.of("Hello")), 80, 24);
 *
 * // 交互式渲染到终端（JLine 3 raw mode）
 * Ink.Instance app = Ink.render(new MyComponent());
 * app.waitUntilExit();
 * </pre>
 */
public class Ink {

    // ===== 静态渲染 API（无终端交互）=====

    /**
     * 将可渲染内容渲染为 ANSI 字符串（适合测试和无交互场景）
     */
    public static String renderToString(Renderable renderable, int width, int height) {
        ElementNode root = buildDomTree(renderable, width);
        VirtualScreen screen = NodeRenderer.render(root);
        return screen.render();
    }

    /**
     * 将可渲染内容渲染到标准输出（静态模式，一次性输出）
     */
    public static void renderOnce(Renderable renderable, int width, int height) {
        renderOnce(renderable, width, height, System.out);
    }

    /**
     * 将可渲染内容渲染到指定输出流
     */
    public static void renderOnce(Renderable renderable, int width, int height, PrintStream out) {
        String output = renderToString(renderable, width, height);
        out.print(output);
        out.flush();
    }

    // ===== 交互式渲染 API（JLine 3 终端）=====

    /**
     * 交互式渲染（自动检测终端尺寸）
     */
    /**
     * 交互式渲染到终端（默认 Ctrl+C 退出）
     */
    public static Instance render(Renderable renderable) {
        return new Instance(renderable, -1, -1, true, true);
    }

    /**
     * 交互式渲染到终端，可配置 Ctrl+C 行为
     *
     * @param exitOnCtrlC true=Ctrl+C 退出应用（默认），false=将 Ctrl+C 传递给组件处理
     */
    public static Instance render(Renderable renderable, boolean exitOnCtrlC) {
        return new Instance(renderable, -1, -1, true, exitOnCtrlC);
    }

    /**
     * 交互式渲染（指定尺寸，不使用终端 raw mode）
     */
    public static Instance render(Renderable renderable, int width, int height) {
        return new Instance(renderable, width, height, false, true);
    }

    /**
     * 构建 DOM 树并执行布局
     */
    static ElementNode buildDomTree(Renderable renderable, int width) {
        ElementNode root = ElementNode.createRoot();
        ElementNode content = renderable.toNode();
        root.appendChild(content);
        FlexLayout.calculateLayout(root, width);
        return root;
    }

    // ===== Ink 实例 =====

    /**
     * Ink 实例，管理渲染生命周期和终端 I/O。
     * 对应 ink 的 Ink 类。
     */
    public static class Instance {
        private static final int DEFAULT_WIDTH = 80;
        private static final int DEFAULT_HEIGHT = 24;

        private final Renderable rootRenderable;
        private volatile boolean running = true;
        private volatile boolean renderDirty = false;
        private int width;
        private int height;

        // 非交互模式（测试用）
        private PrintStream simpleOutput;
        private TerminalWriter simpleWriter;

        // 交互模式（JLine 3）
        private Terminal terminal;
        private Attributes savedAttributes;
        private NonBlockingReader reader;
        private PrintWriter termWriter;
        private boolean interactive;
        private boolean mouseTrackingEnabled;

        // 渲染节流
        private long lastRenderTime = 0;
        private int maxFps = 30;

        // 输出缓存
        private String lastOutput = "";
        private int lastLineCount = 0;
        private String[] previousRenderedLines = new String[0];

        // 清理状态
        private volatile boolean cleaned = false;

        // 焦点管理
        private final FocusManager focusManager = new FocusManager();

        // Ctrl+C 行为：true=退出应用，false=传递给组件
        private final boolean exitOnCtrlC;

        /**
         * 构造实例
         *
         * @param useTerminal  true=使用 JLine 3 终端（raw mode），false=使用 PrintStream
         * @param exitOnCtrlC  true=Ctrl+C 退出应用，false=将 Ctrl+C 传递给组件
         */
        Instance(Renderable renderable, int width, int height, boolean useTerminal, boolean exitOnCtrlC) {
            this.rootRenderable = renderable;
            this.exitOnCtrlC = exitOnCtrlC;

            if (useTerminal) {
                initTerminal();
            } else {
                this.width = width;
                this.height = height;
                this.interactive = false;
                this.simpleOutput = System.out;
                this.simpleWriter = new TerminalWriter(simpleOutput);
            }

            // 注册有状态组件的回调
            if (renderable instanceof Component) {
                Component<?> component = (Component<?>) renderable;
                component.setOnStateChange(this::markDirty);
                component.onMount();
            }

            // 注册可聚焦组件
            if (renderable instanceof Focusable) {
                focusManager.register((Focusable) renderable);
            }

            // 首次渲染
            rerender();
        }

        /**
         * 初始化 JLine 3 终端。
         * 尝试获取系统终端（raw mode），失败时回退到 dumb 终端模式。
         */
        private void initTerminal() {
            try {
                terminal = TerminalBuilder.builder()
                        .system(true)
                        .build();

                String termType = terminal.getType();
                boolean isDumb = "dumb".equals(termType) || "dumb-color".equals(termType);

                Size size = terminal.getSize();
                this.width = size.getColumns() > 0 ? size.getColumns() : DEFAULT_WIDTH;
                this.height = size.getRows() > 0 ? size.getRows() : DEFAULT_HEIGHT;

                if (isDumb) {
                    // dumb 终端：无法 raw mode，回退到简单输出 + 行缓冲输入
                    System.err.println("[jink] 警告: 终端不支持 raw mode（类型: " + termType
                            + "），使用简单输出模式。");
                    System.err.println("[jink] 提示: 请在真实终端中运行，并添加 JVM 参数: "
                            + "--enable-native-access=ALL-UNNAMED");
                    this.interactive = false;
                    this.simpleOutput = System.out;
                    this.simpleWriter = new TerminalWriter(simpleOutput);
                    return;
                }

                this.interactive = true;

                // 进入 raw mode
                savedAttributes = terminal.enterRawMode();
                reader = terminal.reader();
                termWriter = terminal.writer();

                // 进入备用屏幕缓冲区（退出后不留渲染痕迹）
                termWriter.print("\u001B[?1049h");
                // 清屏 + 光标归位（确保备用缓冲区干净，防止首次渲染滚动）
                termWriter.print("\u001B[2J");
                termWriter.print("\u001B[H");
                // 隐藏光标
                termWriter.print("\u001B[?25l");
                // 启用 Bracketed Paste Mode
                termWriter.print(io.mybatis.jink.ansi.Ansi.ENABLE_BRACKETED_PASTE);
                termWriter.flush();

                try {
                    mouseTrackingEnabled = terminal.trackMouse(Terminal.MouseTracking.Normal);
                } catch (Exception ignored) {
                    mouseTrackingEnabled = false;
                }

                // 监听终端尺寸变化
                terminal.handle(Terminal.Signal.WINCH, signal -> {
                    Size newSize = terminal.getSize();
                    this.width = newSize.getColumns() > 0 ? newSize.getColumns() : this.width;
                    this.height = newSize.getRows() > 0 ? newSize.getRows() : this.height;
                    markDirty();
                });

                // 监听 INT 信号（Ctrl+C 可能绕过 raw mode 输入）
                // Windows Console 可能将 Ctrl+C 作为 CTRL_C_EVENT 信号处理，
                // 而非将 0x03 字节传递给 raw mode 输入。
                // 因此需要在信号处理器中手动分发 Ctrl+C 到组件。
                terminal.handle(Terminal.Signal.INT, signal -> {
                    if (exitOnCtrlC) {
                        running = false;
                    } else {
                        // 手动构造 Ctrl+C 事件并分发到组件
                        if (rootRenderable instanceof Component) {
                            KeyParser.ParseResult result = KeyParser.parseControlChar(0x03);
                            ((Component<?>) rootRenderable).onInput(result.inputText(), result.toKey());
                        }
                    }
                });

                // JVM 关闭钩子：确保终端状态恢复（Ctrl+C/SIGTERM 等异常退出时）
                Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup, "jink-cleanup"));
            } catch (IOException e) {
                // 终端初始化完全失败：回退到纯 PrintStream 模式
                System.err.println("[jink] 终端初始化失败: " + e.getMessage()
                        + "，回退到简单输出模式。");
                this.width = DEFAULT_WIDTH;
                this.height = DEFAULT_HEIGHT;
                this.interactive = false;
                this.simpleOutput = System.out;
                this.simpleWriter = new TerminalWriter(simpleOutput);
            }
        }

        /**
         * 标记需要重渲染（交互模式下由事件循环处理，非交互模式立即渲染）
         */
        public void markDirty() {
            this.renderDirty = true;
            if (!interactive) {
                rerender();
            }
        }

        /**
         * 设置最大帧率
         */
        public Instance maxFps(int fps) {
            this.maxFps = fps;
            return this;
        }

        /**
         * 设置输出尺寸（非交互模式）
         */
        public Instance size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        /**
         * 设置输出流（非交互模式）
         */
        public Instance output(PrintStream output) {
            this.simpleOutput = output;
            this.simpleWriter = new TerminalWriter(output);
            return this;
        }

        /**
         * 执行渲染
         */
        public void rerender() {
            if (!running) return;

            // 通知组件当前终端尺寸
            if (rootRenderable instanceof Component) {
                ((Component<?>) rootRenderable).setTerminalSize(width, height);
            }

            ElementNode root = buildDomTree(rootRenderable, width);
            VirtualScreen screen = NodeRenderer.render(root);
            String newOutput = screen.render();

            if (!newOutput.equals(lastOutput)) {
                if (interactive && termWriter != null) {
                    writeToTerminal(newOutput);
                } else if (simpleWriter != null) {
                    simpleWriter.render(newOutput);
                }
                lastOutput = newOutput;
            }

            // 光标定位（每次渲染后都需要更新，即使输出没变）
            if (interactive && termWriter != null && rootRenderable instanceof Component) {
                Component<?> comp = (Component<?>) rootRenderable;
                int cRow = comp.getCursorRow();
                int cCol = comp.getCursorCol();
                if (cRow >= 0 && cCol >= 0) {
                    // ANSI 光标定位是 1-indexed
                    termWriter.print("\u001B[" + (cRow + 1) + ";" + (cCol + 1) + "H");
                    termWriter.print("\u001B[?25h"); // 显示光标
                    termWriter.flush();
                } else {
                    termWriter.print("\u001B[?25l"); // 隐藏光标
                    termWriter.flush();
                }
            }

            renderDirty = false;
        }

        /**
         * 写入终端（差异化更新）。
         * 在备用屏幕缓冲区中使用绝对定位从左上角开始写入。
         */
        private void writeToTerminal(String output) {
            StringBuilder sb = new StringBuilder();

            String[] lines = output.split("\n", -1);
            int maxLines = Math.min(lines.length, height);

            // 差异化行更新：仅输出有变化的行
            for (int i = 0; i < maxLines; i++) {
                String newLine = lines[i];
                String oldLine = i < previousRenderedLines.length ? previousRenderedLines[i] : null;

                if (!newLine.equals(oldLine)) {
                    // ANSI 行号从 1 开始：\e[row;1H 定位到第 row 行第 1 列
                    sb.append("\u001B[").append(i + 1).append(";1H");
                    sb.append("\u001B[2K"); // 清除整行
                    sb.append(newLine);
                }
            }

            // 清除剩余旧内容（当新输出比旧输出少行时）
            for (int i = maxLines; i < lastLineCount; i++) {
                sb.append("\u001B[").append(i + 1).append(";1H");
                sb.append("\u001B[2K");
            }

            if (sb.length() > 0) {
                termWriter.print(sb);
                termWriter.flush();
            }

            previousRenderedLines = lines;
            lastLineCount = maxLines;
        }

        /**
         * 事件循环：读取输入 + 渲染（交互模式）
         */
        public void waitUntilExit() {
            if (!interactive) {
                // 非交互模式：简单等待
                while (running) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                return;
            }

            // 交互模式：事件循环
            try {
                long minFrameInterval = 1000 / maxFps;

                while (running) {
                    // 读取输入（非阻塞，16ms 超时 ≈ 60Hz 轮询）
                    readAndDispatchInput();

                    // 渲染节流
                    if (renderDirty) {
                        long now = System.currentTimeMillis();
                        if (now - lastRenderTime >= minFrameInterval) {
                            rerender();
                            lastRenderTime = now;
                        }
                    }
                }
            } finally {
                cleanup();
            }
        }

        /**
         * 读取并分发输入事件
         */
        private void readAndDispatchInput() {
            try {
                int ch = reader.read(16); // 16ms 超时
                if (ch < 0) return; // 无输入或超时

                KeyParser.ParseResult result;

                if (ch == 0x1b) {
                    // ESC 键或转义序列
                    result = readEscapeSequence();
                } else if (ch < 0x20 || ch == 0x7f) {
                    // 控制字符
                    result = KeyParser.parseControlChar(ch);
                } else {
                    // 普通可打印字符
                    result = KeyParser.parseChar(ch);
                }

                if (result != null) {
                    dispatchInput(result);
                }
            } catch (IOException e) {
                // 读取失败，忽略
            }
        }

        /**
         * 读取 ESC 转义序列。
         * 鼠标事件和功能键都可能跨多个字节到达，需要等待完整序列。
         */
        private KeyParser.ParseResult readEscapeSequence() throws IOException {
            // 第一步：等待 ESC 后的第一个字符（区分独立 ESC 键和序列开始）
            int next = reader.read(150);
            if (next < 0) {
                return KeyParser.parseControlChar(0x1b);
            }

            // Meta+Enter (ESC + CR/LF)
            if (next == '\r' || next == '\n') {
                return KeyParser.parseEscapeSequence(String.valueOf((char) next));
            }

            // 非 CSI/SS3 开头：Meta+字符，立即返回
            if (next != '[' && next != 'O') {
                return KeyParser.parseEscapeSequence(String.valueOf((char) next));
            }

            // CSI (ESC[) 或 SS3 (ESCO) 序列开始，后续字节应该紧随其后
            StringBuilder seq = new StringBuilder();
            seq.append((char) next);

            while (!KeyParser.isCompleteSequence(seq.toString())) {
                // CSI 序列已确认，使用较长超时读取剩余字节
                int more = reader.read(300);
                if (more < 0) break;
                seq.append((char) more);
                if (seq.length() > 20) break;
            }

            // 检测 Bracketed Paste 起始标记 [200~
            if ("[200~".equals(seq.toString())) {
                return readPasteContent();
            }

            return KeyParser.parseEscapeSequence(seq.toString());
        }

        /**
         * 读取 Bracketed Paste 内容（从 [200~ 到 [201~ 之间的所有数据）
         */
        private KeyParser.ParseResult readPasteContent() throws IOException {
            StringBuilder pasteBuffer = new StringBuilder();
            // 读取直到遇到 ESC[201~ (粘贴结束标记)
            // 最大缓冲 64KB 防止异常输入
            while (pasteBuffer.length() < 65536) {
                int ch = reader.read(1000); // 粘贴数据应该快速到达
                if (ch < 0) break;

                if (ch == 0x1b) {
                    // 可能是结束标记 ESC[201~
                    StringBuilder endSeq = new StringBuilder();
                    for (int i = 0; i < 5; i++) { // "[201~" 长度为 5
                        int more = reader.read(100);
                        if (more < 0) break;
                        endSeq.append((char) more);
                    }
                    if ("[201~".equals(endSeq.toString())) {
                        break; // 粘贴结束
                    }
                    // 不是结束标记，将已读字符加入缓冲
                    pasteBuffer.append('\u001B');
                    pasteBuffer.append(endSeq);
                } else {
                    pasteBuffer.append((char) ch);
                }
            }

            return KeyParser.pasteResult(pasteBuffer.toString());
        }

        /**
         * 分发输入事件到组件
         */
        private void dispatchInput(KeyParser.ParseResult result) {
            Key key = result.toKey();
            String input = result.inputText();

            // 内置处理：Ctrl+C 退出（可配置）
            if ("c".equals(result.name()) && result.ctrl()) {
                if (exitOnCtrlC) {
                    exit();
                    return;
                }
                // exitOnCtrlC=false 时，将 Ctrl+C 传递给组件处理
            }

            // 内置处理：Tab 焦点导航
            if ("tab".equals(result.name())) {
                if (key.shift()) {
                    focusManager.focusPrevious();
                } else {
                    focusManager.focusNext();
                }
                markDirty();
                return;
            }

            // 分发到组件
            if (rootRenderable instanceof Component) {
                Component<?> component = (Component<?>) rootRenderable;
                if (result.isPaste()) {
                    component.onPaste(input);
                } else {
                    component.onInput(input, key);
                }
            }
        }

        /**
         * 退出应用
         */
        public void exit() {
            running = false;
            if (rootRenderable instanceof Component) {
                ((Component<?>) rootRenderable).onUnmount();
            }
        }

        // 拦截的 console 输出缓冲
        private final java.util.List<String> interceptedOutput = new java.util.concurrent.CopyOnWriteArrayList<>();

        /**
         * 启用 console 拦截，防止 System.out/err 干扰 TUI 渲染。
         * 拦截到的输出可通过 getInterceptedOutput() 获取。
         * 对应 ink 的 patchConsole。
         */
        public void patchConsole() {
            io.mybatis.jink.util.ConsolePatcher.patch(text -> {
                interceptedOutput.add(text);
                markDirty();
            });
        }

        /**
         * 获取并清空拦截到的 console 输出
         */
        public java.util.List<String> drainInterceptedOutput() {
            java.util.List<String> drained = new java.util.ArrayList<>(interceptedOutput);
            interceptedOutput.clear();
            return drained;
        }

        /**
         * 清理终端资源（幂等，可多次调用）
         */
        private synchronized void cleanup() {
            if (cleaned) return;
            cleaned = true;

            // 恢复 console 拦截
            io.mybatis.jink.util.ConsolePatcher.restore();

            if (terminal != null) {
                try {
                    if (mouseTrackingEnabled) {
                        terminal.trackMouse(Terminal.MouseTracking.Off);
                    }
                    // 显式禁用所有鼠标跟踪模式（确保 JLine 内部状态与终端一致）
                    // 部分终端在启用 Normal 模式时同时启用 SGR 扩展，必须单独关闭
                    termWriter.print("\u001B[?1000l"); // normal mouse tracking off
                    termWriter.print("\u001B[?1002l"); // button-event tracking off
                    termWriter.print("\u001B[?1003l"); // any-event tracking off
                    termWriter.print("\u001B[?1006l"); // SGR extended mouse off
                    termWriter.print("\u001B[?1015l"); // URXVT extended mouse off
                    // 禁用 Bracketed Paste Mode
                    termWriter.print(io.mybatis.jink.ansi.Ansi.DISABLE_BRACKETED_PASTE);
                    // 显示光标
                    termWriter.print("\u001B[?25h");
                    // 离开备用屏幕缓冲区（恢复原始终端内容）
                    termWriter.print("\u001B[?1049l");
                    termWriter.flush();

                    // 恢复终端属性
                    if (savedAttributes != null) {
                        terminal.setAttributes(savedAttributes);
                    }

                    terminal.close();
                } catch (Exception e) {
                    // 忽略清理错误
                }
            }
        }

        public boolean isRunning() {
            return running;
        }

        public String getLastOutput() {
            return lastOutput;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        /**
         * 获取焦点管理器
         */
        public FocusManager getFocusManager() {
            return focusManager;
        }

        /**
         * 直接向终端写入原始转义序列（绕过渲染引擎）。
         * 用于设置终端标题等不可见的控制序列。
         * 对应 ink 的 TerminalWriteContext / writeRaw。
         */
        public void writeRaw(String escapeSequence) {
            if (escapeSequence == null || escapeSequence.isEmpty()) return;
            if (interactive && termWriter != null) {
                termWriter.print(escapeSequence);
                termWriter.flush();
            }
        }
    }
}
