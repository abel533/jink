package io.mybatis.jink.layout;

import io.mybatis.jink.ansi.AnsiStringUtils;
import io.mybatis.jink.dom.ElementNode;
import io.mybatis.jink.dom.Node;
import io.mybatis.jink.dom.TextNode;
import io.mybatis.jink.style.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 简化版 Flexbox 布局引擎，对应 ink 中 Yoga 的角色。
 * 支持：
 * - flexDirection: row / column
 * - justifyContent: flex-start / center / flex-end / space-between / space-around / space-evenly
 * - alignItems: flex-start / center / flex-end / stretch
 * - flexGrow / flexShrink
 * - width / height / minWidth / minHeight / maxWidth / maxHeight
 * - padding / margin / border
 * - gap
 * - display: none
 */
public class FlexLayout {

    /**
     * 从根节点开始计算整个树的布局。
     *
     * @param root          根元素节点
     * @param containerWidth 可用宽度（通常为终端宽度）
     */
    public static void calculateLayout(ElementNode root, int containerWidth) {
        root.setComputedLeft(0);
        root.setComputedTop(0);
        root.setComputedWidth(containerWidth);

        layoutNode(root, containerWidth, Style.AUTO);
    }

    /**
     * 对单个节点进行布局计算。
     */
    private static void layoutNode(ElementNode node, int availableWidth, int availableHeight) {
        Style style = node.getStyle();

        // display: none 不参与布局
        if (style.display() == Display.NONE) {
            node.setComputedWidth(0);
            node.setComputedHeight(0);
            return;
        }

        // 计算边框和内边距占用的空间
        int borderH = style.horizontalBorderWidth();
        int borderV = style.verticalBorderWidth();
        int paddingH = style.horizontalPadding();
        int paddingV = style.verticalPadding();
        int innerBoxOffset = borderH + paddingH;
        int innerBoxOffsetV = borderV + paddingV;

        // 确定节点自身的宽高
        int nodeWidth = resolveSize(style.width(), availableWidth);
        if (nodeWidth == Style.AUTO) {
            nodeWidth = availableWidth;
        }
        nodeWidth = clampSize(nodeWidth, style.minWidth(), style.maxWidth());
        node.setComputedWidth(nodeWidth);

        int contentWidth = nodeWidth - innerBoxOffset;
        if (contentWidth < 0) contentWidth = 0;

        // 收集可见子节点
        List<ElementNode> children = new ArrayList<>();
        for (Node child : node.getChildNodes()) {
            if (child instanceof ElementNode elem) {
                if (elem.getStyle().display() != Display.NONE) {
                    children.add(elem);
                }
            }
            // TextNode 不直接参与布局，由父 Text 容器测量
        }

        // 文本节点：计算文本内容的高度
        if (node.getNodeType() == io.mybatis.jink.dom.NodeType.INK_TEXT) {
            int textHeight = measureTextHeight(node, contentWidth);
            int nodeHeight = resolveSize(style.height(), availableHeight);
            if (nodeHeight == Style.AUTO) {
                nodeHeight = textHeight + innerBoxOffsetV;
            }
            nodeHeight = clampSize(nodeHeight, style.minHeight(), style.maxHeight());
            node.setComputedHeight(nodeHeight);
            return;
        }

        if (children.isEmpty()) {
            // 没有子节点，高度取显式值或 innerBoxOffsetV
            int nodeHeight = resolveSize(style.height(), availableHeight);
            if (nodeHeight == Style.AUTO) {
                nodeHeight = innerBoxOffsetV;
            }
            nodeHeight = clampSize(nodeHeight, style.minHeight(), style.maxHeight());
            node.setComputedHeight(nodeHeight);
            return;
        }

        boolean isColumn = style.flexDirection() == FlexDirection.COLUMN
                || style.flexDirection() == FlexDirection.COLUMN_REVERSE;

        int gap = resolveGap(style, isColumn);

        if (isColumn) {
            layoutColumn(node, children, contentWidth, availableHeight, gap);
        } else {
            layoutRow(node, children, contentWidth, availableHeight, gap);
        }

        // 计算节点自身高度
        int childrenHeight = computeChildrenExtent(children, true, gap);
        if (!isColumn) {
            childrenHeight = computeChildrenExtent(children, false, 0);
        }

        int nodeHeight = resolveSize(style.height(), availableHeight);
        if (nodeHeight == Style.AUTO) {
            nodeHeight = childrenHeight + innerBoxOffsetV;
        }
        nodeHeight = clampSize(nodeHeight, style.minHeight(), style.maxHeight());
        node.setComputedHeight(nodeHeight);

        // 定位子节点（加上 border + padding 偏移）
        int borderTop = style.hasBorder() ? 1 : 0;
        int borderLeft = style.hasBorder() ? 1 : 0;
        for (ElementNode child : children) {
            child.setComputedLeft(child.getComputedLeft() + borderLeft + style.paddingLeft());
            child.setComputedTop(child.getComputedTop() + borderTop + style.paddingTop());
        }
    }

