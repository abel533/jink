package io.mybatis.jink.layout;

import io.mybatis.jink.ansi.AnsiStringUtils;
import io.mybatis.jink.dom.ElementNode;
import io.mybatis.jink.dom.Node;
import io.mybatis.jink.dom.NodeType;
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
     * availableWidth/Height: 可用空间（AUTO fallback 时使用）
     * 百分比以 availableWidth/Height 为基准解析。
     */
    private static void layoutNode(ElementNode node, int availableWidth, int availableHeight) {
        layoutNode(node, availableWidth, availableHeight, availableWidth, availableHeight);
    }

    /**
     * 带百分比参考尺寸的布局计算。
     * percentRefWidth/Height: 百分比解析的参考容器尺寸（通常是父节点的 content 区域大小）。
     */
    private static void layoutNode(ElementNode node, int availableWidth, int availableHeight,
                                   int percentRefWidth, int percentRefHeight) {
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

        // 确定节点自身的宽高（百分比使用 percentRef 解析）
        int nodeWidth = resolveSize(style.width(), percentRefWidth);
        if (nodeWidth == Style.AUTO) {
            nodeWidth = availableWidth;
        }
        nodeWidth = clampSize(nodeWidth, style.minWidth(), style.maxWidth(), percentRefWidth);
        node.setComputedWidth(nodeWidth);

        int contentWidth = nodeWidth - innerBoxOffset;
        if (contentWidth < 0) contentWidth = 0;

        // 收集可见子节点，分离 flex 布局和绝对定位子节点
        List<ElementNode> children = new ArrayList<>();
        List<ElementNode> absoluteChildren = new ArrayList<>();
        for (Node child : node.getChildNodes()) {
            if (child instanceof ElementNode elem) {
                if (elem.getStyle().display() != Display.NONE) {
                    if (elem.getStyle().position() == Position.ABSOLUTE) {
                        absoluteChildren.add(elem);
                    } else {
                        children.add(elem);
                    }
                }
            }
            // TextNode 不直接参与布局，由父 Text 容器测量
        }

        // 文本节点：计算文本内容的高度
        if (node.getNodeType() == io.mybatis.jink.dom.NodeType.INK_TEXT) {
            int textHeight = measureTextHeight(node, contentWidth);
            int nodeHeight = resolveSize(style.height(), percentRefHeight);
            if (nodeHeight == Style.AUTO) {
                if (style.aspectRatio() > 0) {
                    nodeHeight = Math.max(1, (int) Math.round((double) nodeWidth / style.aspectRatio()));
                } else {
                    nodeHeight = textHeight + innerBoxOffsetV;
                }
            }
            nodeHeight = clampSize(nodeHeight, style.minHeight(), style.maxHeight(), percentRefHeight);
            node.setComputedHeight(nodeHeight);
            return;
        }

        if (children.isEmpty() && absoluteChildren.isEmpty()) {
            // 没有子节点，高度取显式值或 aspectRatio 推算或 innerBoxOffsetV
            int nodeHeight = resolveSize(style.height(), percentRefHeight);
            if (nodeHeight == Style.AUTO) {
                if (style.aspectRatio() > 0) {
                    nodeHeight = Math.max(1, (int) Math.round((double) nodeWidth / style.aspectRatio()));
                } else {
                    nodeHeight = innerBoxOffsetV;
                }
            }
            nodeHeight = clampSize(nodeHeight, style.minHeight(), style.maxHeight(), percentRefHeight);
            node.setComputedHeight(nodeHeight);
            return;
        }

        boolean isColumn = style.flexDirection() == FlexDirection.COLUMN
                || style.flexDirection() == FlexDirection.COLUMN_REVERSE;

        int gap = resolveGap(style, isColumn);

        // 计算内容区可用高度：优先使用节点自身 height，其次 aspectRatio，否则用外部 availableHeight
        int contentHeight = Style.AUTO;
        int resolvedHeight = resolveSize(style.height(), percentRefHeight);
        if (resolvedHeight != Style.AUTO) {
            contentHeight = resolvedHeight - innerBoxOffsetV;
            if (contentHeight < 0) contentHeight = 0;
        } else if (style.aspectRatio() > 0) {
            int aspectDerived = Math.max(1, (int) Math.round((double) nodeWidth / style.aspectRatio()));
            contentHeight = aspectDerived - innerBoxOffsetV;
            if (contentHeight < 0) contentHeight = 0;
        } else if (availableHeight != Style.AUTO) {
            contentHeight = availableHeight - innerBoxOffsetV;
            if (contentHeight < 0) contentHeight = 0;
        }

        // Flex 布局（仅非绝对定位子节点参与）
        if (!children.isEmpty()) {
            Style.FlexWrap wrap = style.flexWrap();
            boolean isWrapped = wrap == Style.FlexWrap.WRAP || wrap == Style.FlexWrap.WRAP_REVERSE;

            if (isColumn) {
                if (isWrapped) {
                    layoutColumnWrapped(node, children, contentWidth, contentHeight, gap, wrap);
                } else {
                    layoutColumn(node, children, contentWidth, contentHeight, gap);
                }
            } else {
                if (isWrapped) {
                    layoutRowWrapped(node, children, contentWidth, contentHeight, gap, wrap);
                } else {
                    layoutRow(node, children, contentWidth, contentHeight, gap);
                }
            }
        }

        // 计算节点自身高度
        int childrenHeight = 0;
        if (!children.isEmpty()) {
            Style.FlexWrap wrap = style.flexWrap();
            boolean isWrapped = wrap == Style.FlexWrap.WRAP || wrap == Style.FlexWrap.WRAP_REVERSE;

            if (isWrapped) {
                // 换行布局：取所有子节点的最大 (top + height + marginBottom) 作为总高度
                childrenHeight = computeWrappedExtent(children, isColumn);
            } else {
                childrenHeight = computeChildrenExtent(children, true, gap);
                if (!isColumn) {
                    childrenHeight = computeChildrenExtent(children, false, 0);
                }
            }
        }

        int nodeHeight = resolveSize(style.height(), percentRefHeight);
        if (nodeHeight == Style.AUTO) {
            if (style.aspectRatio() > 0) {
                nodeHeight = Math.max(1, (int) Math.round((double) nodeWidth / style.aspectRatio()));
            } else {
                nodeHeight = childrenHeight + innerBoxOffsetV;
            }
        }
        nodeHeight = clampSize(nodeHeight, style.minHeight(), style.maxHeight(), percentRefHeight);
        node.setComputedHeight(nodeHeight);

        // 定位子节点（加上 border + padding 偏移）
        int borderTop = style.hasBorder() ? 1 : 0;
        int borderLeft = style.hasBorder() ? 1 : 0;
        for (ElementNode child : children) {
            child.setComputedLeft(child.getComputedLeft() + borderLeft + style.paddingLeft());
            child.setComputedTop(child.getComputedTop() + borderTop + style.paddingTop());
        }

        // 绝对定位子节点：相对于父节点 content 区域定位
        if (!absoluteChildren.isEmpty()) {
            int absContentW = nodeWidth - innerBoxOffset;
            int absContentH = nodeHeight - innerBoxOffsetV;
            if (absContentW < 0) absContentW = 0;
            if (absContentH < 0) absContentH = 0;

            for (ElementNode absChild : absoluteChildren) {
                // 先布局绝对定位子节点自身（确定其宽高）
                layoutNode(absChild, absContentW, absContentH, absContentW, absContentH);

                Style acs = absChild.getStyle();
                int childW = absChild.getComputedWidth();
                int childH = absChild.getComputedHeight();

                // 水平定位
                int left;
                if (acs.posLeft() != Style.AUTO) {
                    left = acs.posLeft();
                } else if (acs.posRight() != Style.AUTO) {
                    left = absContentW - acs.posRight() - childW;
                } else {
                    left = 0;
                }

                // 垂直定位
                int top;
                if (acs.posTop() != Style.AUTO) {
                    top = acs.posTop();
                } else if (acs.posBottom() != Style.AUTO) {
                    top = absContentH - acs.posBottom() - childH;
                } else {
                    top = 0;
                }

                absChild.setComputedLeft(left + borderLeft + style.paddingLeft());
                absChild.setComputedTop(top + borderTop + style.paddingTop());
            }
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

            // 先递归布局子节点（百分比以父容器 content 区域为基准）
            layoutNode(child, contentWidth - childMarginH, Style.AUTO,
                    contentWidth, availableHeight);

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
                // 使用固有宽度（基于内容测量）
                int intrinsicWidth = measureIntrinsicWidth(child);
                int maxChildWidth = contentWidth - childMarginH;
                if (intrinsicWidth > 0 || cs.flexGrow() > 0) {
                    childWidth = Math.min(intrinsicWidth, maxChildWidth);
                } else {
                    childWidth = maxChildWidth;
                }
                child.setComputedWidth(childWidth);
                layoutNode(child, childWidth, availableHeight,
                        contentWidth, availableHeight);
            } else {
                child.setComputedWidth(childWidth);
                layoutNode(child, childWidth, availableHeight,
                        contentWidth, availableHeight);
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
                    layoutNode(child, child.getComputedWidth(), availableHeight,
                            contentWidth, availableHeight);
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
                    layoutNode(child, newWidth, availableHeight,
                            contentWidth, availableHeight);
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

    // ===== FlexWrap 多行布局 =====

    /**
     * Row 方向的 wrap 布局：子节点溢出时换行
     */
    private static void layoutRowWrapped(ElementNode parent, List<ElementNode> children,
                                         int contentWidth, int availableHeight, int gap,
                                         Style.FlexWrap wrapMode) {
        // Phase 1: 测量所有子节点的自然宽度
        for (ElementNode child : children) {
            Style cs = child.getStyle();
            int childWidth = resolveSize(cs.width(), contentWidth);
            int childMarginH = cs.horizontalMargin();

            if (childWidth == Style.AUTO) {
                int intrinsicWidth = measureIntrinsicWidth(child);
                int maxChildWidth = contentWidth - childMarginH;
                childWidth = (intrinsicWidth > 0 || cs.flexGrow() > 0)
                        ? Math.min(intrinsicWidth, maxChildWidth)
                        : maxChildWidth;
            }
            child.setComputedWidth(childWidth);
            layoutNode(child, childWidth, availableHeight, contentWidth, availableHeight);
        }

        // Phase 2: 按主轴宽度分行
        List<List<ElementNode>> lines = splitIntoLines(children, contentWidth, gap, false);

        // Phase 3: 每行独立做 flexGrow + justifyContent
        for (List<ElementNode> line : lines) {
            applyFlexGrowShrinkRow(line, contentWidth, gap, availableHeight);
            int usedWidth = computeLineMainSize(line, gap, false);
            positionOnMainAxis(line, parent.getStyle().justifyContent(), contentWidth, usedWidth, gap, false);
        }

        // Phase 4: 行间交叉轴堆叠 + alignItems per line
        stackLinesOnCrossAxis(lines, parent.getStyle().alignItems(), parent.getStyle().alignContent(),
                availableHeight, gap, true, wrapMode == Style.FlexWrap.WRAP_REVERSE);
    }

    /**
     * Column 方向的 wrap 布局：子节点溢出时换列
     */
    private static void layoutColumnWrapped(ElementNode parent, List<ElementNode> children,
                                            int contentWidth, int availableHeight, int gap,
                                            Style.FlexWrap wrapMode) {
        // Phase 1: 测量所有子节点的自然高度
        for (ElementNode child : children) {
            Style cs = child.getStyle();
            int childMarginH = cs.horizontalMargin();
            layoutNode(child, contentWidth - childMarginH, Style.AUTO, contentWidth, availableHeight);
        }

        // Phase 2: 按主轴高度分列
        List<List<ElementNode>> lines = splitIntoLines(children, availableHeight, gap, true);

        // Phase 3: 每列独立做 flexGrow + justifyContent
        for (List<ElementNode> line : lines) {
            applyFlexGrowShrinkColumn(line, availableHeight, gap);
            int usedHeight = computeLineMainSize(line, gap, true);
            positionOnMainAxis(line, parent.getStyle().justifyContent(),
                    availableHeight != Style.AUTO ? availableHeight : usedHeight, usedHeight, gap, true);
        }

        // Phase 4: 列间交叉轴堆叠
        stackLinesOnCrossAxis(lines, parent.getStyle().alignItems(), parent.getStyle().alignContent(),
                contentWidth, gap, false, wrapMode == Style.FlexWrap.WRAP_REVERSE);
    }

    /**
     * 将子节点按主轴尺寸分行/分列
     */
    private static List<List<ElementNode>> splitIntoLines(List<ElementNode> children,
                                                          int mainAxisSize, int gap,
                                                          boolean isColumn) {
        List<List<ElementNode>> lines = new ArrayList<>();
        List<ElementNode> currentLine = new ArrayList<>();
        int currentSize = 0;

        for (ElementNode child : children) {
            Style cs = child.getStyle();
            int childMainSize = isColumn
                    ? child.getComputedHeight() + cs.verticalMargin()
                    : child.getComputedWidth() + cs.horizontalMargin();
            int gapAdd = currentLine.isEmpty() ? 0 : gap;

            if (!currentLine.isEmpty() && mainAxisSize != Style.AUTO
                    && currentSize + gapAdd + childMainSize > mainAxisSize) {
                lines.add(currentLine);
                currentLine = new ArrayList<>();
                currentSize = 0;
                gapAdd = 0;
            }

            currentLine.add(child);
            currentSize += gapAdd + childMainSize;
        }
        if (!currentLine.isEmpty()) {
            lines.add(currentLine);
        }
        return lines;
    }

    /**
     * 对一行 row 子节点应用 flexGrow/flexShrink
     */
    private static void applyFlexGrowShrinkRow(List<ElementNode> line, int contentWidth,
                                               int gap, int availableHeight) {
        int totalWidth = computeLineMainSize(line, gap, false);
        int totalGrow = 0, totalShrink = 0;
        for (ElementNode c : line) {
            totalGrow += c.getStyle().flexGrow();
            totalShrink += c.getStyle().flexShrink();
        }

        int extra = Math.max(0, contentWidth - totalWidth);
        if (totalGrow > 0 && extra > 0) {
            for (ElementNode c : line) {
                int grow = c.getStyle().flexGrow();
                if (grow > 0) {
                    int bonus = extra * grow / totalGrow;
                    c.setComputedWidth(c.getComputedWidth() + bonus);
                    layoutNode(c, c.getComputedWidth(), availableHeight, contentWidth, availableHeight);
                }
            }
        }

        int overflow = Math.max(0, totalWidth - contentWidth);
        if (totalShrink > 0 && overflow > 0) {
            for (ElementNode c : line) {
                int shrink = c.getStyle().flexShrink();
                if (shrink > 0) {
                    int reduction = overflow * shrink / totalShrink;
                    int newW = Math.max(0, c.getComputedWidth() - reduction);
                    c.setComputedWidth(newW);
                    layoutNode(c, newW, availableHeight, contentWidth, availableHeight);
                }
            }
        }
    }

    /**
     * 对一列 column 子节点应用 flexGrow
     */
    private static void applyFlexGrowShrinkColumn(List<ElementNode> line, int availableHeight, int gap) {
        if (availableHeight == Style.AUTO) return;
        int totalHeight = computeLineMainSize(line, gap, true);
        int totalGrow = 0;
        for (ElementNode c : line) totalGrow += c.getStyle().flexGrow();

        int extra = Math.max(0, availableHeight - totalHeight);
        if (totalGrow > 0 && extra > 0) {
            for (ElementNode c : line) {
                int grow = c.getStyle().flexGrow();
                if (grow > 0) {
                    c.setComputedHeight(c.getComputedHeight() + extra * grow / totalGrow);
                }
            }
        }
    }

    /**
     * 计算一行/一列的主轴总尺寸
     */
    private static int computeLineMainSize(List<ElementNode> line, int gap, boolean isColumn) {
        int total = 0;
        for (int i = 0; i < line.size(); i++) {
            ElementNode c = line.get(i);
            total += isColumn
                    ? c.getComputedHeight() + c.getStyle().verticalMargin()
                    : c.getComputedWidth() + c.getStyle().horizontalMargin();
            if (i < line.size() - 1) total += gap;
        }
        return total;
    }

    /**
     * 在交叉轴上堆叠多行/多列，并对每行应用 alignItems
     *
     * @param crossAxisIsVertical true=ROW布局(行堆叠垂直), false=COLUMN布局(列堆叠水平)
     * @param reverse true=wrap-reverse
     */
    private static void stackLinesOnCrossAxis(List<List<ElementNode>> lines, AlignItems alignItems,
                                              AlignContent alignContent,
                                              int crossSize, int gap,
                                              boolean crossAxisIsVertical, boolean reverse) {
        // 计算每行的交叉轴尺寸
        int[] lineSizes = new int[lines.size()];
        int totalLineSize = 0;
        for (int i = 0; i < lines.size(); i++) {
            int maxCross = 0;
            for (ElementNode c : lines.get(i)) {
                int cs = crossAxisIsVertical
                        ? c.getComputedHeight() + c.getStyle().verticalMargin()
                        : c.getComputedWidth() + c.getStyle().horizontalMargin();
                maxCross = Math.max(maxCross, cs);
            }
            lineSizes[i] = maxCross;
            totalLineSize += maxCross;
        }

        int lineCount = lines.size();
        int freeSpace = (crossSize != Style.AUTO) ? Math.max(0, crossSize - totalLineSize) : 0;

        // alignContent STRETCH: 将多余空间均分给每行
        if (alignContent == AlignContent.STRETCH && freeSpace > 0 && lineCount > 0) {
            int extra = freeSpace / lineCount;
            int remainder = freeSpace % lineCount;
            for (int i = 0; i < lineCount; i++) {
                lineSizes[i] += extra + (i < remainder ? 1 : 0);
            }
            freeSpace = 0;
        }

        // 计算起始偏移和行间距
        int startOffset = 0;
        int spaceBetweenLines = 0;

        switch (alignContent) {
            case FLEX_START, STRETCH -> startOffset = 0;
            case FLEX_END -> startOffset = freeSpace;
            case CENTER -> startOffset = freeSpace / 2;
            case SPACE_BETWEEN -> {
                if (lineCount > 1) spaceBetweenLines = freeSpace / (lineCount - 1);
            }
            case SPACE_AROUND -> {
                if (lineCount > 0) {
                    int eachSide = freeSpace / (lineCount * 2);
                    startOffset = eachSide;
                    spaceBetweenLines = eachSide * 2;
                }
            }
            case SPACE_EVENLY -> {
                if (lineCount > 0) {
                    int each = freeSpace / (lineCount + 1);
                    startOffset = each;
                    spaceBetweenLines = each;
                }
            }
        }

        // 堆叠行
        int pos = startOffset;
        for (int lineIdx = 0; lineIdx < lineCount; lineIdx++) {
            int actualIdx = reverse ? (lineCount - 1 - lineIdx) : lineIdx;
            List<ElementNode> line = lines.get(actualIdx);
            int lineSize = lineSizes[actualIdx];

            // 计算该行的最大基线（用于 BASELINE 对齐）
            int lineMaxBaseline = 0;
            boolean lineHasBaseline = false;
            if (crossAxisIsVertical) {
                for (ElementNode child : line) {
                    AlignItems itemAlign = child.getStyle().alignSelf() != null
                            ? child.getStyle().alignSelf() : alignItems;
                    if (itemAlign == AlignItems.BASELINE) {
                        lineHasBaseline = true;
                        lineMaxBaseline = Math.max(lineMaxBaseline, computeBaseline(child));
                    }
                }
            }

            // 对该行中的每个子节点做交叉轴对齐
            for (ElementNode child : line) {
                Style cs = child.getStyle();
                int childCross = crossAxisIsVertical
                        ? child.getComputedHeight() + cs.verticalMargin()
                        : child.getComputedWidth() + cs.horizontalMargin();
                int lineFreeSpace = Math.max(0, lineSize - childCross);

                AlignItems itemAlign = cs.alignSelf() != null ? cs.alignSelf() : alignItems;
                int crossOffset = switch (itemAlign) {
                    case FLEX_START -> 0;
                    case CENTER -> lineFreeSpace / 2;
                    case FLEX_END -> lineFreeSpace;
                    case STRETCH -> 0;
                    case BASELINE -> {
                        if (lineHasBaseline && crossAxisIsVertical) {
                            yield lineMaxBaseline - computeBaseline(child);
                        }
                        yield 0;
                    }
                };

                if (crossAxisIsVertical) {
                    child.setComputedTop(pos + crossOffset + cs.marginTop());
                    if (itemAlign == AlignItems.STRETCH && lineFreeSpace > 0) {
                        child.setComputedHeight(lineSize - cs.verticalMargin());
                    }
                } else {
                    child.setComputedLeft(pos + crossOffset + cs.marginLeft());
                    if (itemAlign == AlignItems.STRETCH && lineFreeSpace > 0) {
                        child.setComputedWidth(lineSize - cs.horizontalMargin());
                    }
                }
            }

            pos += lineSize + spaceBetweenLines;
        }
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
        // Baseline 对齐：先收集所有需要 baseline 对齐的子节点的基线，计算最大基线
        int maxBaseline = 0;
        boolean hasBaseline = false;
        if (crossAxisIsVertical && (align == AlignItems.BASELINE || children.stream()
                .anyMatch(c -> c.getStyle().alignSelf() == AlignItems.BASELINE))) {
            for (ElementNode child : children) {
                AlignItems selfAlign = child.getStyle().alignSelf() != null
                        ? child.getStyle().alignSelf() : align;
                if (selfAlign == AlignItems.BASELINE) {
                    hasBaseline = true;
                    maxBaseline = Math.max(maxBaseline, computeBaseline(child));
                }
            }
        }

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
                case BASELINE -> {
                    if (hasBaseline && crossAxisIsVertical) {
                        // 对齐基线：child.top = maxBaseline - childBaseline + marginTop
                        int childBaseline = computeBaseline(child);
                        yield marginBefore + (maxBaseline - childBaseline);
                    }
                    yield marginBefore;
                }
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
        if (Style.isPercent(size)) {
            // 百分比：相对于容器尺寸计算
            if (containerSize == Style.AUTO) return Style.AUTO;
            return containerSize * Style.getPercent(size) / 100;
        }
        return size;
    }

    private static int clampSize(int size, int min, int max) {
        int resolvedMin = min;
        int resolvedMax = max;
        if (Style.isPercent(resolvedMin)) resolvedMin = Style.AUTO; // 百分比 min/max 需外部 resolve
        if (Style.isPercent(resolvedMax)) resolvedMax = Style.AUTO;
        if (resolvedMin != Style.AUTO && size < resolvedMin) size = resolvedMin;
        if (resolvedMax != Style.AUTO && size > resolvedMax) size = resolvedMax;
        return size;
    }

    /**
     * 带容器尺寸的 clampSize，支持百分比 min/max
     */
    private static int clampSize(int size, int min, int max, int containerSize) {
        int resolvedMin = resolveSize(min, containerSize);
        int resolvedMax = resolveSize(max, containerSize);
        if (resolvedMin != Style.AUTO && size < resolvedMin) size = resolvedMin;
        if (resolvedMax != Style.AUTO && size > resolvedMax) size = resolvedMax;
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
     * 换行布局下计算所有子节点占据的交叉轴总尺寸。
     * 子节点已被 stackLinesOnCrossAxis 定位，取最大 (top+height) 或 (left+width)。
     */
    private static int computeWrappedExtent(List<ElementNode> children, boolean isColumn) {
        int maxExtent = 0;
        for (ElementNode child : children) {
            Style cs = child.getStyle();
            int extent;
            if (isColumn) {
                // column wrap: 交叉轴是水平方向
                extent = child.getComputedLeft() + child.getComputedWidth() + cs.marginRight();
            } else {
                // row wrap: 交叉轴是垂直方向
                extent = child.getComputedTop() + child.getComputedHeight() + cs.marginBottom();
            }
            maxExtent = Math.max(maxExtent, extent);
        }
        return maxExtent;
    }

    /**
     * 测量文本节点的高度（根据可用宽度和 textWrap 模式计算行数）
     */
    static int measureTextHeight(ElementNode textNode, int maxWidth) {
        String text = squashTextContent(textNode);
        if (text.isEmpty()) return 0;
        if (maxWidth <= 0) return 1;

        TextWrap textWrap = textNode.getStyle().textWrap();
        boolean isTruncate = textWrap != null && textWrap != TextWrap.WRAP;

        int lines = 0;
        for (String line : text.split("\n", -1)) {
            if (isTruncate) {
                // 截断模式：每个逻辑行固定占 1 行
                lines++;
            } else {
                int lineWidth = AnsiStringUtils.visibleWidth(line);
                if (lineWidth == 0) {
                    lines++;
                } else {
                    lines += Math.max(1, (int) Math.ceil((double) lineWidth / maxWidth));
                }
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

    /**
     * 测量节点的固有宽度（基于内容，不拉伸）。
     * 用于 Row 布局中确定 AUTO 宽度子节点的自然尺寸。
     */
    public static int measureIntrinsicWidth(ElementNode node) {
        Style style = node.getStyle();
        // 显式宽度直接返回
        if (style.width() != Style.AUTO) return style.width();

        int borderH = style.horizontalBorderWidth();
        int paddingH = style.horizontalPadding();

        // 文本节点：按内容最大行宽
        if (node.getNodeType() == io.mybatis.jink.dom.NodeType.INK_TEXT
                || node.getNodeType() == io.mybatis.jink.dom.NodeType.INK_VIRTUAL_TEXT) {
            String text = squashTextContent(node);
            int maxLineWidth = 0;
            for (String line : text.split("\n", -1)) {
                maxLineWidth = Math.max(maxLineWidth, AnsiStringUtils.visibleWidth(line));
            }
            return maxLineWidth + borderH + paddingH;
        }

        // Box 节点：根据子节点和布局方向计算
        boolean isColumn = style.flexDirection() == FlexDirection.COLUMN
                || style.flexDirection() == FlexDirection.COLUMN_REVERSE;
        int gap = resolveGap(style, isColumn);

        int total = 0;
        int maxChild = 0;
        int childCount = 0;
        for (Node child : node.getChildNodes()) {
            if (child instanceof ElementNode elem && elem.getStyle().display() != Display.NONE) {
                int childIntrinsic = measureIntrinsicWidth(elem) + elem.getStyle().horizontalMargin();
                if (isColumn) {
                    maxChild = Math.max(maxChild, childIntrinsic);
                } else {
                    total += childIntrinsic;
                    childCount++;
                }
            }
        }

        if (isColumn) {
            return maxChild + borderH + paddingH;
        } else {
            total += gap * Math.max(0, childCount - 1);
            return total + borderH + paddingH;
        }
    }

    /**
     * 计算节点的基线偏移量（从节点顶部到第一行文本基线的距离）。
     * 用于 alignItems: BASELINE 对齐。
     * - 文本节点：borderTop + paddingTop + 1（第一行文本的底部）
     * - Box 节点：递归查找第一个文本后代
     * - 无文本：返回节点高度作为 fallback
     */
    static int computeBaseline(ElementNode node) {
        Style style = node.getStyle();
        int borderTop = style.hasBorder() ? 1 : 0;
        int paddingTop = style.paddingTop();

        // 文本节点：基线 = 第一行文本底部
        if (node.getNodeType() == NodeType.INK_TEXT
                || node.getNodeType() == NodeType.INK_VIRTUAL_TEXT) {
            return borderTop + paddingTop + 1;
        }

        // Box 节点：查找第一个子 ElementNode 并递归
        for (Node child : node.getChildNodes()) {
            if (child instanceof ElementNode elem && elem.getStyle().display() != Display.NONE) {
                return borderTop + paddingTop + elem.getComputedTop() + computeBaseline(elem);
            }
        }

        // 无子节点：fallback 到节点高度
        return node.getComputedHeight();
    }
}
