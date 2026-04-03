package io.mybatis.jink.render;

import io.mybatis.jink.ansi.AnsiStringUtils;
import io.mybatis.jink.dom.ElementNode;
import io.mybatis.jink.dom.TextNode;
import io.mybatis.jink.layout.FlexLayout;
import io.mybatis.jink.style.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VirtualScreenTest {

    @Test
    void basicWrite() {
        var screen = new VirtualScreen(10, 3);
        screen.write(0, 0, "Hello");

        String output = screen.render();
        String firstLine = output.split("\n")[0];
        assertEquals("Hello", AnsiStringUtils.stripAnsi(firstLine));
    }

    @Test
    void writeWithStyle() {
        var screen = new VirtualScreen(20, 1);
        var style = Style.builder().bold(true).color(Color.GREEN).build();
        screen.write(0, 0, "Bold Green", style);

        String output = screen.render();
        assertTrue(output.contains("\u001B[1m"));   // bold
        assertTrue(output.contains("\u001B[32m"));  // green
        assertTrue(output.contains("Bold Green"));
    }

    @Test
    void fillArea() {
        var screen = new VirtualScreen(5, 3);
        screen.fill(0, 0, 5, 3, '#', null);

        String output = screen.render();
        for (String line : output.split("\n")) {
            assertEquals("#####", AnsiStringUtils.stripAnsi(line));
        }
    }

    @Test
    void clipping() {
        var screen = new VirtualScreen(10, 3);
        screen.pushClip(2, 0, 5, 2);
        screen.write(0, 0, "0123456789");
        screen.popClip();

        String output = screen.render();
        String line = output.split("\n")[0];
        String stripped = AnsiStringUtils.stripAnsi(line);
        // 只有位置 2-6 应该有内容
        assertEquals(' ', stripped.charAt(0));
        assertEquals(' ', stripped.charAt(1));
        assertEquals('2', stripped.charAt(2));
        assertEquals('6', stripped.charAt(6));
    }
}

class NodeRendererTest {

    @Test
    void renderSimpleText() {
        var root = ElementNode.createRoot();
        root.setStyle(Style.builder().flexDirection(FlexDirection.COLUMN).build());

        var text = ElementNode.createText();
        text.appendChild(new TextNode("Hello, World!"));
        root.appendChild(text);

        FlexLayout.calculateLayout(root, 40);
        VirtualScreen screen = NodeRenderer.render(root);
        String output = screen.render();
        String stripped = AnsiStringUtils.stripAnsi(output);

        assertTrue(stripped.contains("Hello, World!"));
    }

