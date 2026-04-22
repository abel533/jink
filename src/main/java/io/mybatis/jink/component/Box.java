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

    public Box alignSelf(AlignItems alignSelf) {
        style.alignSelf(alignSelf);
        return this;
    }

    public Box alignContent(AlignContent alignContent) {
        style.alignContent(alignContent);
        return this;
    }

    public Box flexWrap(Style.FlexWrap flexWrap) {
        style.flexWrap(flexWrap);
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

    public Box maxWidth(int maxWidth) {
        style.maxWidth(maxWidth);
        return this;
    }

    public Box maxHeight(int maxHeight) {
        style.maxHeight(maxHeight);
        return this;
    }

    /**
     * 设置宽高比约束（width / height）。
     * 当 height 为 AUTO 时，height = width / ratio。
     * 例如：aspectRatio(2.0f) 表示宽度是高度的两倍。
     */
    public Box aspectRatio(float ratio) {
        style.aspectRatio(ratio);
        return this;
    }

    public Box widthPercent(int pct) {
        style.widthPercent(pct);
        return this;
    }

    public Box heightPercent(int pct) {
        style.heightPercent(pct);
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

    public Box flexBasisPercent(int pct) {
        style.flexBasisPercent(pct);
        return this;
    }

    // === 定位 ===

    public Box position(Position position) {
        style.position(position);
        return this;
    }

    public Box posTop(int v) {
        style.posTop(v);
        return this;
    }

    public Box posRight(int v) {
        style.posRight(v);
        return this;
    }

    public Box posBottom(int v) {
        style.posBottom(v);
        return this;
    }

    public Box posLeft(int v) {
        style.posLeft(v);
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

    public Box borderTopColor(Color color) {
        style.borderTopColor(color);
        return this;
    }

    public Box borderRightColor(Color color) {
        style.borderRightColor(color);
        return this;
    }

    public Box borderBottomColor(Color color) {
        style.borderBottomColor(color);
        return this;
    }

    public Box borderLeftColor(Color color) {
        style.borderLeftColor(color);
        return this;
    }

    public Box borderDimColor() {
        style.borderDimColor(true);
        return this;
    }

    public Box borderTopDimColor() {
        style.borderTopDimColor(true);
        return this;
    }

    public Box borderRightDimColor() {
        style.borderRightDimColor(true);
        return this;
    }

    public Box borderBottomDimColor() {
        style.borderBottomDimColor(true);
        return this;
    }

    public Box borderLeftDimColor() {
        style.borderLeftDimColor(true);
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

    public Box overflowX(Overflow overflow) {
        style.overflowX(overflow);
        return this;
    }

    public Box overflowY(Overflow overflow) {
        style.overflowY(overflow);
        return this;
    }

    // === 文本换行 ===

    public Box textWrap(TextWrap textWrap) {
        style.textWrap(textWrap);
        return this;
    }

    // === 文本颜色（用于容器默认文字色） ===

    public Box color(Color color) {
        style.color(color);
        return this;
    }

    // === 背景色 ===

    public Box backgroundColor(Color color) {
        style.backgroundColor(color);
        return this;
    }

    // === 滚动 ===

    /**
     * 启用垂直滚动，返回一个 Scroll 组件包装当前 Box。
     * Scroll 组件支持 ↑/↓ 方向键控制滚动，按 q 退出滚动模式。
     * 滚动条用 ▲▼ 符号在右侧指示当前位置。
     * <p>
     * 注意：Scroll 是一个 Component，需要作为根组件或在 Component 内部使用才能处理键盘事件。
     *
     * @param viewportHeight 视口高度（可见区域的行数）
     * @return Scroll 组件
     */
    public Scroll scroll(int viewportHeight) {
        return Scroll.of(this).viewportHeight(viewportHeight);
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
