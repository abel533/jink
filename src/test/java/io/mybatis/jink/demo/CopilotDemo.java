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
            List<String> messages,
            int scrollOffset
    ) {}

    public CopilotDemo() {
        super(new State("", List.of(), 0));
    }

    @Override
    public Renderable render() {
        State s = getState();
        int w = getColumns();
        int h = getRows();

        // 计算光标位置：输入区在倒数第3行，列 = padding(1) + prompt("❯ "=2) + 输入文本长度
        int inputRow = h - 3; // shortcutBar(-1), separator(-2), inputArea(-3)
        int inputCol = 1 + 2 + s.inputText.length();
        setCursorPosition(inputRow, inputCol);

        // 计算消息区可用行数：总高度 - 标题框(7) - 底部栏(5: statusBar+sep+input+sep+shortcut) - paddingTop(1)
        int headerHeight = 7;
        int bottomHeight = 5;
        int messagePaddingTop = 1;
        int maxMessageLines = h - headerHeight - bottomHeight - messagePaddingTop;

        return Box.of(
                // === 顶部标题框（圆角洋红色边框）===
                headerBox(w),
                // === 状态消息列表 ===
                statusMessages(s, maxMessageLines),
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
        ).flexDirection(FlexDirection.COLUMN).width(w).height(h);
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
     * 状态消息列表（带虚拟滚动 + PageUp/PageDown 滚动支持）
     */
    private Renderable statusMessages(State s, int maxLines) {
        List<Renderable> allItems = new ArrayList<>();

        // 固定的启动消息
        allItems.add(statusLine(Color.BRIGHT_YELLOW,
                "No configuration found. Run /init to generate a jink-config.md file."));
        allItems.add(statusLine(Color.BRIGHT_BLUE,
                "Project loaded from current directory."));
        allItems.add(statusLine(Color.BRIGHT_BLUE,
                "Environment loaded: Java 21, Maven, 102 tests passing."));

        // 用户发送的消息
        for (String msg : s.messages) {
            allItems.add(statusLine(Color.BRIGHT_GREEN, msg));
        }

        // 计算可见窗口
        List<Renderable> visibleItems;
        if (maxLines > 0 && allItems.size() > maxLines) {
            // 默认显示最新的消息，scrollOffset 向上偏移
            int endIdx = allItems.size() - s.scrollOffset;
            int startIdx = Math.max(0, endIdx - maxLines);
            endIdx = Math.min(allItems.size(), startIdx + maxLines);
            visibleItems = allItems.subList(startIdx, endIdx);
        } else {
            visibleItems = allItems;
        }

        return Box.of(visibleItems.toArray(new Renderable[0]))
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
        int totalMessages = 3 + s.messages.size(); // 3 固定消息 + 用户消息

        if (key.return_() && key.meta()) {
            // Alt+Enter: 多行输入换行
            setState(new State(s.inputText + "\n", s.messages, 0));
        } else if (key.return_()) {
            // Enter: 发送消息
            if (!s.inputText.isEmpty()) {
                List<String> newMessages = new ArrayList<>(s.messages);
                newMessages.add("You: " + s.inputText.replace("\n", " "));
                setState(new State("", newMessages, 0));
            }
        } else if (key.backspace()) {
            // Backspace: 删除输入
            if (!s.inputText.isEmpty()) {
                String newText = s.inputText.substring(0, s.inputText.length() - 1);
                setState(new State(newText, s.messages, s.scrollOffset));
            }
        } else if (key.pageUp() || key.upArrow()) {
            // PageUp / ↑: 向上滚动查看历史
            int newOffset = Math.min(s.scrollOffset + (key.pageUp() ? 5 : 1),
                    Math.max(0, totalMessages - 1));
            setState(new State(s.inputText, s.messages, newOffset));
        } else if (key.pageDown() || key.downArrow()) {
            // PageDown / ↓: 向下滚动
            int newOffset = Math.max(0, s.scrollOffset - (key.pageDown() ? 5 : 1));
            setState(new State(s.inputText, s.messages, newOffset));
        } else if (!input.isEmpty() && isPrintableInput(input, key)) {
            // 普通文本输入（过滤导航键残余字符）
            setState(new State(s.inputText + input, s.messages, s.scrollOffset));
        }
    }

    /**
     * 判断输入是否为可打印文本（排除导航键和控制字符残余）
     */
    private boolean isPrintableInput(String input, Key key) {
        if (key.upArrow() || key.downArrow() || key.leftArrow() || key.rightArrow()) return false;
        if (key.pageUp() || key.pageDown() || key.home() || key.end()) return false;
        if (key.escape() || key.tab() || key.delete()) return false;
        // 排除单个控制字符
        if (input.length() == 1 && input.charAt(0) < 0x20) return false;
        return true;
    }

    public static void main(String[] args) {
        Ink.Instance app = Ink.render(new CopilotDemo());
        app.waitUntilExit();
    }
}
