package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Component;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.input.Key;
import io.mybatis.jink.style.Color;
import io.mybatis.jink.style.FlexDirection;

/**
 * ink 官方示例 use-focus 的 jink 等效实现。
 *
 * <p>展示焦点导航：↑/↓ 方向键切换焦点，Esc 重置。
 * ink 使用 useFocus() hook 配合 Tab 键；jink 框架内部使用 Tab 键管理内置焦点，
 * 因此此示例使用 ↑/↓ 方向键模拟 useFocus 行为。
 *
 * <p>注意：jink 不支持子组件独立注册焦点，此处用状态跟踪当前焦点索引。
 *
 * <p>运行:
 * <pre>
 * .\scripts\run-demo.ps1 io.mybatis.jink.demo.UseFocusDemo
 * </pre>
 */
public class UseFocusDemo extends Component<UseFocusDemo.State> {

    private static final String[] ITEMS = {"First", "Second", "Third"};

    record State(int focusIndex) {}

    public UseFocusDemo() {
        super(new State(-1));
    }

    @Override
    public Renderable render() {
        State s = getState();
        Renderable[] items = new Renderable[ITEMS.length];
        for (int i = 0; i < ITEMS.length; i++) {
            boolean focused = i == s.focusIndex();
            items[i] = focused
                    ? Text.of(ITEMS[i] + " ", Text.of("(focused)").color(Color.GREEN))
                    : Text.of(ITEMS[i]);
        }
        return Box.of(
                Box.of(Text.of("Press ↑/↓ to move focus, Esc to reset, q to quit."))
                        .marginBottom(1),
                Box.of(items).flexDirection(FlexDirection.COLUMN)
        ).flexDirection(FlexDirection.COLUMN).padding(1);
    }

    @Override
    public void onInput(String input, Key key) {
        State s = getState();
        if ("q".equals(input)) {
            System.exit(0);
        } else if (key.escape()) {
            setState(new State(-1));
        } else if (key.upArrow()) {
            int next = s.focusIndex() <= 0 ? ITEMS.length - 1 : s.focusIndex() - 1;
            setState(new State(next));
        } else if (key.downArrow()) {
            int next = (s.focusIndex() + 1) % ITEMS.length;
            setState(new State(next));
        }
    }

    public static void main(String[] args) {
        Ink.render(new UseFocusDemo()).waitUntilExit();
    }
}
