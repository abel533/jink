package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.*;
import io.mybatis.jink.input.Key;
import io.mybatis.jink.style.*;
import io.mybatis.jink.util.StringWidth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Copilot CLI 风格完整复刻 Demo。
 * 模拟截图中的 GitHub Copilot CLI 界面布局。
 */
public class CopilotDemo extends Component<CopilotDemo.State> {

    /** 提示符宽度（"❯ " = 2 字符），续行缩进用 */
    private static final int PROMPT_WIDTH = 2;

    static final class State {
        private final String inputText;
        private final List<String> messages;
        private final int scrollOffset;
        State(String inputText, List<String> messages, int scrollOffset) {
            this.inputText = inputText;
            this.messages = messages;
            this.scrollOffset = scrollOffset;
        }
        String inputText() { return inputText; }
        List<String> messages() { return messages; }
        int scrollOffset() { return scrollOffset; }
    }

    /** 输入历史记录 */
    private final List<String> inputHistory = new ArrayList<>();
    /** 当前浏览的历史索引，-1 表示不在历史模式 */
    private int historyIndex = -1;
    /** 进入历史浏览前保存的当前输入 */
    private String savedInput = "";

    public CopilotDemo() {
        super(new State("", Collections.emptyList(), 0));
    }

    @Override
    public Renderable render() {
        State s = getState();
        int w = getColumns();
        int h = getRows();

        // 计算输入区行数（支持多行输入）
        int inputLineCount = 1;
        String lastLine = s.inputText;
        if (!s.inputText.isEmpty()) {
            String[] inputLines = s.inputText.split("\n", -1);
            inputLineCount = inputLines.length;
            lastLine = inputLines[inputLines.length - 1];
        }

        // 底部结构：shortcutBar(1) + separator(1) + input(N) + separator(1) + statusBar(1)
        // 输入区最后一行在 h-1(shortcut) -1(sep) -1 = h-3（从底部算）
        int cursorRow = h - 3;
        int cursorCol = 1 + PROMPT_WIDTH + StringWidth.width(lastLine);
        setCursorPosition(cursorRow, cursorCol);

        int headerHeight = 7;
        int bottomHeight = 4 + inputLineCount; // statusBar(1) + sep(1) + input(N) + sep(1) + shortcut(1)
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
                statusBar(w, h),
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
                "Environment loaded: Java 21, Maven, 146 tests passing."));

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
    private Renderable statusBar(int w, int h) {
        String left = System.getProperty("user.dir", ".") + " (" + w + "x" + h + ")";
        String right = "Jink 0.1.0 (Java 21)";
        int pad = w - left.length() - right.length() - 2;
        StringBuilder middleSb = new StringBuilder();
        for (int i = 0; i < (pad > 0 ? pad : 1); i++) middleSb.append(" ");
        String middle = middleSb.toString();

        return Box.of(
                Text.of(left).dimmed(),
                Spacer.create(),
                Text.of(right).dimmed()
        ).paddingX(1);
    }

    /**
     * 输入区（提示符 + 输入文本/placeholder，多行时续行缩进对齐）
     */
    private Renderable inputArea(State s, int w) {
        Text prompt = Text.of("\u276F ").color(Color.BRIGHT_GREEN).bold();
        Text content;
        if (s.inputText.isEmpty()) {
            content = Text.of("Type @ to mention files, / for commands, or ? for help").dimmed();
        } else {
            // 多行输入：续行加缩进，使文字和第一行对齐
            String indent = rep(" ", PROMPT_WIDTH);
            String displayText = s.inputText.replace("\n", "\n" + indent);
            content = Text.of(displayText).color(Color.WHITE);
        }

        return Box.of(
                Text.of(prompt, content)
        ).paddingX(1);
    }

    /**
     * 水平分隔线（浅灰色 ─ 线）
     */
    private Renderable separator(int w) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.max(0, w - 2); i++) sb.append("─");
        return Box.of(
                Text.of(sb.toString()).color(Color.BRIGHT_BLACK)
        ).paddingX(1);
    }

    /**
     * 底部快捷键栏
     */
    private Renderable shortcutBar(int w) {
        return Box.of(
                Text.of(
                        Text.of("↑↓").dimmed(),
                        Text.of(" input history").dimmed(),
                        Text.of("  "),
                        Text.of("wheel").dimmed(),
                        Text.of(" messages").dimmed()
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
        int totalMessages = 3 + s.messages.size();

        if (key.return_() && key.meta()) {
            // Shift+Enter: 多行输入换行
            setState(new State(s.inputText + "\n", s.messages, 0));
        } else if (key.return_()) {
            // Enter: 发送消息
            if (!s.inputText.isEmpty()) {
                List<String> newMessages = new ArrayList<>(s.messages);
                String[] lines = s.inputText.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i].trim();
                    if (!line.isEmpty()) {
                        newMessages.add(i == 0 ? "You: " + line : "     " + line);
                    }
                }
                // 记录输入历史
                inputHistory.add(s.inputText);
                historyIndex = -1;
                savedInput = "";
                setState(new State("", newMessages, 0));
            }
        } else if (key.backspace()) {
            if (!s.inputText.isEmpty()) {
                abandonHistoryPreview();
                String newText = s.inputText.substring(0, s.inputText.length() - 1);
                setState(new State(newText, s.messages, s.scrollOffset));
            }
        } else if (key.upArrow()) {
            browseHistoryUp(s);
        } else if (key.downArrow()) {
            browseHistoryDown(s);
        } else if (key.scrollUp()) {
            setState(new State(s.inputText, s.messages, scrollMessages(totalMessages, s.scrollOffset, 3)));
        } else if (key.scrollDown()) {
            setState(new State(s.inputText, s.messages, scrollMessages(totalMessages, s.scrollOffset, -3)));
        } else if (key.pageUp()) {
            // PageUp: 向上滚动查看消息历史（大步）
            setState(new State(s.inputText, s.messages, scrollMessages(totalMessages, s.scrollOffset, 10)));
        } else if (key.pageDown()) {
            // PageDown: 向下滚动消息（大步）
            setState(new State(s.inputText, s.messages, scrollMessages(totalMessages, s.scrollOffset, -10)));
        } else if (!input.isEmpty() && isPrintableInput(input, key)) {
            // 普通文本输入（过滤导航键和控制组合键）
            abandonHistoryPreview();
            setState(new State(s.inputText + input, s.messages, s.scrollOffset));
        }
    }

    private void browseHistoryUp(State s) {
        if (inputHistory.isEmpty()) {
            return;
        }
        if (historyIndex == -1) {
            savedInput = s.inputText;
            historyIndex = inputHistory.size() - 1;
        } else if (historyIndex > 0) {
            historyIndex--;
        }
        setState(new State(inputHistory.get(historyIndex), s.messages, s.scrollOffset));
    }

    private void browseHistoryDown(State s) {
        if (historyIndex < 0) {
            return;
        }
        historyIndex++;
        if (historyIndex >= inputHistory.size()) {
            historyIndex = -1;
            setState(new State(savedInput, s.messages, s.scrollOffset));
            savedInput = "";
            return;
        }
        setState(new State(inputHistory.get(historyIndex), s.messages, s.scrollOffset));
    }

    private int scrollMessages(int totalMessages, int currentOffset, int delta) {
        int maxOffset = Math.max(0, totalMessages - 1);
        return Math.max(0, Math.min(currentOffset + delta, maxOffset));
    }

    private void abandonHistoryPreview() {
        if (historyIndex >= 0) {
            historyIndex = -1;
            savedInput = "";
        }
    }

    /**
     * 判断输入是否为可打印文本（排除导航键、控制组合键）
     */
    private boolean isPrintableInput(String input, Key key) {
        if (key.ctrl() || key.meta()) return false;
        if (key.upArrow() || key.downArrow() || key.leftArrow() || key.rightArrow()) return false;
        if (key.pageUp() || key.pageDown() || key.home() || key.end()) return false;
        if (key.escape() || key.tab() || key.delete()) return false;
        if (key.scrollUp() || key.scrollDown()) return false;
        // 排除单个控制字符
        if (input.length() == 1 && input.charAt(0) < 0x20) return false;
        return true;
    }

    public static void main(String[] args) {
        Ink.Instance app = Ink.render(new CopilotDemo());
        app.waitUntilExit();
    }

    private static String rep(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }
}
