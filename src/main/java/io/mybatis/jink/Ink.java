package io.mybatis.jink;

import io.mybatis.jink.component.Component;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.dom.ElementNode;
import io.mybatis.jink.layout.FlexLayout;
import io.mybatis.jink.render.NodeRenderer;
import io.mybatis.jink.render.TerminalWriter;
import io.mybatis.jink.render.VirtualScreen;

import java.io.PrintStream;

/**
 * Jink 框架主入口。
 * 对应 ink 的 render() 函数。
 * <p>
 * 使用示例:
 * <pre>
 * // 渲染为字符串（测试用）
 * String output = Ink.renderToString(Box.of(Text.of("Hello")), 80, 24);
 *
 * // 交互式渲染到终端
 * Ink.Instance app = Ink.render(new MyComponent());
 * app.waitUntilExit();
 * </pre>
 */
public class Ink {

    /**
     * 将可渲染内容渲染为 ANSI 字符串（适合测试和无交互场景）
     */
    public static String renderToString(Renderable renderable, int width, int height) {
        ElementNode root = buildDomTree(renderable, width, height);
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

    /**
     * 交互式渲染（Phase 5 实现完整终端 I/O 版本）
     */
    public static Instance render(Renderable renderable) {
        return render(renderable, 80, 24);
    }

    /**
     * 交互式渲染（指定尺寸）
     */
    public static Instance render(Renderable renderable, int width, int height) {
        return new Instance(renderable, width, height);
    }

    /**
     * 构建 DOM 树并执行布局
     */
    static ElementNode buildDomTree(Renderable renderable, int width, int height) {
        ElementNode root = ElementNode.createRoot();
        ElementNode content = renderable.toNode();
        root.appendChild(content);
        FlexLayout.calculateLayout(root, width);
        return root;
    }

    /**
     * Ink 实例，管理渲染生命周期
     */
    public static class Instance {
        private final Renderable rootRenderable;
        private volatile boolean running = true;
        private int width;
        private int height;
        private PrintStream output = System.out;
        private TerminalWriter writer;
        private String lastOutput = "";

        Instance(Renderable renderable, int width, int height) {
            this.rootRenderable = renderable;
            this.width = width;
            this.height = height;
            this.writer = new TerminalWriter(output);

            // 如果是有状态组件，注册状态变化回调
            if (renderable instanceof Component<?> component) {
                component.setOnStateChange(this::rerender);
                component.onMount();
            }

            // 首次渲染
            rerender();
        }

        /**
         * 设置输出尺寸
         */
        public Instance size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        /**
         * 设置输出流
         */
        public Instance output(PrintStream output) {
            this.output = output;
            this.writer = new TerminalWriter(output);
            return this;
        }

        /**
         * 触发重渲染
         */
        public void rerender() {
            if (!running) return;

            ElementNode root = buildDomTree(rootRenderable, width, height);
            VirtualScreen screen = NodeRenderer.render(root);
            String newOutput = screen.render();

            if (!newOutput.equals(lastOutput)) {
                writer.render(newOutput);
                lastOutput = newOutput;
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
         * 等待退出（Phase 5 使用 JLine 3 事件循环实现完整版本）
         */
        public void waitUntilExit() {
            while (running) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        public boolean isRunning() {
            return running;
        }

        public String getLastOutput() {
            return lastOutput;
        }
    }
}
