package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.style.*;

/**
 * 简单 Demo：验证前 4 阶段的组件/布局/渲染效果。
 * 模拟 GitHub Copilot CLI 的终端 UI 界面。
 */
public class SimpleDemo {

    public static void main(String[] args) {
        int width = 60;

        // 构建 UI 树
        Renderable ui = Box.of(
                // 欢迎框（带圆角边框）
                welcomeBox(width - 2),
                // 消息列表
                messageList(),
                // 分隔线
                separator(width),
                // 输入提示
                inputPrompt(width),
                // 分隔线
                separator(width),
                // 底部快捷键栏
                shortcutBar()
        ).flexDirection(FlexDirection.COLUMN).width(width);

        // 渲染为字符串输出
        System.out.println("=== Jink Demo: Copilot CLI 风格 UI ===\n");
        String output = Ink.renderToString(ui, width, 20);
        System.out.println(output);

        System.out.println("\n=== 渲染完成 ===");

        // 额外验证：嵌套布局
        System.out.println("\n=== 嵌套 Flexbox 布局验证 ===\n");
        Renderable nested = Box.of(
                Box.of(
                        Text.of("左侧面板").color(Color.Basic.CYAN)
                ).flexGrow(1).borderStyle(BorderStyle.SINGLE).height(5),
                Box.of(
                        Text.of("右侧面板").color(Color.Basic.YELLOW)
                ).flexGrow(1).borderStyle(BorderStyle.SINGLE).height(5)
        ).width(40).height(5);

        System.out.println(Ink.renderToString(nested, 40, 5));

        // 颜色样式验证
        System.out.println("\n=== 颜色与样式验证 ===\n");
        Renderable colors = Box.of(
                Text.of("普通文本"),
                Text.of("粗体").bold(),
                Text.of("斜体").italic(),
                Text.of("下划线").underline(),
                Text.of("红色").color(Color.Basic.RED),
                Text.of("绿色背景").backgroundColor(Color.Basic.GREEN),
                Text.of("RGB颜色").color(new Color.Rgb(255, 165, 0))
        ).flexDirection(FlexDirection.COLUMN).width(30);

        System.out.println(Ink.renderToString(colors, 30, 7));
    }

    /**
     * 欢迎框
     */
    static Renderable welcomeBox(int width) {
        return Box.of(
                Text.of("Welcome to GitHub Copilot CLI v1.0").color(Color.Basic.MAGENTA).bold(),
                Text.of("Powered by Jink - Java ink framework").dimmed()
        ).flexDirection(FlexDirection.COLUMN)
                .borderStyle(BorderStyle.ROUND)
                .borderColor(Color.Basic.MAGENTA)
                .paddingX(1)
                .width(width);
    }

    /**
     * 消息列表
     */
    static Renderable messageList() {
        return Box.of(
                Text.of("● Ready to assist with your coding tasks").color(Color.Basic.GREEN),
                Text.of("● Type your question below").color(Color.Basic.GREEN),
                Text.of("● Use shortcuts at the bottom for quick actions").color(Color.Basic.GREEN)
        ).flexDirection(FlexDirection.COLUMN)
                .paddingX(1)
                .paddingY(1);
    }

    /**
     * 分隔线（用文本模拟）
     */
    static Renderable separator(int width) {
        return Text.of("─".repeat(width));
    }

    /**
     * 输入提示
     */
    static Renderable inputPrompt(int width) {
        return Box.of(
                Text.of("> ").color(Color.Basic.CYAN).bold(),
                Text.of("Ask a question...").dimmed()
        ).paddingX(1);
    }

    /**
     * 底部快捷键栏
     */
    static Renderable shortcutBar() {
        return Box.of(
                shortcutKey("Ctrl+C", "Exit"),
                Text.of("  "),
                shortcutKey("Ctrl+L", "Clear"),
                Text.of("  "),
                shortcutKey("Tab", "Accept"),
                Text.of("  "),
                shortcutKey("Esc", "Cancel")
        ).paddingX(1);
    }

    /**
     * 单个快捷键显示
     */
    static Renderable shortcutKey(String key, String label) {
        return Text.of(
                Text.of(key).bold().inverse(),
                Text.of(" " + label).dimmed()
        );
    }
}
