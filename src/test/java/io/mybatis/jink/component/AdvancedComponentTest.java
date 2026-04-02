package io.mybatis.jink.component;

import io.mybatis.jink.Ink;
import io.mybatis.jink.style.Color;
import io.mybatis.jink.style.FlexDirection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 6 高级特性测试
 */
class AdvancedComponentTest {

    // ===== Spacer =====

    @Test
    void spacerPushesToEdges() {
        // Spacer 应把两个 Text 推到左右两端
        var ui = Box.of(
                Text.of("L"),
                Spacer.create(),
                Text.of("R")
        ).width(10);

        String output = Ink.renderToString(ui, 10, 1);
        String stripped = output.replaceAll("\u001B\\[[^m]*m", "");
        assertTrue(stripped.startsWith("L"), "应以 L 开头: " + stripped);
        assertTrue(stripped.stripTrailing().endsWith("R"), "应以 R 结尾: " + stripped);
        assertEquals(10, stripped.length(), "宽度应为10");
    }

    @Test
    void spacerInColumn() {
        var ui = Box.of(
                Text.of("Top"),
                Spacer.create(),
                Text.of("Bottom")
        ).flexDirection(FlexDirection.COLUMN).width(10).height(5);

        String output = Ink.renderToString(ui, 10, 5);
        String stripped = output.replaceAll("\u001B\\[[^m]*m", "");
        String[] lines = stripped.split("\n");
        assertTrue(lines.length >= 3, "至少3行: " + lines.length);
        assertTrue(lines[0].contains("Top"), "首行包含 Top");
        assertTrue(lines[lines.length - 1].contains("Bottom"), "末行包含 Bottom");
    }

    // ===== Newline =====

    @Test
    void newlineInsertsLineBreak() {
        var ui = Text.of("A", Newline.create(), "B");
        String output = Ink.renderToString(ui, 20, 3);
        String stripped = output.replaceAll("\u001B\\[[^m]*m", "");
        assertTrue(stripped.contains("A"), "应包含 A");
        assertTrue(stripped.contains("B"), "应包含 B");
        String[] lines = stripped.split("\n");
        assertTrue(lines.length >= 2, "换行后至少2行: " + stripped);
    }

    @Test
    void newlineMultipleCount() {
        var ui = Text.of("A", Newline.create(3), "B");
        String output = Ink.renderToString(ui, 20, 6);
        String stripped = output.replaceAll("\u001B\\[[^m]*m", "");
        String[] lines = stripped.split("\n", -1);
        // A + 3 个空行 + B = 至少 5 行
        assertTrue(lines.length >= 4, "3个换行符应产生至少4行: " + lines.length);
    }

    // ===== Transform =====

    @Test
    void transformUpperCase() {
        var ui = Transform.of(Text.of("hello world"))
                .transform((line, idx) -> line.toUpperCase());

        String output = Ink.renderToString(ui, 20, 1);
        String stripped = output.replaceAll("\u001B\\[[^m]*m", "");
        assertTrue(stripped.contains("HELLO WORLD"),
                "transform 应将文本转大写: " + stripped);
    }

    @Test
    void transformWithIndex() {
        var ui = Transform.of(
                Box.of(
                        Text.of("line0"),
                        Text.of("line1"),
                        Text.of("line2")
                ).flexDirection(FlexDirection.COLUMN).width(10)
        ).transform((line, idx) -> idx + ":" + line);

        String output = Ink.renderToString(ui, 10, 3);
        String stripped = output.replaceAll("\u001B\\[[^m]*m", "");
        assertTrue(stripped.contains("0:"), "行索引 0: " + stripped);
        assertTrue(stripped.contains("1:"), "行索引 1: " + stripped);
    }

    // ===== Static =====

    @Test
    void staticRendersAllItems() {
        var items = List.of("log1", "log2", "log3");
        var ui = Static.<String>of(items)
                .render((item, idx) -> Text.of("[" + idx + "] " + item));

        String output = Ink.renderToString(ui, 30, 5);
        String stripped = output.replaceAll("\u001B\\[[^m]*m", "");
        assertTrue(stripped.contains("[0] log1"), "应包含 log1: " + stripped);
        assertTrue(stripped.contains("[1] log2"), "应包含 log2: " + stripped);
        assertTrue(stripped.contains("[2] log3"), "应包含 log3: " + stripped);
    }

    @Test
    void staticSkipsPreviousItems() {
        var items = List.of("old1", "old2", "new1");
        // previousCount=2: 只渲染索引 2 开始的项
        var ui = Static.<String>of(items, 2)
                .render((item, idx) -> Text.of(item));

        String output = Ink.renderToString(ui, 20, 3);
        String stripped = output.replaceAll("\u001B\\[[^m]*m", "");
        assertFalse(stripped.contains("old1"), "不应包含旧项 old1: " + stripped);
        assertFalse(stripped.contains("old2"), "不应包含旧项 old2: " + stripped);
        assertTrue(stripped.contains("new1"), "应包含新项 new1: " + stripped);
    }

