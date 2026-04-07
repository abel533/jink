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
 * ink 官方示例 router 的 jink 等效实现。
 *
 * <p>使用状态机模拟路由：Home 和 About 两个页面，按 Enter 在页面间跳转，按 q 退出。
 * 对应 ink 原版的 react-router MemoryRouter + Routes。
 *
 * <p>运行:
 * <pre>
 * mvn exec:java -Dexec.mainClass=io.mybatis.jink.demo.RouterDemo -Dexec.classpathScope=test
 * </pre>
 */
public class RouterDemo extends Component<RouterDemo.State> {

    enum Page { HOME, ABOUT }

    static final class State {
        private final Page page;
        State(Page page) { this.page = page; }
        Page page() { return page; }
    }

    public RouterDemo() {
        super(new State(Page.HOME));
    }

    @Override
    public Renderable render() {
        return getState().page() == Page.HOME ? renderHome() : renderAbout();
    }

    private Renderable renderHome() {
        return Box.of(
                Text.of("Home").bold().color(Color.GREEN),
                Text.of("Press Enter to go to About, or \"q\" to quit.")
        ).flexDirection(FlexDirection.COLUMN);
    }

    private Renderable renderAbout() {
        return Box.of(
                Text.of("About").bold().color(Color.BLUE),
                Text.of("Press Enter to go back Home, or \"q\" to quit.")
        ).flexDirection(FlexDirection.COLUMN);
    }

    @Override
    public void onInput(String input, Key key) {
        if ("q".equals(input)) {
            System.exit(0);
        }
        if (key.return_()) {
            Page next = getState().page() == Page.HOME ? Page.ABOUT : Page.HOME;
            setState(new State(next));
        }
    }

    public static void main(String[] args) {
        Ink.render(new RouterDemo()).waitUntilExit();
    }
}
