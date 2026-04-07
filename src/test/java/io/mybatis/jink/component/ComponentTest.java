package io.mybatis.jink.component;

import io.mybatis.jink.Ink;
import io.mybatis.jink.ansi.AnsiStringUtils;
import io.mybatis.jink.style.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComponentTest {

    @Test
    void testSimpleText() {
        String output = Ink.renderToString(
                Text.of("Hello World"),
                20, 1
        );
        String plain = AnsiStringUtils.stripAnsi(output);
        assertTrue(plain.contains("Hello World"));
    }

    @Test
    void testColoredText() {
        String output = Ink.renderToString(
                Text.of("Green").color(Color.Basic.GREEN),
                20, 1
        );
        assertTrue(output.contains("\u001b["));
        String plain = AnsiStringUtils.stripAnsi(output);
        assertTrue(plain.contains("Green"));
    }

    @Test
    void testBoldText() {
        String output = Ink.renderToString(
                Text.of("Bold").bold(),
                20, 1
        );
        assertTrue(output.contains("\u001b[1m"));
        String plain = AnsiStringUtils.stripAnsi(output);
        assertTrue(plain.contains("Bold"));
    }

    @Test
    void testBoxWithText() {
        String output = Ink.renderToString(
                Box.of(
                        Text.of("Inside Box")
                ).width(20).height(1),
                20, 1
        );
        String plain = AnsiStringUtils.stripAnsi(output);
        assertTrue(plain.contains("Inside Box"));
    }

    @Test
    void testBorderedBox() {
        String output = Ink.renderToString(
                Box.of(
                        Text.of("Content")
                ).borderStyle(BorderStyle.SINGLE).width(15).height(3),
                20, 5
        );
        String plain = AnsiStringUtils.stripAnsi(output);
        assertTrue(plain.contains("┌"), "应包含左上角");
        assertTrue(plain.contains("┐"), "应包含右上角");
        assertTrue(plain.contains("└"), "应包含左下角");
        assertTrue(plain.contains("┘"), "应包含右下角");
        assertTrue(plain.contains("Content"), "应包含文本内容");
    }

    @Test
    void testColumnLayout() {
        String output = Ink.renderToString(
                Box.of(
                        Text.of("Line 1"),
                        Text.of("Line 2")
                ).flexDirection(FlexDirection.COLUMN).width(20).height(2),
                20, 2
        );
        String plain = AnsiStringUtils.stripAnsi(output);
        assertTrue(plain.contains("Line 1"), "应包含 Line 1");
        assertTrue(plain.contains("Line 2"), "应包含 Line 2");
    }

    @Test
    void testRowLayout() {
        String output = Ink.renderToString(
                Box.of(
                        Text.of("A"),
                        Text.of("B")
                ).width(10).height(1),
                10, 1
        );
        String plain = AnsiStringUtils.stripAnsi(output);
        assertTrue(plain.contains("A"), "应包含 A");
        assertTrue(plain.contains("B"), "应包含 B");
    }

    @Test
    void testNestedBoxes() {
        String output = Ink.renderToString(
                Box.of(
                        Box.of(
                                Text.of("Inner")
                        ).borderStyle(BorderStyle.SINGLE).width(10).height(3)
                ).width(20).height(5),
                20, 5
        );
        String plain = AnsiStringUtils.stripAnsi(output);
        assertTrue(plain.contains("Inner"), "应包含内部文本");
        assertTrue(plain.contains("┌"), "应包含边框");
    }

    @Test
    void testPaddedBox() {
        String output = Ink.renderToString(
                Box.of(
                        Text.of("Padded")
                ).padding(1).width(12).height(3),
                20, 5
        );
        String plain = AnsiStringUtils.stripAnsi(output);
        assertTrue(plain.contains("Padded"), "应包含填充文本");
    }

    @Test
    void testStatefulComponent() {
        Component<Integer> counter = new Component<Integer>(0) {
            @Override
            public Renderable render() {
                return Box.of(
                        Text.of("Count: " + getState())
                ).width(20).height(1);
            }
        };

        // 首次渲染
        String output1 = Ink.renderToString(counter, 20, 1);
        assertTrue(AnsiStringUtils.stripAnsi(output1).contains("Count: 0"));

        // 更新状态并重新渲染
        counter.setState(42);
        String output2 = Ink.renderToString(counter, 20, 1);
        assertTrue(AnsiStringUtils.stripAnsi(output2).contains("Count: 42"));
    }

    @Test
    void testNestedText() {
        String output = Ink.renderToString(
                Text.of("Hello ", Text.of("World").bold()),
                20, 1
        );
        String plain = AnsiStringUtils.stripAnsi(output);
        assertTrue(plain.contains("Hello World"), "嵌套文本应正确组合");
    }

    @Test
    void testFlexGrow() {
        String output = Ink.renderToString(
                Box.of(
                        Box.of(Text.of("A")).flexGrow(1),
                        Box.of(Text.of("B")).flexGrow(1)
                ).width(20).height(1),
                20, 1
        );
        String plain = AnsiStringUtils.stripAnsi(output);
        assertTrue(plain.contains("A"), "应包含 A");
        assertTrue(plain.contains("B"), "应包含 B");
    }

    @Test
    void testBorderWithColor() {
        String output = Ink.renderToString(
                Box.of(
                        Text.of("Colored Border")
                ).borderStyle(BorderStyle.SINGLE)
                        .borderColor(Color.Basic.RED)
                        .width(20).height(3),
                25, 5
        );
        assertTrue(output.contains("\u001b["), "应包含 ANSI 颜色代码");
        String plain = AnsiStringUtils.stripAnsi(output);
        assertTrue(plain.contains("Colored Border"), "应包含文本");
    }

    @Test
    void testComponentLifecycle() {
        boolean[] mounted = new boolean[]{false};
        boolean[] unmounted = new boolean[]{false};

        Component<String> component = new Component<String>("test") {
            @Override
            public Renderable render() {
                return Text.of(getState());
            }

            @Override
            public void onMount() {
                mounted[0] = true;
            }

            @Override
            public void onUnmount() {
                unmounted[0] = true;
            }
        };

        Ink.Instance instance = Ink.render(component, 20, 1);
        assertTrue(mounted[0], "组件应已挂载");
        assertFalse(unmounted[0], "组件不应卸载");

        instance.exit();
        assertTrue(unmounted[0], "组件应已卸载");
    }

    @Test
    void testComponentCanRequestFrameworkExit() {
        class ExitComponent extends Component<String> {
            private boolean unmounted;

            ExitComponent() {
                super("test");
            }

            @Override
            public Renderable render() {
                return Text.of(getState());
            }

            @Override
            public void onUnmount() {
                unmounted = true;
            }

            void triggerExit() {
                exit();
            }
        }

        ExitComponent component = new ExitComponent();
        Ink.Instance instance = Ink.render(component, 20, 1);

        component.triggerExit();

        assertFalse(instance.isRunning(), "组件触发退出后实例应停止运行");
        assertTrue(component.unmounted, "组件触发退出后应执行卸载回调");
    }

    @Test
    void testStateChangeTriggersRerender() {
        int[] renderCount = new int[]{0};

        Component<Integer> component = new Component<Integer>(0) {
            @Override
            public Renderable render() {
                renderCount[0]++;
                return Text.of("Value: " + getState());
            }
        };

        // Ink.render 会调用一次 rerender
        Ink.Instance instance = Ink.render(component, 20, 1);
        int initialRenders = renderCount[0];
        assertTrue(initialRenders >= 1, "应至少渲染一次");

        // setState 触发重渲染
        component.setState(99);
        assertTrue(renderCount[0] > initialRenders, "setState 应触发重渲染");
        assertTrue(instance.getLastOutput().contains("99"), "输出应反映新状态");
    }

    @Test
    void testBackgroundColor() {
        String output = Ink.renderToString(
                Box.of(
                        Text.of("BG")
                ).backgroundColor(Color.Basic.BLUE).width(10).height(1),
                10, 1
        );
        assertTrue(output.contains("\u001b["), "应包含 ANSI 背景色");
    }

    @Test
    void testRoundBorder() {
        String output = Ink.renderToString(
                Box.of(
                        Text.of("Round")
                ).borderStyle(BorderStyle.ROUND).width(12).height(3),
                15, 5
        );
        String plain = AnsiStringUtils.stripAnsi(output);
        assertTrue(plain.contains("╭"), "圆角边框应使用 ╭");
        assertTrue(plain.contains("╮"), "圆角边框应使用 ╮");
    }
}
