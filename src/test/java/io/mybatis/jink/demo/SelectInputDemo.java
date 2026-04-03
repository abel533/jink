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
 * ink 官方示例 select-input 的 jink 等效实现。
 *
 * <p>用上下方向键在颜色列表中导航，Enter 确认选择，q 退出。
 * 对应 ink 原版的 useInput + useState 组合（ARIA 相关功能不支持，已略去）。
 *
 * <p>注意：ink 原版使用 {@code useIsScreenReaderEnabled()} 提供 ARIA 数字选择，
 * jink 暂不支持辅助功能，仅实现核心方向键导航逻辑。
 *
 * <p>运行:
 * <pre>
 * mvn exec:java -Dexec.mainClass=io.mybatis.jink.demo.SelectInputDemo -Dexec.classpathScope=test
 * </pre>
 */
public class SelectInputDemo extends Component<SelectInputDemo.State> {

    private static final String[] ITEMS = {"Red", "Green", "Blue", "Yellow", "Magenta", "Cyan"};

    record State(int selectedIndex, String confirmed) {}

    public SelectInputDemo() {
        super(new State(0, null));
    }

    @Override
    public Renderable render() {
        State s = getState();
        Box list = Box.of(Text.of("Select a color:")).flexDirection(FlexDirection.COLUMN);
        for (int i = 0; i < ITEMS.length; i++) {
            boolean selected = i == s.selectedIndex();
            list.add(Text.of((selected ? "> " : "  ") + ITEMS[i])
                    .color(selected ? Color.BLUE : Color.DEFAULT));
        }
        if (s.confirmed() != null) {
            list.add(Box.of(Text.of("Selected: " + s.confirmed()).color(Color.GREEN).bold()).marginTop(1));
            list.add(Text.of("Press q to quit.").dimmed());
        }
        return list;
    }

    @Override
    public void onInput(String input, Key key) {
        State s = getState();
        if ("q".equals(input)) System.exit(0);
        if (key.upArrow()) {
            int next = s.selectedIndex() == 0 ? ITEMS.length - 1 : s.selectedIndex() - 1;
            setState(new State(next, s.confirmed()));
        } else if (key.downArrow()) {
            int next = s.selectedIndex() == ITEMS.length - 1 ? 0 : s.selectedIndex() + 1;
            setState(new State(next, s.confirmed()));
        } else if (key.return_()) {
            setState(new State(s.selectedIndex(), ITEMS[s.selectedIndex()]));
        }
    }

    public static void main(String[] args) {
        Ink.render(new SelectInputDemo()).waitUntilExit();
    }
}
