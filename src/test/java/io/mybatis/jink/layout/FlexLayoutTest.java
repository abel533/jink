package io.mybatis.jink.layout;

import io.mybatis.jink.dom.ElementNode;
import io.mybatis.jink.dom.TextNode;
import io.mybatis.jink.style.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlexLayoutTest {

    @Test
    void singleBoxFullWidth() {
        var root = ElementNode.createRoot();
        var box = ElementNode.createBox();
        root.appendChild(box);

        FlexLayout.calculateLayout(root, 80);

        assertEquals(80, root.getComputedWidth());
        assertEquals(80, box.getComputedWidth());
    }

    @Test
    void columnDirectionStacksVertically() {
        var root = ElementNode.createRoot();
        root.setStyle(Style.builder().flexDirection(FlexDirection.COLUMN).build());

        var child1 = ElementNode.createBox();
        child1.setStyle(Style.builder().height(3).build());
        var child2 = ElementNode.createBox();
        child2.setStyle(Style.builder().height(5).build());

        root.appendChild(child1);
        root.appendChild(child2);

        FlexLayout.calculateLayout(root, 80);

        assertEquals(0, child1.getComputedTop());
        assertEquals(3, child2.getComputedTop());
        assertEquals(80, child1.getComputedWidth());
        assertEquals(80, child2.getComputedWidth());
        assertEquals(8, root.getComputedHeight());
    }

    @Test
    void rowDirectionSideBySide() {
        var root = ElementNode.createRoot();
        root.setStyle(Style.builder().flexDirection(FlexDirection.ROW).build());

        var child1 = ElementNode.createBox();
        child1.setStyle(Style.builder().width(20).height(3).build());
        var child2 = ElementNode.createBox();
        child2.setStyle(Style.builder().width(30).height(5).build());

        root.appendChild(child1);
        root.appendChild(child2);

        FlexLayout.calculateLayout(root, 80);

        assertEquals(0, child1.getComputedLeft());
        assertEquals(20, child2.getComputedLeft());
        assertEquals(20, child1.getComputedWidth());
        assertEquals(30, child2.getComputedWidth());
    }

    @Test
    void flexGrowDistribution() {
        var root = ElementNode.createRoot();
        root.setStyle(Style.builder().flexDirection(FlexDirection.ROW).build());

        var child1 = ElementNode.createBox();
        child1.setStyle(Style.builder().flexGrow(1).height(3).build());
        var child2 = ElementNode.createBox();
        child2.setStyle(Style.builder().flexGrow(1).height(3).build());

        root.appendChild(child1);
        root.appendChild(child2);

        FlexLayout.calculateLayout(root, 80);

        // 每个子节点获得一半宽度
        assertEquals(40, child1.getComputedWidth());
        assertEquals(40, child2.getComputedWidth());
    }

    @Test
    void paddingAffectsContentArea() {
        var root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.COLUMN)
                .padding(2)
                .build());

        var child = ElementNode.createBox();
        child.setStyle(Style.builder().height(3).build());
        root.appendChild(child);

        FlexLayout.calculateLayout(root, 80);

        // 子节点应该被 padding 偏移
        assertEquals(2, child.getComputedLeft());
        assertEquals(2, child.getComputedTop());
        // 子节点宽度 = 80 - 2*2 (padding)
        assertEquals(76, child.getComputedWidth());
    }

    @Test
    void borderAffectsLayout() {
        var root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.COLUMN)
                .borderStyle(BorderStyle.SINGLE)
                .build());

        var child = ElementNode.createBox();
        child.setStyle(Style.builder().height(3).build());
        root.appendChild(child);

        FlexLayout.calculateLayout(root, 80);

        // 子节点被 border(1) 偏移
        assertEquals(1, child.getComputedLeft());
        assertEquals(1, child.getComputedTop());
        // 子节点宽度 = 80 - 2 (border)
        assertEquals(78, child.getComputedWidth());
    }

    @Test
    void displayNoneSkipsLayout() {
        var root = ElementNode.createRoot();
        root.setStyle(Style.builder().flexDirection(FlexDirection.COLUMN).build());

        var child1 = ElementNode.createBox();
        child1.setStyle(Style.builder().height(3).build());
        var hidden = ElementNode.createBox();
        hidden.setStyle(Style.builder().display(Display.NONE).height(5).build());
        var child2 = ElementNode.createBox();
        child2.setStyle(Style.builder().height(4).build());

        root.appendChild(child1);
        root.appendChild(hidden);
        root.appendChild(child2);

        FlexLayout.calculateLayout(root, 80);

        assertEquals(0, child1.getComputedTop());
        // hidden 被跳过，child2 紧跟 child1
        assertEquals(3, child2.getComputedTop());
        assertEquals(0, hidden.getComputedWidth());
    }

    @Test
    void textNodeMeasurement() {
        var root = ElementNode.createRoot();
        root.setStyle(Style.builder().flexDirection(FlexDirection.COLUMN).build());

        var text = ElementNode.createText();
        text.appendChild(new TextNode("Hello, World!"));
        root.appendChild(text);

        FlexLayout.calculateLayout(root, 80);

        assertEquals(1, text.getComputedHeight()); // 单行文本
    }

    @Test
    void textNodeWrapping() {
        var root = ElementNode.createRoot();
        root.setStyle(Style.builder().flexDirection(FlexDirection.COLUMN).build());

        // 创建超长文本
        var text = ElementNode.createText();
        text.appendChild(new TextNode("A".repeat(200)));
        root.appendChild(text);

        FlexLayout.calculateLayout(root, 80);

        // 200字符在80列宽度下需要3行
        assertEquals(3, text.getComputedHeight());
    }

    @Test
    void justifyContentCenter() {
        var root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.ROW)
                .justifyContent(JustifyContent.CENTER)
                .build());

        var child = ElementNode.createBox();
        child.setStyle(Style.builder().width(20).height(3).build());
        root.appendChild(child);

        FlexLayout.calculateLayout(root, 80);

        assertEquals(30, child.getComputedLeft()); // (80-20)/2 = 30
    }

    @Test
    void justifyContentSpaceBetween() {
        var root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.ROW)
                .justifyContent(JustifyContent.SPACE_BETWEEN)
                .build());

        var child1 = ElementNode.createBox();
        child1.setStyle(Style.builder().width(10).height(3).build());
        var child2 = ElementNode.createBox();
        child2.setStyle(Style.builder().width(10).height(3).build());

        root.appendChild(child1);
        root.appendChild(child2);

        FlexLayout.calculateLayout(root, 80);

        assertEquals(0, child1.getComputedLeft());
        assertEquals(70, child2.getComputedLeft()); // 80-10 = 70
    }

    @Test
    void gapBetweenChildren() {
        var root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.COLUMN)
                .gap(1)
                .build());

        var child1 = ElementNode.createBox();
        child1.setStyle(Style.builder().height(3).build());
        var child2 = ElementNode.createBox();
        child2.setStyle(Style.builder().height(4).build());

        root.appendChild(child1);
        root.appendChild(child2);

        FlexLayout.calculateLayout(root, 80);

        assertEquals(0, child1.getComputedTop());
        assertEquals(4, child2.getComputedTop()); // 3 + 1(gap)
    }

    @Test
    void squashTextContent() {
        var text = ElementNode.createText();
        text.appendChild(new TextNode("Hello"));
        var virtualText = ElementNode.createVirtualText();
        virtualText.appendChild(new TextNode(" World"));
        text.appendChild(virtualText);

        assertEquals("Hello World", FlexLayout.squashTextContent(text));
    }

    @Test
    void nestedLayout() {
        var root = ElementNode.createRoot();
        root.setStyle(Style.builder().flexDirection(FlexDirection.COLUMN).build());

        var header = ElementNode.createBox();
        header.setStyle(Style.builder()
                .height(3)
                .borderStyle(BorderStyle.SINGLE)
                .build());
        var headerText = ElementNode.createText();
        headerText.appendChild(new TextNode("Header"));
        header.appendChild(headerText);

        var body = ElementNode.createBox();
        body.setStyle(Style.builder().flexGrow(1).build());

        root.appendChild(header);
        root.appendChild(body);

        FlexLayout.calculateLayout(root, 80);

        assertEquals(0, header.getComputedTop());
        assertEquals(3, body.getComputedTop());
        assertEquals(80, header.getComputedWidth());
        assertEquals(80, body.getComputedWidth());
    }
}
