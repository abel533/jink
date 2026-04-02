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
    public static Instance render(Renderable renderable) {
        return new Instance(renderable, -1, -1, true);
    }

    /**
     * 交互式渲染（指定尺寸，不使用终端 raw mode）
     */
    public static Instance render(Renderable renderable, int width, int height) {
        return new Instance(renderable, width, height, false);
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

        // 渲染节流
        private long lastRenderTime = 0;
        private int maxFps = 30;

        // 输出缓存
        private String lastOutput = "";
        private int lastLineCount = 0;

        // 焦点管理
        private final FocusManager focusManager = new FocusManager();

        /**
         * 构造实例
         *
         * @param useTerminal true=使用 JLine 3 终端（raw mode），false=使用 PrintStream
         */
        Instance(Renderable renderable, int width, int height, boolean useTerminal) {
            this.rootRenderable = renderable;

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
            if (renderable instanceof Component<?> component) {
                component.setOnStateChange(this::markDirty);
                component.onMount();
            }

            // 注册可聚焦组件
            if (renderable instanceof Focusable focusable) {
                focusManager.register(focusable);
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

                // 隐藏光标
                termWriter.print("\u001B[?25l");
                termWriter.flush();

                // 监听终端尺寸变化
                terminal.handle(Terminal.Signal.WINCH, signal -> {
                    Size newSize = terminal.getSize();
                    this.width = newSize.getColumns() > 0 ? newSize.getColumns() : this.width;
                    this.height = newSize.getRows() > 0 ? newSize.getRows() : this.height;
                    markDirty();
                });
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
            if (rootRenderable instanceof Component<?> component) {
                component.setTerminalSize(width, height);
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
            renderDirty = false;
        }

        /**
         * 写入终端（差异化更新）。
         * 使用 ANSI 擦除序列清除旧输出，然后写入新内容。
         */
        private void writeToTerminal(String output) {
            StringBuilder sb = new StringBuilder();

            // 清除之前的输出：先回到起始行，再逐行擦除
            if (lastLineCount > 0) {
                // 向上移动到输出起始行
                if (lastLineCount > 1) {
                    sb.append("\u001B[").append(lastLineCount - 1).append("A");
                }
                // 回到行首
                sb.append("\r");
                // 从起始行向下逐行擦除
                for (int i = 0; i < lastLineCount; i++) {
                    sb.append("\u001B[2K"); // 清除整行
                    if (i < lastLineCount - 1) {
                        sb.append("\u001B[1B"); // 下移一行
                    }
                }
                // 回到起始行
                if (lastLineCount > 1) {
                    sb.append("\u001B[").append(lastLineCount - 1).append("A");
                }
                sb.append("\r");
            }

            // 写入新输出（将 \n 替换为 \r\n 以确保 raw mode 下换行正确）
            String[] lines = output.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) sb.append("\r\n");
                sb.append(lines[i]);
            }

            termWriter.print(sb);
            termWriter.flush();

            // 记录行数
            lastLineCount = lines.length;
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
         * 读取 ESC 转义序列
         */
        private KeyParser.ParseResult readEscapeSequence() throws IOException {
            // 等待短暂时间看是否有后续字符
            int next = reader.read(50);
            if (next < 0) {
                // 单独的 ESC 键
                return KeyParser.parseControlChar(0x1b);
            }

            StringBuilder seq = new StringBuilder();
            seq.append((char) next);

            // 继续读取直到序列完成
            while (!KeyParser.isCompleteSequence(seq.toString())) {
                int more = reader.read(10);
                if (more < 0) break;
                seq.append((char) more);
                if (seq.length() > 10) break; // 防止无限读取
            }

            return KeyParser.parseEscapeSequence(seq.toString());
        }

        /**
         * 分发输入事件到组件
         */
        private void dispatchInput(KeyParser.ParseResult result) {
            Key key = result.toKey();
            String input = result.inputText();

            // 内置处理：Ctrl+C 退出
            if ("c".equals(result.name()) && result.ctrl()) {
                exit();
                return;
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
            if (rootRenderable instanceof Component<?> component) {
                component.onInput(input, key);
            }
        }

        /**
         * 退出应用
         */
        public void exit() {
            running = false;
            if (rootRenderable instanceof Component<?> component) {
                component.onUnmount();
            }
        }

        /**
         * 清理终端资源
         */
        private void cleanup() {
            if (terminal != null) {
                try {
                    // 显示光标
                    termWriter.print("\u001B[?25h");
                    // 输出换行，避免下一个 shell 提示覆盖输出
                    termWriter.println();
                    termWriter.flush();

                    // 恢复终端属性
                    if (savedAttributes != null) {
                        terminal.setAttributes(savedAttributes);
                    }

                    terminal.close();
                } catch (IOException e) {
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
    }
}