    @Test
    void renderWithBorder() {
        var root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.COLUMN)
                .borderStyle(BorderStyle.SINGLE)
                .width(20)
                .height(3)
                .build());

        var text = ElementNode.createText();
        text.appendChild(new TextNode("Hi"));
        root.appendChild(text);

        FlexLayout.calculateLayout(root, 20);
        VirtualScreen screen = NodeRenderer.render(root);
        String output = screen.render();
        String stripped = AnsiStringUtils.stripAnsi(output);
        String[] lines = stripped.split("\n");

        // 第一行应该是上边框
        assertTrue(lines[0].startsWith("┌"));
        assertTrue(lines[0].endsWith("┐"));
        // 中间行有内容和侧边框
        assertTrue(lines[1].startsWith("│"));
        assertTrue(lines[1].contains("Hi"));
        // 最后一行是下边框
        assertTrue(lines[2].startsWith("└"));
        assertTrue(lines[2].endsWith("┘"));
    }

    @Test
    void renderColumnLayout() {
        var root = ElementNode.createRoot();
        root.setStyle(Style.builder().flexDirection(FlexDirection.COLUMN).build());

        var line1 = ElementNode.createText();
        line1.appendChild(new TextNode("Line 1"));
        var line2 = ElementNode.createText();
        line2.appendChild(new TextNode("Line 2"));

        root.appendChild(line1);
        root.appendChild(line2);

        FlexLayout.calculateLayout(root, 40);
        VirtualScreen screen = NodeRenderer.render(root);
        String output = screen.render();
        String stripped = AnsiStringUtils.stripAnsi(output);

        assertTrue(stripped.contains("Line 1"));
        assertTrue(stripped.contains("Line 2"));

        String[] lines = stripped.split("\n");
        assertTrue(lines[0].contains("Line 1"));
        assertTrue(lines[1].contains("Line 2"));
    }

    @Test
    void renderWithPadding() {
        var root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.COLUMN)
                .paddingLeft(2)
                .paddingTop(1)
                .width(20)
                .build());

        var text = ElementNode.createText();
        text.appendChild(new TextNode("Padded"));
        root.appendChild(text);

        FlexLayout.calculateLayout(root, 20);
        VirtualScreen screen = NodeRenderer.render(root);
        String output = screen.render();
        String stripped = AnsiStringUtils.stripAnsi(output);
        String[] lines = stripped.split("\n");

        // 第一行应该是空的（paddingTop）
        assertTrue(lines[0].isBlank());
        // 第二行应该有左 padding
        assertTrue(lines[1].startsWith("  Padded"));
    }

    @Test
    void renderBackgroundColor() {
        var root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.COLUMN)
                .backgroundColor(Color.BLUE)
                .width(10)
                .height(3)
                .build());

        FlexLayout.calculateLayout(root, 10);
        VirtualScreen screen = NodeRenderer.render(root);
        String output = screen.render();

        // 应该包含蓝色背景 ANSI 序列
        assertTrue(output.contains("\u001B[44m"));
    }

    @Test
    void renderNestedBoxes() {
        var root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.COLUMN)
                .borderStyle(BorderStyle.SINGLE)
                .width(30)
                .build());

        var inner = ElementNode.createBox();
        inner.setStyle(Style.builder()
                .flexDirection(FlexDirection.COLUMN)
                .borderStyle(BorderStyle.ROUND)
                .build());

        var text = ElementNode.createText();
        text.appendChild(new TextNode("Nested"));
        inner.appendChild(text);
        root.appendChild(inner);

        FlexLayout.calculateLayout(root, 30);
        VirtualScreen screen = NodeRenderer.render(root);
        String output = screen.render();
        String stripped = AnsiStringUtils.stripAnsi(output);

        assertTrue(stripped.contains("┌")); // outer border
        assertTrue(stripped.contains("╭")); // inner round border
        assertTrue(stripped.contains("Nested"));
    }

    @Test
    void renderTextTruncateEnd() {
        // 10 列宽的容器，放超长文本，TRUNCATE_END 应截断末尾加 …
        var root = ElementNode.createRoot();
        root.setStyle(Style.builder().flexDirection(FlexDirection.COLUMN).width(10).build());

        var text = ElementNode.createText();
        text.setStyle(Style.builder().textWrap(TextWrap.TRUNCATE_END).build());
        text.appendChild(new TextNode("Hello World 12345"));
        root.appendChild(text);

        FlexLayout.calculateLayout(root, 10);
        VirtualScreen screen = NodeRenderer.render(root);
        String output = screen.render();
        String stripped = AnsiStringUtils.stripAnsi(output).split("\n")[0];

        // 前 9 个字符 + …
        assertEquals(10, AnsiStringUtils.visibleWidth(stripped.trim()));
        assertTrue(stripped.trim().endsWith("…"));
        assertTrue(stripped.trim().startsWith("Hello Wor"));
    }

    @Test
    void renderTextTruncateStart() {
        var root = ElementNode.createRoot();
        root.setStyle(Style.builder().flexDirection(FlexDirection.COLUMN).width(10).build());

        var text = ElementNode.createText();
        text.setStyle(Style.builder().textWrap(TextWrap.TRUNCATE_START).build());
        text.appendChild(new TextNode("Hello World 12345"));
        root.appendChild(text);

        FlexLayout.calculateLayout(root, 10);
        VirtualScreen screen = NodeRenderer.render(root);
        String output = screen.render();
        String stripped = AnsiStringUtils.stripAnsi(output).split("\n")[0];

        // … + 末尾 9 个字符
        assertEquals(10, AnsiStringUtils.visibleWidth(stripped.trim()));
        assertTrue(stripped.trim().startsWith("…"));
        assertTrue(stripped.trim().endsWith("12345"));
    }

    @Test
    void renderTextTruncateMiddle() {
        var root = ElementNode.createRoot();
        root.setStyle(Style.builder().flexDirection(FlexDirection.COLUMN).width(10).build());

        var text = ElementNode.createText();
        text.setStyle(Style.builder().textWrap(TextWrap.TRUNCATE_MIDDLE).build());
        text.appendChild(new TextNode("Hello World 12345"));
        root.appendChild(text);

        FlexLayout.calculateLayout(root, 10);
        VirtualScreen screen = NodeRenderer.render(root);
        String output = screen.render();
        String stripped = AnsiStringUtils.stripAnsi(output).split("\n")[0];

        // 应包含 … 在中间
        assertTrue(stripped.trim().contains("…"));
        assertTrue(stripped.trim().startsWith("Hell"));
        assertTrue(stripped.trim().endsWith("2345"));
    }

    @Test
    void renderTextTruncateShortTextNoTruncation() {
        // 文本比容器短时不应截断
        var root = ElementNode.createRoot();
        root.setStyle(Style.builder().flexDirection(FlexDirection.COLUMN).width(20).build());

        var text = ElementNode.createText();
        text.setStyle(Style.builder().textWrap(TextWrap.TRUNCATE_END).build());
        text.appendChild(new TextNode("Short"));
        root.appendChild(text);

        FlexLayout.calculateLayout(root, 20);
        VirtualScreen screen = NodeRenderer.render(root);
        String output = screen.render();
        String stripped = AnsiStringUtils.stripAnsi(output).split("\n")[0];

        assertTrue(stripped.contains("Short"));
        assertFalse(stripped.contains("…"));
    }

    @Test
    void renderTextTruncateMultiLine() {
        // 多行文本中，每行独立截断
        var root = ElementNode.createRoot();
        root.setStyle(Style.builder().flexDirection(FlexDirection.COLUMN).width(10).build());

        var text = ElementNode.createText();
        text.setStyle(Style.builder().textWrap(TextWrap.TRUNCATE_END).build());
        text.appendChild(new TextNode("Long line one\nLong line two"));
        root.appendChild(text);

        FlexLayout.calculateLayout(root, 10);
        VirtualScreen screen = NodeRenderer.render(root);
        String output = screen.render();
        String[] lines = AnsiStringUtils.stripAnsi(output).split("\n");

        // 两行，每行都应该截断
        assertTrue(lines.length >= 2);
        assertTrue(lines[0].trim().endsWith("…"));
        assertTrue(lines[1].trim().endsWith("…"));
    }

    @Test
    void renderTextTruncateOneLine() {
        // 截断模式下，文本高度应为逻辑行数（不换行）
        var root = ElementNode.createRoot();
        root.setStyle(Style.builder().flexDirection(FlexDirection.COLUMN).width(10).build());

        var text = ElementNode.createText();
        text.setStyle(Style.builder().textWrap(TextWrap.TRUNCATE_END).build());
        text.appendChild(new TextNode("Hello World 12345")); // 17 字符，但不应换行
        root.appendChild(text);

        FlexLayout.calculateLayout(root, 10);
        // 截断模式下，高度应为 1（单行），而非换行后的 2 行
        assertEquals(1, text.getComputedHeight());
    }

    @Test
    void renderPerSideBorderColor() {
        // 验证每边独立边框颜色正确渲染
        var root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.COLUMN)
                .borderStyle(BorderStyle.SINGLE)
                .borderTopColor(Color.RED)
                .borderBottomColor(Color.GREEN)
                .borderLeftColor(Color.BLUE)
                .borderRightColor(Color.YELLOW)
                .width(10)
                .height(3)
                .build());

        FlexLayout.calculateLayout(root, 10);
        VirtualScreen screen = NodeRenderer.render(root);
        String output = screen.render();

        // 应包含红色（顶部）和绿色（底部）的 ANSI 序列
        assertTrue(output.contains("\u001B[31m")); // RED
        assertTrue(output.contains("\u001B[32m")); // GREEN
        assertTrue(output.contains("\u001B[34m")); // BLUE
        assertTrue(output.contains("\u001B[33m")); // YELLOW
    }

    @Test
    void renderBorderColorFallback() {
        // borderColor 作为 fallback，未设置独立颜色的边使用 borderColor
        var root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.COLUMN)
                .borderStyle(BorderStyle.SINGLE)
                .borderColor(Color.CYAN)
                .borderTopColor(Color.RED)
                .width(10)
                .height(3)
                .build());

        // effectiveBorderTopColor 应为 RED
        assertEquals(Color.RED, root.getStyle().effectiveBorderTopColor());
        // 其余三边应 fallback 到 CYAN
        assertEquals(Color.CYAN, root.getStyle().effectiveBorderRightColor());
        assertEquals(Color.CYAN, root.getStyle().effectiveBorderBottomColor());
        assertEquals(Color.CYAN, root.getStyle().effectiveBorderLeftColor());
    }
}
