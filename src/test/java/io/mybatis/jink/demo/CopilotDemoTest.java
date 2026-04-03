package io.mybatis.jink.demo;

import io.mybatis.jink.input.Key;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CopilotDemoTest {

    @Test
    void upDownArrowsBrowseInputHistory() {
        TestCopilotDemo demo = new TestCopilotDemo();

        submit(demo, "first");
        submit(demo, "second");
        demo.onInput("draft", Key.plain());

        demo.onInput("", Key.builder().upArrow().build());
        assertEquals("second", demo.state().inputText());
        assertEquals(0, demo.state().scrollOffset());

        demo.onInput("", Key.builder().upArrow().build());
        assertEquals("first", demo.state().inputText());

        demo.onInput("", Key.builder().downArrow().build());
        assertEquals("second", demo.state().inputText());

        demo.onInput("", Key.builder().downArrow().build());
        assertEquals("draft", demo.state().inputText());
    }

    @Test
    void wheelScrollMovesMessageViewport() {
        TestCopilotDemo demo = new TestCopilotDemo();

        submit(demo, "first");
        submit(demo, "second");

        demo.onInput("", Key.builder().scrollUp().build());
        assertEquals(3, demo.state().scrollOffset());

        demo.onInput("", Key.builder().scrollDown().build());
        assertEquals(0, demo.state().scrollOffset());
    }

    @Test
    void editingLeavesHistoryPreviewMode() {
        TestCopilotDemo demo = new TestCopilotDemo();

        submit(demo, "first");
        submit(demo, "second");
        demo.onInput("", Key.builder().upArrow().build());
        assertEquals("second", demo.state().inputText());

        demo.onInput("!", Key.plain());
        assertEquals("second!", demo.state().inputText());

        demo.onInput("", Key.builder().downArrow().build());
        assertEquals("second!", demo.state().inputText());
    }

    private static void submit(TestCopilotDemo demo, String text) {
        demo.onInput(text, Key.plain());
        demo.onInput("", Key.builder().return_().build());
    }

    private static final class TestCopilotDemo extends CopilotDemo {
        CopilotDemo.State state() {
            return getState();
        }
    }
}
