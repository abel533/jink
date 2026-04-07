package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Component;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.input.Key;
import io.mybatis.jink.style.FlexDirection;

/**
 * ink 官方示例 use-input 的 jink 等效实现。
 *
 * <p>用方向键在 20×10 区域内移动一个小脸 "^_^"，按 q 退出。
 * 对应 ink 的 useInput + useState 组合。
 *
 * <p>运行:
 * <pre>
 * mvn exec:java -Dexec.mainClass=io.mybatis.jink.demo.UseInputDemo -Dexec.classpathScope=test
 * </pre>
 */
public class UseInputDemo extends Component<UseInputDemo.State> {

    static final class State {
        private final int x;
        private final int y;
        State(int x, int y) { this.x = x; this.y = y; }
        int x() { return x; }
        int y() { return y; }
    }

    public UseInputDemo() {
        super(new State(1, 1));
    }

    @Override
    public Renderable render() {
        State s = getState();
        return Box.of(
                Text.of("Use arrow keys to move the face. Press \"q\" to exit."),
                Box.of(Text.of("^_^")).paddingLeft(s.x()).paddingTop(s.y()).height(12)
        ).flexDirection(FlexDirection.COLUMN);
    }

    @Override
    public void onInput(String input, Key key) {
        State s = getState();
        if ("q".equals(input)) {
            System.exit(0);
        }
        int x = s.x(), y = s.y();
        if (key.leftArrow())  x = Math.max(1, x - 1);
        if (key.rightArrow()) x = Math.min(20, x + 1);
        if (key.upArrow())    y = Math.max(1, y - 1);
        if (key.downArrow())  y = Math.min(10, y + 1);
        setState(new State(x, y));
    }

    public static void main(String[] args) {
        Ink.render(new UseInputDemo()).waitUntilExit();
    }
}
