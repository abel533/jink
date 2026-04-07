package io.mybatis.jink.layout;

import io.mybatis.jink.dom.ElementNode;
import io.mybatis.jink.dom.TextNode;
import io.mybatis.jink.style.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlexLayoutTest {

    @Test
    void singleBoxFullWidth() {
        ElementNode root = ElementNode.createRoot();
        ElementNode box = ElementNode.createBox();
        root.appendChild(box);

        FlexLayout.calculateLayout(root, 80);

        assertEquals(80, root.getComputedWidth());
        assertEquals(80, box.getComputedWidth());
    }

    @Test
    void columnDirectionStacksVertically() {
        ElementNode root = ElementNode.createRoot();
        root.setStyle(Style.builder().flexDirection(FlexDirection.COLUMN).build());

        ElementNode child1 = ElementNode.createBox();
        child1.setStyle(Style.builder().height(3).build());
        ElementNode child2 = ElementNode.createBox();
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
        ElementNode root = ElementNode.createRoot();
        root.setStyle(Style.builder().flexDirection(FlexDirection.ROW).build());

        ElementNode child1 = ElementNode.createBox();
        child1.setStyle(Style.builder().width(20).height(3).build());
        ElementNode child2 = ElementNode.createBox();
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
        ElementNode root = ElementNode.createRoot();
        root.setStyle(Style.builder().flexDirection(FlexDirection.ROW).build());

        ElementNode child1 = ElementNode.createBox();
        child1.setStyle(Style.builder().flexGrow(1).height(3).build());
        ElementNode child2 = ElementNode.createBox();
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
        ElementNode root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.COLUMN)
                .padding(2)
                .build());

        ElementNode child = ElementNode.createBox();
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
        ElementNode root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.COLUMN)
                .borderStyle(BorderStyle.SINGLE)
                .build());

        ElementNode child = ElementNode.createBox();
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
        ElementNode root = ElementNode.createRoot();
        root.setStyle(Style.builder().flexDirection(FlexDirection.COLUMN).build());

        ElementNode child1 = ElementNode.createBox();
        child1.setStyle(Style.builder().height(3).build());
        ElementNode hidden = ElementNode.createBox();
        hidden.setStyle(Style.builder().display(Display.NONE).height(5).build());
        ElementNode child2 = ElementNode.createBox();
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
        ElementNode root = ElementNode.createRoot();
        root.setStyle(Style.builder().flexDirection(FlexDirection.COLUMN).build());

        ElementNode text = ElementNode.createText();
        text.appendChild(new TextNode("Hello, World!"));
        root.appendChild(text);

        FlexLayout.calculateLayout(root, 80);

        assertEquals(1, text.getComputedHeight()); // 单行文本
    }

    @Test
    void textNodeWrapping() {
        ElementNode root = ElementNode.createRoot();
        root.setStyle(Style.builder().flexDirection(FlexDirection.COLUMN).build());

        // 创建超长文本
        ElementNode text = ElementNode.createText();
        text.appendChild(new TextNode("A".repeat(200)));
        root.appendChild(text);

        FlexLayout.calculateLayout(root, 80);

        // 200字符在80列宽度下需要3行
        assertEquals(3, text.getComputedHeight());
    }

    @Test
    void justifyContentCenter() {
        ElementNode root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.ROW)
                .justifyContent(JustifyContent.CENTER)
                .build());

        ElementNode child = ElementNode.createBox();
        child.setStyle(Style.builder().width(20).height(3).build());
        root.appendChild(child);

        FlexLayout.calculateLayout(root, 80);

        assertEquals(30, child.getComputedLeft()); // (80-20)/2 = 30
    }

    @Test
    void justifyContentSpaceBetween() {
        ElementNode root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.ROW)
                .justifyContent(JustifyContent.SPACE_BETWEEN)
                .build());

        ElementNode child1 = ElementNode.createBox();
        child1.setStyle(Style.builder().width(10).height(3).build());
        ElementNode child2 = ElementNode.createBox();
        child2.setStyle(Style.builder().width(10).height(3).build());

        root.appendChild(child1);
        root.appendChild(child2);

        FlexLayout.calculateLayout(root, 80);

        assertEquals(0, child1.getComputedLeft());
        assertEquals(70, child2.getComputedLeft()); // 80-10 = 70
    }

    @Test
    void gapBetweenChildren() {
        ElementNode root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.COLUMN)
                .gap(1)
                .build());

        ElementNode child1 = ElementNode.createBox();
        child1.setStyle(Style.builder().height(3).build());
        ElementNode child2 = ElementNode.createBox();
        child2.setStyle(Style.builder().height(4).build());

        root.appendChild(child1);
        root.appendChild(child2);

        FlexLayout.calculateLayout(root, 80);

        assertEquals(0, child1.getComputedTop());
        assertEquals(4, child2.getComputedTop()); // 3 + 1(gap)
    }

    @Test
    void squashTextContent() {
        ElementNode text = ElementNode.createText();
        text.appendChild(new TextNode("Hello"));
        ElementNode virtualText = ElementNode.createVirtualText();
        virtualText.appendChild(new TextNode(" World"));
        text.appendChild(virtualText);

        assertEquals("Hello World", FlexLayout.squashTextContent(text));
    }

    @Test
    void nestedLayout() {
        ElementNode root = ElementNode.createRoot();
        root.setStyle(Style.builder().flexDirection(FlexDirection.COLUMN).build());

        ElementNode header = ElementNode.createBox();
        header.setStyle(Style.builder()
                .height(3)
                .borderStyle(BorderStyle.SINGLE)
                .build());
        ElementNode headerText = ElementNode.createText();
        headerText.appendChild(new TextNode("Header"));
        header.appendChild(headerText);

        ElementNode body = ElementNode.createBox();
        body.setStyle(Style.builder().flexGrow(1).build());

        root.appendChild(header);
        root.appendChild(body);

        FlexLayout.calculateLayout(root, 80);

        assertEquals(0, header.getComputedTop());
        assertEquals(3, body.getComputedTop());
        assertEquals(80, header.getComputedWidth());
        assertEquals(80, body.getComputedWidth());
    }

    // ===== 百分比尺寸测试 =====

    @Test
    void percentWidth() {
        // 子节点宽度 50%，容器 100 列
        ElementNode root = ElementNode.createRoot();
        root.setStyle(Style.builder().flexDirection(FlexDirection.ROW).build());

        ElementNode child = ElementNode.createBox();
        child.setStyle(Style.builder().widthPercent(50).height(1).build());
        root.appendChild(child);

        FlexLayout.calculateLayout(root, 100);

        assertEquals(50, child.getComputedWidth());
    }

    @Test
    void percentWidthTwoChildren() {
        // 两个子节点各 30%，容器 100 列
        ElementNode root = ElementNode.createRoot();
        root.setStyle(Style.builder().flexDirection(FlexDirection.ROW).build());

        ElementNode child1 = ElementNode.createBox();
        child1.setStyle(Style.builder().widthPercent(30).height(1).build());
        ElementNode child2 = ElementNode.createBox();
        child2.setStyle(Style.builder().widthPercent(30).height(1).build());

        root.appendChild(child1);
        root.appendChild(child2);

        FlexLayout.calculateLayout(root, 100);

        assertEquals(30, child1.getComputedWidth());
        assertEquals(30, child2.getComputedWidth());
        assertEquals(0, child1.getComputedLeft());
        assertEquals(30, child2.getComputedLeft());
    }

    @Test
    void percentEncoding() {
        // 测试百分比编码/解码
        assertEquals(-51, Style.percent(50));
        assertTrue(Style.isPercent(Style.percent(50)));
        assertEquals(50, Style.getPercent(Style.percent(50)));

        assertFalse(Style.isPercent(Style.AUTO));
        assertFalse(Style.isPercent(0));
        assertFalse(Style.isPercent(100));
    }

    @Test
    void percentHeight() {
        // 容器高度 20，子节点高度 50%
        ElementNode root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.COLUMN)
                .height(20)
                .build());

        ElementNode child = ElementNode.createBox();
        child.setStyle(Style.builder().heightPercent(50).build());
        root.appendChild(child);

        FlexLayout.calculateLayout(root, 80);

        assertEquals(10, child.getComputedHeight());
    }

    // ===== 绝对定位测试 =====

    @Test
    void absolutePositionLeft() {
        // 绝对定位子节点，left=5, top=3
        ElementNode root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.COLUMN)
                .width(40).height(20)
                .build());

        ElementNode absChild = ElementNode.createBox();
        absChild.setStyle(Style.builder()
                .position(Position.ABSOLUTE)
                .posLeft(5).posTop(3)
                .width(10).height(5)
                .build());
        root.appendChild(absChild);

        FlexLayout.calculateLayout(root, 40);

        assertEquals(5, absChild.getComputedLeft());
        assertEquals(3, absChild.getComputedTop());
        assertEquals(10, absChild.getComputedWidth());
        assertEquals(5, absChild.getComputedHeight());
    }

    @Test
    void absolutePositionRight() {
        // right=0 → 贴右边
        ElementNode root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.COLUMN)
                .width(40).height(20)
                .build());

        ElementNode absChild = ElementNode.createBox();
        absChild.setStyle(Style.builder()
                .position(Position.ABSOLUTE)
                .posRight(0).posBottom(0)
                .width(10).height(5)
                .build());
        root.appendChild(absChild);

        FlexLayout.calculateLayout(root, 40);

        // 40 - 0 - 10 = 30
        assertEquals(30, absChild.getComputedLeft());
        // 20 - 0 - 5 = 15
        assertEquals(15, absChild.getComputedTop());
    }

    @Test
    void absoluteDoesNotAffectFlexLayout() {
        // 绝对定位子节点不影响 flex 布局
        ElementNode root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.COLUMN)
                .width(40)
                .build());

        ElementNode flexChild = ElementNode.createBox();
        flexChild.setStyle(Style.builder().height(5).build());

        ElementNode absChild = ElementNode.createBox();
        absChild.setStyle(Style.builder()
                .position(Position.ABSOLUTE)
                .posLeft(0).posTop(0)
                .width(20).height(20)
                .build());

        ElementNode flexChild2 = ElementNode.createBox();
        flexChild2.setStyle(Style.builder().height(3).build());

        root.appendChild(flexChild);
        root.appendChild(absChild);
        root.appendChild(flexChild2);

        FlexLayout.calculateLayout(root, 40);

        // flex 子节点不受绝对定位影响
        assertEquals(0, flexChild.getComputedTop());
        assertEquals(5, flexChild2.getComputedTop()); // 紧跟在 flexChild 之后
        // 绝对定位子节点独立定位
        assertEquals(0, absChild.getComputedLeft());
        assertEquals(0, absChild.getComputedTop());
    }

    // ===== FlexWrap 测试 =====

    @Test
    void flexWrapRowBasic() {
        // 3 个 15 列宽子节点在 40 列容器中，第 3 个应换行
        ElementNode root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.ROW)
                .flexWrap(Style.FlexWrap.WRAP)
                .width(40)
                .build());

        ElementNode c1 = ElementNode.createBox();
        c1.setStyle(Style.builder().width(15).height(3).build());
        ElementNode c2 = ElementNode.createBox();
        c2.setStyle(Style.builder().width(15).height(3).build());
        ElementNode c3 = ElementNode.createBox();
        c3.setStyle(Style.builder().width(15).height(4).build());

        root.appendChild(c1);
        root.appendChild(c2);
        root.appendChild(c3);

        FlexLayout.calculateLayout(root, 40);

        // 第一行：c1 + c2 = 30 <= 40
        assertEquals(0, c1.getComputedLeft());
        assertEquals(0, c1.getComputedTop());
        assertEquals(15, c2.getComputedLeft());
        assertEquals(0, c2.getComputedTop());

        // 第二行：c3 换行
        assertEquals(0, c3.getComputedLeft());
        assertEquals(3, c3.getComputedTop()); // 第一行高度=3
    }

    @Test
    void flexWrapRowAllFit() {
        // 所有子节点都在一行内，不应换行
        ElementNode root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.ROW)
                .flexWrap(Style.FlexWrap.WRAP)
                .width(40)
                .build());

        ElementNode c1 = ElementNode.createBox();
        c1.setStyle(Style.builder().width(10).height(3).build());
        ElementNode c2 = ElementNode.createBox();
        c2.setStyle(Style.builder().width(10).height(3).build());
        ElementNode c3 = ElementNode.createBox();
        c3.setStyle(Style.builder().width(10).height(3).build());

        root.appendChild(c1);
        root.appendChild(c2);
        root.appendChild(c3);

        FlexLayout.calculateLayout(root, 40);

        // 全在第一行
        assertEquals(0, c1.getComputedLeft());
        assertEquals(10, c2.getComputedLeft());
        assertEquals(20, c3.getComputedLeft());
        assertEquals(0, c1.getComputedTop());
        assertEquals(0, c2.getComputedTop());
        assertEquals(0, c3.getComputedTop());
    }

    @Test
    void flexWrapRowHeight() {
        // 验证换行后容器高度正确
        ElementNode root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.ROW)
                .flexWrap(Style.FlexWrap.WRAP)
                .width(30)
                .build());

        ElementNode c1 = ElementNode.createBox();
        c1.setStyle(Style.builder().width(20).height(5).build());
        ElementNode c2 = ElementNode.createBox();
        c2.setStyle(Style.builder().width(20).height(3).build());

        root.appendChild(c1);
        root.appendChild(c2);

        FlexLayout.calculateLayout(root, 30);

        // 两行：5 + 3 = 8
        assertEquals(8, root.getComputedHeight());
    }

    // ===== alignContent 测试 =====

    @Test
    void alignContentCenter() {
        // 容器高 20，两行总高 6 (3+3)，CENTER 应使起始偏移 = (20-6)/2 = 7
        ElementNode root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.ROW)
                .flexWrap(Style.FlexWrap.WRAP)
                .alignContent(AlignContent.CENTER)
                .width(20)
                .height(20)
                .build());

        ElementNode c1 = ElementNode.createBox();
        c1.setStyle(Style.builder().width(15).height(3).build());
        ElementNode c2 = ElementNode.createBox();
        c2.setStyle(Style.builder().width(15).height(3).build());

        root.appendChild(c1);
        root.appendChild(c2);

        FlexLayout.calculateLayout(root, 20);

        // 两行都应居中偏移 7
        assertEquals(7, c1.getComputedTop());
        assertEquals(10, c2.getComputedTop()); // 7 + 3
    }

    @Test
    void alignContentSpaceBetween() {
        // 容器高 20，两行总高 6，SPACE_BETWEEN: 第一行 top=0，第二行 top=20-3=17
        ElementNode root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.ROW)
                .flexWrap(Style.FlexWrap.WRAP)
                .alignContent(AlignContent.SPACE_BETWEEN)
                .width(20)
                .height(20)
                .build());

        ElementNode c1 = ElementNode.createBox();
        c1.setStyle(Style.builder().width(15).height(3).build());
        ElementNode c2 = ElementNode.createBox();
        c2.setStyle(Style.builder().width(15).height(3).build());

        root.appendChild(c1);
        root.appendChild(c2);

        FlexLayout.calculateLayout(root, 20);

        assertEquals(0, c1.getComputedTop());
        // freeSpace=14, 2行之间间距=14, c2.top = 3 + 14 = 17
        assertEquals(17, c2.getComputedTop());
    }

    @Test
    void alignContentStretch() {
        // 容器高 20，两行总高 6，STRETCH: 每行多分 7 高度
        ElementNode root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.ROW)
                .flexWrap(Style.FlexWrap.WRAP)
                .alignContent(AlignContent.STRETCH)
                .alignItems(AlignItems.STRETCH)
                .width(20)
                .height(20)
                .build());

        ElementNode c1 = ElementNode.createBox();
        c1.setStyle(Style.builder().width(15).height(3).build());
        ElementNode c2 = ElementNode.createBox();
        c2.setStyle(Style.builder().width(15).height(3).build());

        root.appendChild(c1);
        root.appendChild(c2);

        FlexLayout.calculateLayout(root, 20);

        // freeSpace=14, 每行多得 7, 第一行 lineSize=10, 第二行 lineSize=10
        assertEquals(0, c1.getComputedTop());
        // c1 被 stretch 到 lineSize=10
        assertEquals(10, c1.getComputedHeight());
        assertEquals(10, c2.getComputedTop());
        assertEquals(10, c2.getComputedHeight());
    }

    // ===== baseline 对齐测试 =====

    @Test
    void alignItemsBaseline() {
        // ROW 布局中，两个不同高度的文本节点应按基线对齐
        // c1: paddingTop=2, 文本 "A" → baseline=3 (padding 2 + 行高 1)
        // c2: paddingTop=0, 文本 "B" → baseline=1
        // 最大基线=3，c2 应下移 2
        ElementNode root = ElementNode.createRoot();
        root.setStyle(Style.builder()
                .flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.BASELINE)
                .width(40)
                .build());

        ElementNode c1 = ElementNode.createText();
        c1.setStyle(Style.builder().paddingTop(2).build());
        c1.appendChild(new TextNode("A"));

        ElementNode c2 = ElementNode.createText();
        c2.setStyle(Style.builder().paddingTop(0).build());
        c2.appendChild(new TextNode("B"));

        root.appendChild(c1);
        root.appendChild(c2);

        FlexLayout.calculateLayout(root, 40);

        // c1 baseline = paddingTop(2) + 1 = 3
        // c2 baseline = paddingTop(0) + 1 = 1
        // c2 应偏移 maxBaseline - childBaseline = 3 - 1 = 2
        assertEquals(0, c1.getComputedTop());
        assertEquals(2, c2.getComputedTop());
    }

    @Test
    void computeBaselineTextNode() {
        // 直接测试 computeBaseline 方法
        ElementNode text = ElementNode.createText();
        text.setStyle(Style.builder().paddingTop(3).build());
        text.appendChild(new TextNode("Hello"));

        FlexLayout.calculateLayout(text, 20);
        int baseline = FlexLayout.computeBaseline(text);
        // paddingTop(3) + border(0) + 1 = 4
        assertEquals(4, baseline);
    }
}
