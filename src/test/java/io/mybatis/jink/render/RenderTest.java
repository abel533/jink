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
}