    /**
     * Column 方向布局（主轴为垂直方向）
     */
    private static void layoutColumn(ElementNode parent, List<ElementNode> children,
                                     int contentWidth, int availableHeight, int gap) {
        // 第一遍：计算所有子节点的自然尺寸
        int totalFixedHeight = 0;
        int totalGrow = 0;

        for (ElementNode child : children) {
            Style cs = child.getStyle();
            int childMarginH = cs.horizontalMargin();

            // 先递归布局子节点（确定其宽高）
            layoutNode(child, contentWidth - childMarginH, Style.AUTO);

            totalFixedHeight += child.getComputedHeight() + cs.verticalMargin();
            totalGrow += cs.flexGrow();
        }

        totalFixedHeight += gap * Math.max(0, children.size() - 1);

        // 分配 flexGrow 额外空间
        int contentHeight = availableHeight != Style.AUTO ? availableHeight : totalFixedHeight;
        int extraSpace = Math.max(0, contentHeight - totalFixedHeight);

        if (totalGrow > 0 && extraSpace > 0) {
            for (ElementNode child : children) {
                int grow = child.getStyle().flexGrow();
                if (grow > 0) {
                    int bonus = extraSpace * grow / totalGrow;
                    child.setComputedHeight(child.getComputedHeight() + bonus);
                }
            }
        }

        // 主轴定位 (justifyContent)
        positionOnMainAxis(children, parent.getStyle().justifyContent(),
                contentHeight, totalFixedHeight + extraSpace, gap, true);

        // 交叉轴定位 (alignItems) - COLUMN 布局的交叉轴是水平方向
        positionOnCrossAxis(children, parent.getStyle().alignItems(), contentWidth, false);
    }

    /**
     * Row 方向布局（主轴为水平方向）
     */
    private static void layoutRow(ElementNode parent, List<ElementNode> children,
                                  int contentWidth, int availableHeight, int gap) {
        // 第一遍：计算所有子节点的自然尺寸
        int totalFixedWidth = 0;
        int totalGrow = 0;
        int totalShrink = 0;

        for (ElementNode child : children) {
            Style cs = child.getStyle();
            int childWidth = resolveSize(cs.width(), contentWidth);
            int childMarginH = cs.horizontalMargin();

            if (childWidth == Style.AUTO) {
                // 先递归布局，确定自然宽度
                layoutNode(child, contentWidth - childMarginH, availableHeight);
            } else {
                child.setComputedWidth(childWidth);
                layoutNode(child, childWidth, availableHeight);
            }

            totalFixedWidth += child.getComputedWidth() + childMarginH;
            totalGrow += cs.flexGrow();
            totalShrink += cs.flexShrink();
        }

        totalFixedWidth += gap * Math.max(0, children.size() - 1);

        // flexGrow 分配
        int extraSpace = Math.max(0, contentWidth - totalFixedWidth);
        if (totalGrow > 0 && extraSpace > 0) {
            for (ElementNode child : children) {
                int grow = child.getStyle().flexGrow();
                if (grow > 0) {
                    int bonus = extraSpace * grow / totalGrow;
                    child.setComputedWidth(child.getComputedWidth() + bonus);
                    // 重新布局以计算新高度
                    layoutNode(child, child.getComputedWidth(), availableHeight);
                }
            }
        }

        // flexShrink 收缩
        int overflow = Math.max(0, totalFixedWidth - contentWidth);
        if (totalShrink > 0 && overflow > 0) {
            for (ElementNode child : children) {
                int shrink = child.getStyle().flexShrink();
                if (shrink > 0) {
                    int reduction = overflow * shrink / totalShrink;
                    int newWidth = Math.max(0, child.getComputedWidth() - reduction);
                    child.setComputedWidth(newWidth);
                    layoutNode(child, newWidth, availableHeight);
                }
            }
        }

        // 主轴定位 (justifyContent)
        int usedWidth = 0;
        for (ElementNode child : children) {
            usedWidth += child.getComputedWidth() + child.getStyle().horizontalMargin();
        }
        usedWidth += gap * Math.max(0, children.size() - 1);

        positionOnMainAxis(children, parent.getStyle().justifyContent(),
                contentWidth, usedWidth, gap, false);

        // 交叉轴定位 (alignItems) - ROW 布局的交叉轴是垂直方向
        positionOnCrossAxis(children, parent.getStyle().alignItems(),
                availableHeight != Style.AUTO ? availableHeight : 0, true);
    }

