package io.mybatis.jink.component;

import io.mybatis.jink.dom.ElementNode;
import io.mybatis.jink.style.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Box 组件 - Flexbox 容器。
 * 对应 ink 的 Box 组件。
 * <p>
 * 使用示例:
 * <pre>
 * Box.of(
 *     Text.of("Hello").color(Color.Basic.GREEN),
 *     Text.of("World").bold()
 * ).flexDirection(FlexDirection.COLUMN)
 *  .padding(1)
 *  .borderStyle(BorderStyle.SINGLE)
 * </pre>
 */
public class Box implements Renderable {

    private final List<Renderable> children = new ArrayList<>();
    private final Style.Builder style = Style.builder();

    private Box() {}

    /**
     * 创建 Box 并添加子元素
     */
    public static Box of(Renderable... children) {
        Box box = new Box();
        Collections.addAll(box.children, children);
        return box;
    }

    /**
     * 追加子元素
     */
    public Box add(Renderable child) {
        children.add(child);
        return this;
    }

    // === Flex 方向与对齐 ===

    public Box flexDirection(FlexDirection direction) {
        style.flexDirection(direction);
        return this;
    }

    public Box justifyContent(JustifyContent justifyContent) {
        style.justifyContent(justifyContent);
        return this;
    }

    public Box alignItems(AlignItems alignItems) {
        style.alignItems(alignItems);
        return this;
    }

    // === 尺寸 ===

    public Box width(int width) {
        style.width(width);
        return this;
    }

    public Box height(int height) {
        style.height(height);
        return this;
    }

    public Box minWidth(int minWidth) {
        style.minWidth(minWidth);
        return this;
    }

    public Box minHeight(int minHeight) {
        style.minHeight(minHeight);
        return this;
    }

    // === Flex 属性 ===

    public Box flexGrow(int flexGrow) {
        style.flexGrow(flexGrow);
        return this;
    }

    public Box flexShrink(int flexShrink) {
        style.flexShrink(flexShrink);
        return this;
    }

    public Box flexBasis(int flexBasis) {
        style.flexBasis(flexBasis);
        return this;
    }

    // === 内边距 ===

    public Box padding(int padding) {
        style.padding(padding);
        return this;
    }

    public Box paddingX(int padding) {
        style.paddingX(padding);
        return this;
    }

    public Box paddingY(int padding) {
        style.paddingY(padding);
        return this;
    }

    public Box paddingLeft(int padding) {
        style.paddingLeft(padding);
        return this;
    }

    public Box paddingRight(int padding) {
        style.paddingRight(padding);
        return this;
    }

    public Box paddingTop(int padding) {
        style.paddingTop(padding);
        return this;
    }

    public Box paddingBottom(int padding) {
        style.paddingBottom(padding);
        return this;
    }

    // === 外边距 ===

    public Box margin(int margin) {
        style.margin(margin);
        return this;
    }

    public Box marginX(int margin) {
        style.marginX(margin);
        return this;
    }

    public Box marginY(int margin) {
        style.marginY(margin);
        return this;
    }

    public Box marginLeft(int margin) {
        style.marginLeft(margin);
        return this;
    }

    public Box marginRight(int margin) {
        style.marginRight(margin);
        return this;
    }

    public Box marginTop(int margin) {
        style.marginTop(margin);
        return this;
    }

    public Box marginBottom(int margin) {
        style.marginBottom(margin);
        return this;
    }

    // === 边框 ===

    public Box borderStyle(BorderStyle borderStyle) {
        style.borderStyle(borderStyle);
        return this;
    }

    public Box borderColor(Color borderColor) {
        style.borderColor(borderColor);
        return this;
    }

    // === 间距 ===

    public Box gap(int gap) {
        style.gap(gap);
        return this;
    }

    public Box columnGap(int gap) {
        style.columnGap(gap);
        return this;
    }

    public Box rowGap(int gap) {
        style.rowGap(gap);
        return this;
    }

    // === 显示与溢出 ===

    public Box display(Display display) {
        style.display(display);
        return this;
    }

    public Box overflow(Overflow overflow) {
        style.overflow(overflow);
        return this;
    }

    // === 背景色 ===

    public Box backgroundColor(Color color) {
        style.backgroundColor(color);
        return this;
    }

    @Override
    public ElementNode toNode() {
        ElementNode node = ElementNode.createBox();
        node.setStyle(style.build());
        for (Renderable child : children) {
            node.appendChild(child.toNode());
        }
        return node;
    }
}
