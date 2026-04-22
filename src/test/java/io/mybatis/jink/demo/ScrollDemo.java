package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Component;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Scroll;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.input.Key;
import io.mybatis.jink.style.BorderStyle;
import io.mybatis.jink.style.Color;
import io.mybatis.jink.style.FlexDirection;

import java.util.ArrayList;
import java.util.List;

/**
 * 滚动功能演示。
 * 展示 10 行内容装进 5 行高度的 Box 里滚动浏览的效果。
 * <p>
 * 使用方式:
 * <pre>
 * mvn compile exec:java -Dexec.mainClass="io.mybatis.jink.demo.ScrollDemo"
 * </pre>
 * <p>
 * 操作说明:
 * - 按 ↑↓ 方向键滚动内容
 * - 按 PageUp/PageDown 快速滚动
 * - 按 Home/End 跳转到顶部/底部
 * - 按 q 退出滚动模式（本演示中始终处于滚动模式）
 * - 按 Ctrl+C 退出程序
 */
public class ScrollDemo extends Component<ScrollDemo.State> {

    public static final class State {
        public final List<String> items;
        public final boolean scrollMode;
        public final int scrollOffset;

        public State(List<String> items, boolean scrollMode, int scrollOffset) {
            this.items = items;
            this.scrollMode = scrollMode;
            this.scrollOffset = scrollOffset;
        }
    }

    private final Scroll scrollComponent;

    public ScrollDemo() {
        super(new State(generateItems(), true, 0));

        List<Renderable> contentItems = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            contentItems.add(Text.of("Item " + i + ": This is line " + i + " of the scrollable content.")
                    .color(i % 2 == 0 ? Color.Basic.CYAN : Color.Basic.WHITE));
        }

        Box contentBox = Box.of(contentItems.toArray(new Renderable[0]))
                .flexDirection(FlexDirection.COLUMN)
                .padding(1);

        this.scrollComponent = contentBox.scroll(5);
        this.scrollComponent.scrollMode(true);
    }

    private static List<String> generateItems() {
        List<String> items = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            items.add("Item " + i + ": This is line " + i + " of the scrollable content.");
        }
        return items;
    }

    @Override
    public Renderable render() {
        State s = getState();

        Box header = Box.of(
                Text.of("Scroll Demo").color(Color.Basic.MAGENTA).bold(),
                Text.of("10 items in 5-line viewport").dimmed()
        ).flexDirection(FlexDirection.COLUMN)
                .borderStyle(BorderStyle.ROUND)
                .borderColor(Color.Basic.MAGENTA)
                .paddingX(1);

        Box scrollContainer = Box.of(scrollComponent)
                .flexDirection(FlexDirection.COLUMN)
                .borderStyle(BorderStyle.SINGLE)
                .borderColor(Color.Basic.BLUE)
                .width(60);

        Box helpText = Box.of(
                Text.of(Text.of(" ↑↓ ").bold().inverse(), Text.of(" Scroll").dimmed()),
                Text.of("  "),
                Text.of(Text.of(" PgUp/PgDn ").bold().inverse(), Text.of(" Fast scroll").dimmed()),
                Text.of("  "),
                Text.of(Text.of(" Home/End ").bold().inverse(), Text.of(" Jump").dimmed()),
                Text.of("  "),
                Text.of(Text.of(" Ctrl+C ").bold().inverse(), Text.of(" Exit").dimmed())
        ).paddingX(1);

        Box statusBar = Box.of(
                Text.of("Scroll offset: " + scrollComponent.getScrollOffset()).dimmed(),
                Text.of("  "),
                Text.of(scrollComponent.canScrollUp() ? "▲ Can scroll up" : "  At top").dimmed(),
                Text.of("  "),
                Text.of(scrollComponent.canScrollDown() ? "▼ Can scroll down" : "  At bottom").dimmed()
        ).paddingX(1);

        return Box.of(
                header,
                scrollContainer,
                Text.of(rep("─", 60)),
                helpText,
                Text.of(rep("─", 60)),
                statusBar
        ).flexDirection(FlexDirection.COLUMN)
                .width(60);
    }

    @Override
    public void onInput(String input, Key key) {
        scrollComponent.onInput(input, key);
    }

    public static void main(String[] args) {
        Ink.Instance app = Ink.render(new ScrollDemo());
        app.waitUntilExit();
    }

    private static String rep(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(s);
        }
        return sb.toString();
    }
}