    @Test
    void staticItemCount() {
        var items = List.of("a", "b", "c");
        var staticComp = Static.<String>of(items)
                .render((item, idx) -> Text.of(item));
        assertEquals(3, staticComp.getItemCount());
    }

    // ===== FocusManager =====

    @Test
    void focusManagerRegisterAndNavigate() {
        var fm = new FocusManager();

        var f1 = new TestFocusable("input1", false);
        var f2 = new TestFocusable("input2", false);
        var f3 = new TestFocusable("input3", false);

        fm.register(f1);
        fm.register(f2);
        fm.register(f3);

        assertNull(fm.getActiveFocusId(), "初始无焦点");
        assertEquals(3, fm.getFocusableCount());

        // focusNext 从头开始
        fm.focusNext();
        assertEquals("input1", fm.getActiveFocusId());
        assertTrue(f1.focused);

        fm.focusNext();
        assertEquals("input2", fm.getActiveFocusId());
        assertFalse(f1.focused);
        assertTrue(f2.focused);

        fm.focusNext();
        assertEquals("input3", fm.getActiveFocusId());

        // 循环回到第一个
        fm.focusNext();
        assertEquals("input1", fm.getActiveFocusId());
    }

    @Test
    void focusManagerPrevious() {
        var fm = new FocusManager();
        fm.register(new TestFocusable("a", false));
        fm.register(new TestFocusable("b", false));
        fm.register(new TestFocusable("c", false));

        // focusPrevious 从末尾开始
        fm.focusPrevious();
        assertEquals("c", fm.getActiveFocusId());

        fm.focusPrevious();
        assertEquals("b", fm.getActiveFocusId());

        fm.focusPrevious();
        assertEquals("a", fm.getActiveFocusId());

        // 循环回到最后
        fm.focusPrevious();
        assertEquals("c", fm.getActiveFocusId());
    }

    @Test
    void focusManagerAutoFocus() {
        var fm = new FocusManager();
        fm.register(new TestFocusable("normal", false));
        assertNull(fm.getActiveFocusId());

        fm.register(new TestFocusable("auto", true));
        assertEquals("auto", fm.getActiveFocusId());
    }

    @Test
    void focusManagerDeactivate() {
        var fm = new FocusManager();
        fm.register(new TestFocusable("a", false));
        fm.register(new TestFocusable("b", false));
        fm.register(new TestFocusable("c", false));

        fm.focus("b");
        assertEquals("b", fm.getActiveFocusId());

        // 停用 b，应自动跳到下一个
        fm.deactivate("b");
        assertEquals("c", fm.getActiveFocusId());

        // 导航不再经过 b
        fm.focusNext();
        assertEquals("a", fm.getActiveFocusId());
        fm.focusNext();
        assertEquals("c", fm.getActiveFocusId());
    }

    @Test
    void focusManagerUnregister() {
        var fm = new FocusManager();
        fm.register(new TestFocusable("x", false));
        fm.register(new TestFocusable("y", false));

        fm.focus("x");
        assertEquals("x", fm.getActiveFocusId());

        fm.unregister("x");
        assertNull(fm.getActiveFocusId());
        assertEquals(1, fm.getFocusableCount());
    }

    @Test
    void focusManagerDisable() {
        var fm = new FocusManager();
        fm.register(new TestFocusable("a", false));

        fm.disableFocus();
        fm.focusNext();
        assertNull(fm.getActiveFocusId(), "焦点系统禁用后不应导航");

        fm.enableFocus();
        fm.focusNext();
        assertEquals("a", fm.getActiveFocusId());
    }

    @Test
    void shiftTabParsedCorrectly() {
        // ESC[Z 是 Shift+Tab 的转义序列
        var result = io.mybatis.jink.input.KeyParser.parseEscapeSequence("[Z");
        assertEquals("tab", result.name());
        assertTrue(result.shift(), "应包含 shift 修饰");
    }

    // ===== 辅助类 =====

    static class TestFocusable implements Focusable {
        final String id;
        final boolean autoFocus;
        boolean focused = false;

        TestFocusable(String id, boolean autoFocus) {
            this.id = id;
            this.autoFocus = autoFocus;
        }

        @Override
        public String getFocusId() { return id; }

        @Override
        public boolean isAutoFocus() { return autoFocus; }

        @Override
        public void onFocusChange(boolean focused) { this.focused = focused; }
    }
}
