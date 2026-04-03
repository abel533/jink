package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Component;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.input.Key;
import io.mybatis.jink.style.BorderStyle;
import io.mybatis.jink.style.Color;
import io.mybatis.jink.style.FlexDirection;

import java.util.List;

/**
 * 交互式提示组件 Demo：展示「从列表中选择一项」或「自由输入内容」。
 *
 * <p>界面特性：
 * <ul>
 *   <li>单线边框包裹整个组件</li>
 *   <li>顶部显示问题/提示文本</li>
 *   <li>带编号的选项列表，❯ 指示当前选中项（青色高亮）</li>
 *   <li>最后一项为「Other (type your answer)」，选中并回车后切换到文本输入模式</li>
 *   <li>底部提示：↑↓ to select · Enter to confirm · Esc to cancel</li>
 * </ul>
 *
 * <p>键盘操作：
 * <ul>
 *   <li>↑ / ↓ — 上下选择</li>
 *   <li>Enter — 确认选中项（若选中 Other 则进入输入模式）</li>
 *   <li>Esc — 取消/退出</li>
 *   <li>输入模式下：正常字符累积，Backspace 删除，Enter 提交，Esc 返回选择模式</li>
 * </ul>
 *
 * <p>运行:
 * <pre>
 * .\scripts\run-demo.ps1 io.mybatis.jink.demo.PromptDemo
 * </pre>
 */
public class PromptDemo extends Component<PromptDemo.State> {

    private static final String OTHER_LABEL = "Other (type your answer)";

    enum Mode { SELECT, INPUT, DONE }

    record State(int selectedIndex, Mode mode, String inputText, String result) {}

    private final String question;
    private final List<String> options;

    public PromptDemo(String question, List<String> options) {
        super(new State(0, Mode.SELECT, "", null));
        this.question = question;
        this.options = options;
    }

    @Override
    public Renderable render() {
        State s = getState();

        if (s.mode() == Mode.DONE) {
            return Box.of(
                    Text.of("✔ " + s.result()).color(Color.GREEN).bold()
            ).paddingX(1).paddingY(0);
        }

        // Build option rows
        Box optionList = Box.of().flexDirection(FlexDirection.COLUMN).marginTop(1);
        for (int i = 0; i < options.size(); i++) {
            boolean isSelected = i == s.selectedIndex();
            boolean isOther = options.get(i).equals(OTHER_LABEL);

            if (isSelected && isOther && s.mode() == Mode.INPUT) {
                // Text input mode for "Other"
                String typedText = s.inputText().isEmpty() ? "" : s.inputText();
                optionList.add(
                        Box.of(
                                Text.of("❯ " + (i + 1) + ". ").color(Color.CYAN),
                                Text.of(typedText + "█").color(Color.CYAN)
                        ).flexDirection(FlexDirection.ROW)
                );
            } else {
                String prefix = isSelected ? "❯ " : "  ";
                String label = prefix + (i + 1) + ". " + options.get(i);
                optionList.add(Text.of(label).color(isSelected ? Color.CYAN : null));
            }
        }

        String hint = s.mode() == Mode.INPUT
                ? "Type your answer · Enter to confirm · Esc to go back"
                : "↑↓ to select · Enter to confirm · Esc to cancel";

        return Box.of(
                Text.of(question),
                optionList,
                Box.of(
                        Text.of(hint).dimmed()
                ).marginTop(1)
        ).flexDirection(FlexDirection.COLUMN)
                .borderStyle(BorderStyle.SINGLE)
                .paddingX(1)
                .paddingY(0);
    }

    @Override
    public void onInput(String input, Key key) {
        State s = getState();

        if (s.mode() == Mode.INPUT) {
            if (key.return_()) {
                if (!s.inputText().isEmpty()) {
                    confirmAndExit(s.inputText());
                }
            } else if (key.escape()) {
                setState(new State(s.selectedIndex(), Mode.SELECT, "", null));
            } else if (key.backspace() || key.delete()) {
                if (!s.inputText().isEmpty()) {
                    setState(new State(s.selectedIndex(), Mode.INPUT,
                            s.inputText().substring(0, s.inputText().length() - 1), null));
                }
            } else if (!input.isEmpty()) {
                setState(new State(s.selectedIndex(), Mode.INPUT, s.inputText() + input, null));
            }
            return;
        }

        // SELECT mode
        if (key.upArrow()) {
            int next = s.selectedIndex() == 0 ? options.size() - 1 : s.selectedIndex() - 1;
            setState(new State(next, Mode.SELECT, "", null));
        } else if (key.downArrow()) {
            int next = (s.selectedIndex() + 1) % options.size();
            setState(new State(next, Mode.SELECT, "", null));
        } else if (key.return_()) {
            String selected = options.get(s.selectedIndex());
            if (selected.equals(OTHER_LABEL)) {
                setState(new State(s.selectedIndex(), Mode.INPUT, "", null));
            } else {
                confirmAndExit(selected);
            }
        } else if (key.escape()) {
            System.exit(0);
        }
    }

    private void confirmAndExit(String result) {
        setState(new State(getState().selectedIndex(), Mode.DONE, "", result));
        Thread t = new Thread(() -> {
            try { Thread.sleep(600); } catch (InterruptedException ignored) {}
            System.exit(0);
        });
        t.setDaemon(true);
        t.start();
    }

    public static void main(String[] args) {
        String question = "接下来要做什么？";
        List<String> options = List.of(
                "继续当前任务",
                "查看 ink 示例详解",
                "查看原有 Demo 详解",
                OTHER_LABEL
        );
        Ink.render(new PromptDemo(question, options)).waitUntilExit();
    }
}
