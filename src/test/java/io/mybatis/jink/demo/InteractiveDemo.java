package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Component;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.input.Key;
import io.mybatis.jink.style.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 交互式 Demo：模拟 Copilot CLI 界面。
 * 使用 JLine 3 raw mode，支持键盘输入。
 * <p>
 * 运行方式: mvn compile exec:java -Dexec.mainClass="io.mybatis.jink.demo.InteractiveDemo"
 * 或: java -cp target/test-classes:target/classes:... io.mybatis.jink.demo.InteractiveDemo
 */
public class InteractiveDemo extends Component<InteractiveDemo.State> {

    static final class State {
        private final String inputText;
        private final int cursorPos;
        private final List<String> messages;
        private final int selectedIndex;
        State(String inputText, int cursorPos, List<String> messages, int selectedIndex) {
            this.inputText = inputText;
            this.cursorPos = cursorPos;
            this.messages = messages;
            this.selectedIndex = selectedIndex;
        }
        String inputText() { return inputText; }
        int cursorPos() { return cursorPos; }
        List<String> messages() { return messages; }
        int selectedIndex() { return selectedIndex; }
    }

    public InteractiveDemo() {
        super(new State("", 0, Arrays.asList(
                "● Welcome to Jink Interactive Demo!",
                "● Type something and press Enter to add a message",
                "● Use ↑↓ to select messages, Backspace to delete",
                "● Press Ctrl+C to exit"
        ), -1));
    }

    @Override
    public Renderable render() {
        State s = getState();
        int width = 60;

        // 构建消息列表
        List<Renderable> msgItems = new ArrayList<>();
        for (int i = 0; i < s.messages.size(); i++) {
            String msg = s.messages.get(i);
            boolean selected = i == s.selectedIndex;
            if (selected) {
                msgItems.add(Text.of("▶ " + msg).color(Color.Basic.CYAN).bold());
            } else {
                msgItems.add(Text.of("  " + msg).color(Color.Basic.WHITE));
            }
        }

        // 输入区域
        String displayText = s.inputText.isEmpty() ? "Type here..." : s.inputText;
        Text inputDisplay = s.inputText.isEmpty()
                ? Text.of("> " + displayText).dimmed()
                : Text.of("> " + displayText).color(Color.Basic.GREEN);

        return Box.of(
                // 标题
                Box.of(
                        Text.of("Jink Interactive Demo").color(Color.Basic.MAGENTA).bold(),
                        Text.of("Terminal UI powered by Java").dimmed()
                ).flexDirection(FlexDirection.COLUMN)
                        .borderStyle(BorderStyle.ROUND)
                        .borderColor(Color.Basic.MAGENTA)
                        .paddingX(1)
                        .width(width - 2),
                // 消息列表
                Box.of(msgItems.toArray(new Renderable[0]))
                        .flexDirection(FlexDirection.COLUMN)
                        .paddingX(1)
                        .paddingY(1),
                // 分隔线
                Text.of(rep("─", width)),
                // 输入区域
                Box.of(inputDisplay).paddingX(1),
                // 分隔线
                Text.of(rep("─", width)),
                // 快捷键
                Box.of(
                        Text.of(Text.of(" Enter ").bold().inverse(), Text.of(" Send").dimmed()),
                        Text.of("  "),
                        Text.of(Text.of(" ↑↓ ").bold().inverse(), Text.of(" Select").dimmed()),
                        Text.of("  "),
                        Text.of(Text.of(" Bksp ").bold().inverse(), Text.of(" Delete").dimmed()),
                        Text.of("  "),
                        Text.of(Text.of(" Ctrl+C ").bold().inverse(), Text.of(" Exit").dimmed())
                ).paddingX(1)
        ).flexDirection(FlexDirection.COLUMN).width(width);
    }

    @Override
    public void onInput(String input, Key key) {
        State s = getState();

        if (key.return_()) {
            // Enter: 添加消息
            if (!s.inputText.isEmpty()) {
                List<String> newMessages = new ArrayList<>(s.messages);
                newMessages.add("You: " + s.inputText);
                setState(new State("", 0, newMessages, -1));
            }
        } else if (key.backspace()) {
            if (s.selectedIndex >= 0) {
                // 删除选中消息
                List<String> newMessages = new ArrayList<>(s.messages);
                newMessages.remove(s.selectedIndex);
                int newIdx = Math.min(s.selectedIndex, newMessages.size() - 1);
                setState(new State(s.inputText, s.cursorPos, newMessages, newIdx));
            } else if (!s.inputText.isEmpty()) {
                // 删除输入文字
                String newText = s.inputText.substring(0, s.inputText.length() - 1);
                setState(new State(newText, Math.max(0, s.cursorPos - 1), s.messages, s.selectedIndex));
            }
        } else if (key.upArrow()) {
            int newIdx = s.selectedIndex <= 0 ? s.messages.size() - 1 : s.selectedIndex - 1;
            setState(new State(s.inputText, s.cursorPos, s.messages, newIdx));
        } else if (key.downArrow()) {
            int newIdx = s.selectedIndex >= s.messages.size() - 1 ? -1 : s.selectedIndex + 1;
            setState(new State(s.inputText, s.cursorPos, s.messages, newIdx));
        } else if (key.escape()) {
            // 取消选择
            setState(new State(s.inputText, s.cursorPos, s.messages, -1));
        } else if (!input.isEmpty()) {
            // 普通文本输入
            String newText = s.inputText + input;
            setState(new State(newText, s.cursorPos + input.length(), s.messages, -1));
        }
    }

    public static void main(String[] args) {
        Ink.Instance app = Ink.render(new InteractiveDemo());
        app.waitUntilExit();
    }

    private static String rep(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }
}
