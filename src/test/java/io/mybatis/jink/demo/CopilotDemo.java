package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.*;
import io.mybatis.jink.input.Key;
import io.mybatis.jink.style.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Copilot CLI 风格完整复刻 Demo。
 * 模拟截图中的 GitHub Copilot CLI 界面布局。
 */
public class CopilotDemo extends Component<CopilotDemo.State> {

    record State(
            String inputText,
            List<String> messages
    ) {}

    public CopilotDemo() {
        super(new State("", List.of()));
    }

    @Override
    public Renderable render() {
        State s = getState();
        int w = getColumns();

        return Box.of(
                // === 顶部标题框（圆角洋红色边框）===
                headerBox(w),
                // === 状态消息列表 ===
                statusMessages(s),
                // === 弹性空白区 ===
                Spacer.create(),
                // === 状态栏（路径 + 模型信息）===
                statusBar(w),
                // === 输入区上方分隔线 ===
                separator(w),
                // === 输入区 ===
                inputArea(s, w),
                // === 输入区下方分隔线 ===
                separator(w),
                // === 底部快捷键栏 ===
                shortcutBar(w)
        ).flexDirection(FlexDirection.COLUMN).width(w).height(getRows());
    }

    /**
     * 顶部标题框
     */
    private Renderable headerBox(int w) {
        return Box.of(
                // 第一行：彩色方块 + 版本号
                Text.of(
                        Text.of("\u25CB\u25CB").color(Color.BRIGHT_GREEN),
                        Text.of("  "),
                        Text.of("Jink").color(Color.BRIGHT_MAGENTA).bold(),
                        Text.of(" v0.1.0").color(Color.WHITE)
                ),
                // 第二行：彩色方块 + 描述
                Text.of(
                        Text.of("\u25A0\u25A0\u25A0").color(Color.BRIGHT_RED),
                        Text.of("  "),
                        Text.of("Describe a task to get started.").dimmed()
                ),
                // 空行（使用空格确保高度为1）
                Text.of(" "),
                // Tip 行
                Text.of(
                        Text.of("Tip: ").dimmed(),
                        Text.of("/help").color(Color.BRIGHT_CYAN).bold(),
                        Text.of(" Show available commands.").dimmed()
                ),
                Text.of("Jink uses AI, so always check for mistakes.").dimmed()
        ).flexDirection(FlexDirection.COLUMN)
                .borderStyle(BorderStyle.ROUND)
                .borderColor(Color.BRIGHT_MAGENTA)
                .paddingX(1);
    }

    /**
     * 状态消息列表
     */
    private Renderable statusMessages(State s) {
        List<Renderable> items = new ArrayList<>();

        // 固定的启动消息
        items.add(statusLine(Color.BRIGHT_YELLOW,
                "No configuration found. Run /init to generate a jink-config.md file."));
        items.add(statusLine(Color.BRIGHT_BLUE,
                "Project loaded from current directory."));
        items.add(statusLine(Color.BRIGHT_BLUE,
                "Environment loaded: Java 21, Maven, 102 tests passing."));

        // 用户发送的消息
        for (String msg : s.messages) {
            items.add(statusLine(Color.BRIGHT_GREEN, msg));
        }

        return Box.of(items.toArray(new Renderable[0]))
                .flexDirection(FlexDirection.COLUMN)
                .paddingTop(1)
                .paddingX(1);
    }

    /**
     * 单条状态消息（彩色圆点 + 文本）
     */
    private Renderable statusLine(Color dotColor, String text) {
        return Text.of(
                Text.of("\u25CF ").color(dotColor),
                Text.of(text).color(Color.WHITE)
        );
    }

    /**
     * 状态栏（左: 路径, 右: 模型信息）
     */
    private Renderable statusBar(int w) {
        String left = "D:\\Learn\\ink4j";
        String right = "Jink 0.1.0 (Java 21)";
        int pad = w - left.length() - right.length() - 2;
        String middle = pad > 0 ? " ".repeat(pad) : " ";

        return Box.of(
                Text.of(left).dimmed(),
                Spacer.create(),
                Text.of(right).dimmed()
        ).paddingX(1);
    }

    /**
     * 输入区（提示符 + 输入文本/placeholder）
     */
    private Renderable inputArea(State s, int w) {
        Text prompt = Text.of("\u276F ").color(Color.BRIGHT_GREEN).bold();
        Text content;
        if (s.inputText.isEmpty()) {
            content = Text.of("Type @ to mention files, / for commands, or ? for help").dimmed();
        } else {
            content = Text.of(s.inputText).color(Color.WHITE);
        }

        return Box.of(
                Text.of(prompt, content)
        ).paddingX(1);
    }

    /**
     * 水平分隔线（浅灰色 ─ 线）
     */
    private Renderable separator(int w) {
        return Box.of(
                Text.of("─".repeat(Math.max(0, w - 2))).color(Color.BRIGHT_BLACK)
        ).paddingX(1);
    }

    /**
     * 底部快捷键栏
     */
    private Renderable shortcutBar(int w) {
        return Box.of(
                Text.of(
                        Text.of("shift+tab").dimmed(),
                        Text.of(" switch mode").dimmed()
                ),
                Spacer.create(),
                Text.of(
                        Text.of("Remaining reqs.: ").dimmed(),
                        Text.of("88.6%").color(Color.BRIGHT_GREEN)
                )
        ).paddingX(1);
    }

    @Override
    public void onInput(String input, Key key) {
        State s = getState();

        if (key.return_()) {
            // Enter: 发送消息
            if (!s.inputText.isEmpty()) {
                List<String> newMessages = new ArrayList<>(s.messages);
                newMessages.add("You: " + s.inputText);
                setState(new State("", newMessages));
            }
        } else if (key.backspace()) {
            // Backspace: 删除输入
            if (!s.inputText.isEmpty()) {
                String newText = s.inputText.substring(0, s.inputText.length() - 1);
                setState(new State(newText, s.messages));
            }
        } else if (!input.isEmpty()) {
            // 普通文本输入
            setState(new State(s.inputText + input, s.messages));
        }
    }

    public static void main(String[] args) {
        Ink.Instance app = Ink.render(new CopilotDemo());
        app.waitUntilExit();
    }
}