    /**
     * 主轴定位（根据 justifyContent）
     */
    private static void positionOnMainAxis(List<ElementNode> children, JustifyContent justify,
                                           int containerSize, int usedSize, int gap, boolean isColumn) {
        int freeSpace = Math.max(0, containerSize - usedSize);
        int offset = 0;
        int spaceBetween = 0;

        switch (justify) {
            case FLEX_START -> offset = 0;
            case FLEX_END -> offset = freeSpace;
            case CENTER -> offset = freeSpace / 2;
            case SPACE_BETWEEN -> {
                if (children.size() > 1) {
                    spaceBetween = freeSpace / (children.size() - 1);
                }
            }
            case SPACE_AROUND -> {
                if (!children.isEmpty()) {
                    int space = freeSpace / children.size();
                    offset = space / 2;
                    spaceBetween = space;
                }
            }
            case SPACE_EVENLY -> {
                if (!children.isEmpty()) {
                    int space = freeSpace / (children.size() + 1);
                    offset = space;
                    spaceBetween = space;
                }
            }
        }

        int pos = offset;
        for (int i = 0; i < children.size(); i++) {
            ElementNode child = children.get(i);
            Style cs = child.getStyle();

            if (isColumn) {
                child.setComputedTop(pos + cs.marginTop());
                pos += child.getComputedHeight() + cs.verticalMargin();
            } else {
                child.setComputedLeft(pos + cs.marginLeft());
                pos += child.getComputedWidth() + cs.horizontalMargin();
            }

            if (i < children.size() - 1) {
                pos += gap + spaceBetween;
            }
        }
    }

    /**
     * 交叉轴定位（根据 alignItems）
     *
     * @param crossAxisIsVertical true=交叉轴为垂直方向(ROW布局), false=交叉轴为水平方向(COLUMN布局)
     */
    private static void positionOnCrossAxis(List<ElementNode> children, AlignItems align,
                                            int crossSize, boolean crossAxisIsVertical) {
        for (ElementNode child : children) {
            Style cs = child.getStyle();
            AlignItems selfAlign = cs.alignSelf() != null ? cs.alignSelf() : align;

            int childCrossSize;
            int childMargin;
            int marginBefore;

            if (crossAxisIsVertical) {
                childCrossSize = child.getComputedHeight();
                childMargin = cs.verticalMargin();
                marginBefore = cs.marginTop();
            } else {
                childCrossSize = child.getComputedWidth();
                childMargin = cs.horizontalMargin();
                marginBefore = cs.marginLeft();
            }

            int availCross = crossSize > 0 ? crossSize : childCrossSize;
            int freeSpace = Math.max(0, availCross - childCrossSize - childMargin);

            int crossPos = switch (selfAlign) {
                case FLEX_START -> marginBefore;
                case CENTER -> marginBefore + freeSpace / 2;
                case FLEX_END -> marginBefore + freeSpace;
                case STRETCH -> {
                    if (crossSize > 0) {
                        if (crossAxisIsVertical) {
                            child.setComputedHeight(crossSize - childMargin);
                        } else {
                            child.setComputedWidth(crossSize - childMargin);
                        }
                    }
                    yield marginBefore;
                }
                case BASELINE -> marginBefore;
            };

            if (crossAxisIsVertical) {
                child.setComputedTop(crossPos);
            } else {
                child.setComputedLeft(crossPos);
            }
        }
    }

    // ===== 辅助方法 =====

    private static int resolveSize(int size, int containerSize) {
        if (size == Style.AUTO) return Style.AUTO;
        return size;
    }

    private static int clampSize(int size, int min, int max) {
        if (min != Style.AUTO && size < min) size = min;
        if (max != Style.AUTO && size > max) size = max;
        return size;
    }

    private static int resolveGap(Style style, boolean isColumn) {
        if (isColumn) {
            return style.rowGap() != Style.AUTO ? style.rowGap() : style.gap();
        } else {
            return style.columnGap() != Style.AUTO ? style.columnGap() : style.gap();
        }
    }

    /**
     * 计算所有子节点在指定方向上的总尺寸
     */
    private static int computeChildrenExtent(List<ElementNode> children, boolean vertical, int gap) {
        if (vertical) {
            int total = 0;
            for (int i = 0; i < children.size(); i++) {
                ElementNode child = children.get(i);
                total += child.getComputedHeight() + child.getStyle().verticalMargin();
                if (i < children.size() - 1) total += gap;
            }
            return total;
        } else {
            int max = 0;
            for (ElementNode child : children) {
                int h = child.getComputedHeight() + child.getStyle().verticalMargin();
                max = Math.max(max, h);
            }
            return max;
        }
    }

    /**
     * 测量文本节点的高度（根据可用宽度计算换行后的行数）
     */
    static int measureTextHeight(ElementNode textNode, int maxWidth) {
        String text = squashTextContent(textNode);
        if (text.isEmpty()) return 0;
        if (maxWidth <= 0) return 1;

        int lines = 0;
        for (String line : text.split("\n", -1)) {
            int lineWidth = AnsiStringUtils.visibleWidth(line);
            if (lineWidth == 0) {
                lines++;
            } else {
                lines += Math.max(1, (int) Math.ceil((double) lineWidth / maxWidth));
            }
        }
        return lines;
    }

    /**
     * 递归收集文本节点的所有文本内容（对应 ink 的 squashTextNodes）
     */
    public static String squashTextContent(Node node) {
        if (node instanceof TextNode textNode) {
            return textNode.getNodeValue();
        }
        if (node instanceof ElementNode elem) {
            StringBuilder sb = new StringBuilder();
            for (Node child : elem.getChildNodes()) {
                sb.append(squashTextContent(child));
            }
            return sb.toString();
        }
        return "";
    }
}
