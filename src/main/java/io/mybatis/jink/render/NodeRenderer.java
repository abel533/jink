package io.mybatis.jink.render;

import io.mybatis.jink.ansi.AnsiStringUtils;
import io.mybatis.jink.dom.ElementNode;
import io.mybatis.jink.dom.Node;
import io.mybatis.jink.dom.NodeType;
import io.mybatis.jink.dom.TextNode;
import io.mybatis.jink.layout.FlexLayout;
import io.mybatis.jink.style.BorderStyle;
import io.mybatis.jink.style.Color;
import io.mybatis.jink.style.Display;
import io.mybatis.jink.style.Style;
import io.mybatis.jink.style.TextWrap;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * 节点渲染器，对应 ink 的 render-node-to-output.ts。
 * 将已布局的 DOM 树通过 DFS 渲染到 VirtualScreen。
 */
public class NodeRenderer {

    /**
     * 渲染整个 DOM 树到虚拟屏幕
     */
    public static VirtualScreen render(ElementNode root) {
        int width = root.getComputedWidth();
        int height = root.getComputedHeight();
        if (width <= 0) width = 1;
        if (height <= 0) height = 1;

        VirtualScreen screen = new VirtualScreen(width, height);
        renderNode(root, screen, 0, 0);
        return screen;
    }

    /**
     * DFS 渲染单个节点及其子节点
     */
    private static void renderNode(ElementNode node, VirtualScreen screen, int offsetX, int offsetY) {
        Style style = node.getStyle();

        if (style.display() == Display.NONE) return;

        int x = offsetX + node.getComputedLeft();
        int y = offsetY + node.getComputedTop();
        int w = node.getComputedWidth();
        int h = node.getComputedHeight();

        // 渲染背景色
        if (style.backgroundColor() != null) {
            renderBackground(screen, x, y, w, h, style);
        }

        // 渲染边框
        if (style.hasBorder()) {
            renderBorder(screen, x, y, w, h, style);
        }

        // 渲染文本内容
        if (node.getNodeType() == NodeType.INK_TEXT) {
            renderText(node, screen, x, y, w, h, style);
            return; // 文本节点不递归渲染子节点
        }

        // Clipping (overflow: hidden)
        boolean clipped = isOverflowHidden(style);

        // 滚动支持：检查滚动属性
        Integer scrollOffset = (Integer) node.getAttribute("internal_scrollOffset");
        Integer viewportHeight = (Integer) node.getAttribute("internal_viewportHeight");
        Integer contentHeight = (Integer) node.getAttribute("internal_contentHeight");
        // hasScrollAttrs：只要设置了滚动视口属性就应该 clip（即使 contentHeight 还未测量）
        boolean hasScrollAttrs = scrollOffset != null && viewportHeight != null && viewportHeight > 0;
        // hasScroll：内容确实超出视口，需要应用滚动偏移和滚动条
        boolean hasScroll = hasScrollAttrs && contentHeight != null && contentHeight > viewportHeight;

        if (clipped || hasScrollAttrs) {
            int clipX = x + (style.hasBorder() ? 1 : 0);
            int clipY = y + (style.hasBorder() ? 1 : 0);
            int clipW = w - style.horizontalBorderWidth();
            int clipH = h - style.verticalBorderWidth();
            screen.pushClip(clipX, clipY, clipW, clipH);
        }

        // Transform 支持：渲染子节点到临时屏幕，再逐行变换后写回
        @SuppressWarnings("unchecked")
        BiFunction<String, Integer, String> transformFn =
                (BiFunction<String, Integer, String>) node.getAttribute("internal_transform");

        // 计算滚动偏移（如果有滚动）
        int scrollY = hasScroll ? -scrollOffset : 0;

        if (transformFn != null) {
            renderWithTransform(node, screen, x, y + scrollY, w, h, transformFn);
        } else {
            // 递归渲染子节点（应用滚动偏移）
            for (Node child : node.getChildNodes()) {
                if (child instanceof ElementNode) {
                    renderNode((ElementNode) child, screen, x, y + scrollY);
                }
            }
        }

        if (clipped || hasScrollAttrs) {
            screen.popClip();
        }

        // 渲染滚动条（如果有滚动且需要显示）
        if (hasScroll) {
            renderScrollbar(screen, x, y, w, h, style, scrollOffset, viewportHeight, contentHeight);
        }
    }

    /**
     * 渲染背景色
     */
    private static void renderBackground(VirtualScreen screen, int x, int y, int w, int h, Style style) {
        Style bgStyle = Style.builder().backgroundColor(style.backgroundColor()).build();
        screen.fill(x, y, w, h, ' ', bgStyle);
    }

    /**
     * 渲染边框
     */
    private static void renderBorder(VirtualScreen screen, int x, int y, int w, int h, Style style) {
        BorderStyle border = style.borderStyle();

        // 每边独立颜色样式（null = 无颜色，使用默认）
        Style topStyle = buildBorderSideStyle(style.effectiveBorderTopColor(), style.effectiveBorderTopDimmed());
        Style rightStyle = buildBorderSideStyle(style.effectiveBorderRightColor(), style.effectiveBorderRightDimmed());
        Style bottomStyle = buildBorderSideStyle(style.effectiveBorderBottomColor(), style.effectiveBorderBottomDimmed());
        Style leftStyle = buildBorderSideStyle(style.effectiveBorderLeftColor(), style.effectiveBorderLeftDimmed());

        // 四个角（角落使用相邻上/下边的颜色）
        screen.write(x, y, border.getTopLeft(), topStyle);
        screen.write(x + w - 1, y, border.getTopRight(), topStyle);
        screen.write(x, y + h - 1, border.getBottomLeft(), bottomStyle);
        screen.write(x + w - 1, y + h - 1, border.getBottomRight(), bottomStyle);

        // 上下边
        for (int col = x + 1; col < x + w - 1; col++) {
            screen.write(col, y, border.getTop(), topStyle);
            screen.write(col, y + h - 1, border.getBottom(), bottomStyle);
        }

        // 左右边
        for (int row = y + 1; row < y + h - 1; row++) {
            screen.write(x, row, border.getLeft(), leftStyle);
            screen.write(x + w - 1, row, border.getRight(), rightStyle);
        }
    }

    /**
     * 构建边框某边的样式，支持颜色和 dimmed 属性。
     */
    private static Style buildBorderSideStyle(Color color, boolean dimmed) {
        if (color == null && !dimmed) return null;
        Style.Builder b = Style.builder();
        if (color != null) b.color(color);
        if (dimmed) b.dimmed(true);
        return b.build();
    }

    /**
     * 渲染文本内容（支持嵌套 Text 的独立样式）
     * 支持 textWrap 模式：WRAP（自动换行）、TRUNCATE/TRUNCATE_END/TRUNCATE_START/TRUNCATE_MIDDLE（截断）
     */
    private static void renderText(ElementNode textNode, VirtualScreen screen,
                                   int x, int y, int w, int h, Style style) {
        int borderLeft = style.hasBorder() ? 1 : 0;
        int borderTop = style.hasBorder() ? 1 : 0;
        int contentX = x + borderLeft + style.paddingLeft();
        int contentY = y + borderTop + style.paddingTop();
        int contentW = w - style.horizontalBorderWidth() - style.horizontalPadding();
        if (contentW <= 0) return;

        // 收集所有带样式的文本片段
        List<StyledSpan> spans = new ArrayList<>();
        collectStyledSpans(textNode, textNode.getStyle(), spans);
        if (spans.isEmpty()) return;

        TextWrap textWrap = style.textWrap();
        if (textWrap == null || textWrap == TextWrap.WRAP) {
            // 原有自动换行逻辑
            renderTextWrap(spans, screen, contentX, contentY, contentW, y + h);
        } else {
            // 截断模式：按逻辑行截断
            renderTextTruncate(spans, screen, contentX, contentY, contentW, y + h, textWrap);
        }
    }

    /**
     * 自动换行模式渲染文本（原有逻辑）
     */
    private static void renderTextWrap(List<StyledSpan> spans, VirtualScreen screen,
                                       int contentX, int contentY, int contentW, int maxY) {
        int curX = contentX;
        int curY = contentY;

        for (StyledSpan span : spans) {
            String text = span.text;
            Style spanStyle = span.style;

            for (int i = 0; i < text.length(); ) {
                if (curY >= maxY) return;

                int cp = text.codePointAt(i);
                if (cp == '\n') {
                    curY++;
                    curX = contentX;
                    i += Character.charCount(cp);
                    continue;
                }

                int charWidth = AnsiStringUtils.isWideChar(cp) ? 2 : 1;
                if (curX + charWidth - contentX > contentW) {
                    curY++;
                    curX = contentX;
                    if (curY >= maxY) return;
                }

                screen.write(curX, curY, new String(Character.toChars(cp)), spanStyle);
                curX += charWidth;
                i += Character.charCount(cp);
            }
        }
    }

    /**
     * 截断模式渲染文本：对每个逻辑行独立截断，不换行
     */
    private static void renderTextTruncate(List<StyledSpan> spans, VirtualScreen screen,
                                           int contentX, int contentY, int contentW,
                                           int maxY, TextWrap mode) {
        // 构建带样式的字符列表，按换行符分割为逻辑行
        List<List<StyledCodePoint>> lines = buildStyledLines(spans);

        int curY = contentY;
        for (List<StyledCodePoint> line : lines) {
            if (curY >= maxY) return;
            renderTruncatedLine(line, screen, contentX, curY, contentW, mode);
            curY++;
        }
    }

    /**
     * 渲染单行截断文本
     */
    private static void renderTruncatedLine(List<StyledCodePoint> line, VirtualScreen screen,
                                            int contentX, int curY, int contentW, TextWrap mode) {
        int totalWidth = 0;
        for (StyledCodePoint sc : line) totalWidth += sc.displayWidth;

        // 宽度够，直接渲染
        if (totalWidth <= contentW) {
            int curX = contentX;
            for (StyledCodePoint sc : line) {
                screen.write(curX, curY, new String(Character.toChars(sc.codePoint)), sc.style);
                curX += sc.displayWidth;
            }
            return;
        }

        // 容器太窄，只能放省略号
        if (contentW <= 1) {
            if (contentW == 1 && !line.isEmpty()) {
                screen.write(contentX, curY, ELLIPSIS, line.get(0).style);
            }
            return;
        }

        int availW = contentW - ELLIPSIS_WIDTH; // 留 1 列给 "…"

        if (mode == TextWrap.TRUNCATE || mode == TextWrap.TRUNCATE_END) {
            // 从头开始取字符，末尾加 …
            int curX = contentX;
            Style lastStyle = null;
            for (StyledCodePoint sc : line) {
                if (curX - contentX + sc.displayWidth > availW) break;
                screen.write(curX, curY, new String(Character.toChars(sc.codePoint)), sc.style);
                lastStyle = sc.style;
                curX += sc.displayWidth;
            }
            screen.write(curX, curY, ELLIPSIS, lastStyle);
        } else if (mode == TextWrap.TRUNCATE_START) {
            // 从末尾取字符，开头加 …
            int startIdx = line.size();
            int accWidth = 0;
            for (int i = line.size() - 1; i >= 0; i--) {
                if (accWidth + line.get(i).displayWidth > availW) break;
                accWidth += line.get(i).displayWidth;
                startIdx = i;
            }
            int curX = contentX;
            screen.write(curX, curY, ELLIPSIS, line.get(startIdx).style);
            curX += ELLIPSIS_WIDTH;
            for (int i = startIdx; i < line.size(); i++) {
                screen.write(curX, curY, new String(Character.toChars(line.get(i).codePoint)), line.get(i).style);
                curX += line.get(i).displayWidth;
            }
        } else if (mode == TextWrap.TRUNCATE_MIDDLE) {
            // 保留首尾，中间加 …
            int firstHalfW = availW / 2;
            int secondHalfW = availW - firstHalfW;

            // 前半部分
            int curX = contentX;
            int firstEndIdx = 0;
            int accWidth = 0;
            for (int i = 0; i < line.size(); i++) {
                if (accWidth + line.get(i).displayWidth > firstHalfW) break;
                accWidth += line.get(i).displayWidth;
                firstEndIdx = i + 1;
            }
            for (int i = 0; i < firstEndIdx; i++) {
                screen.write(curX, curY, new String(Character.toChars(line.get(i).codePoint)), line.get(i).style);
                curX += line.get(i).displayWidth;
            }

            // 省略号
            Style midStyle = firstEndIdx > 0 ? line.get(firstEndIdx - 1).style
                    : (line.isEmpty() ? null : line.get(0).style);
            screen.write(curX, curY, ELLIPSIS, midStyle);
            curX += ELLIPSIS_WIDTH;

            // 后半部分
            int lastStartIdx = line.size();
            accWidth = 0;
            for (int i = line.size() - 1; i >= 0; i--) {
                if (accWidth + line.get(i).displayWidth > secondHalfW) break;
                accWidth += line.get(i).displayWidth;
                lastStartIdx = i;
            }
            for (int i = lastStartIdx; i < line.size(); i++) {
                screen.write(curX, curY, new String(Character.toChars(line.get(i).codePoint)), line.get(i).style);
                curX += line.get(i).displayWidth;
            }
        }
    }

    // 省略号字符及其显示宽度
    private static final String ELLIPSIS = "…";
    private static final int ELLIPSIS_WIDTH = 1;

    /**
     * 带样式的字符（code point + 显示宽度 + 样式）
     */
    private static final class StyledCodePoint {
        final int codePoint;
        final int displayWidth;
        final Style style;
        StyledCodePoint(int codePoint, int displayWidth, Style style) {
            this.codePoint = codePoint;
            this.displayWidth = displayWidth;
            this.style = style;
        }
    }

    /**
     * 将文本片段列表转换为按换行符分割的逻辑行
     */
    private static List<List<StyledCodePoint>> buildStyledLines(List<StyledSpan> spans) {
        List<List<StyledCodePoint>> lines = new ArrayList<>();
        List<StyledCodePoint> currentLine = new ArrayList<>();

        for (StyledSpan span : spans) {
            String text = span.text;
            Style style = span.style;
            for (int i = 0; i < text.length(); ) {
                int cp = text.codePointAt(i);
                if (cp == '\n') {
                    lines.add(currentLine);
                    currentLine = new ArrayList<>();
                } else {
                    int w = AnsiStringUtils.isWideChar(cp) ? 2 : 1;
                    currentLine.add(new StyledCodePoint(cp, w, style));
                }
                i += Character.charCount(cp);
            }
        }
        lines.add(currentLine);
        return lines;
    }

    /**
     * 递归收集文本节点树中所有带样式的文本片段
     */
    private static void collectStyledSpans(Node node, Style inheritedStyle,
                                           List<StyledSpan> spans) {
        if (node instanceof TextNode) {
            TextNode tn = (TextNode) node;
            String text = tn.getNodeValue();
            if (text != null && !text.isEmpty()) {
                spans.add(new StyledSpan(text, inheritedStyle));
            }
        } else if (node instanceof ElementNode) {
            ElementNode en = (ElementNode) node;
            Style merged = mergeTextStyles(inheritedStyle, en.getStyle());
            for (Node child : en.getChildNodes()) {
                collectStyledSpans(child, merged, spans);
            }
        }
    }

    /**
     * 合并文本样式：子节点的非默认值覆盖父节点
     */
    private static Style mergeTextStyles(Style parent, Style child) {
        return Style.builder()
                .color(child.color() != null ? child.color() : parent.color())
                .backgroundColor(child.backgroundColor() != null
                        ? child.backgroundColor() : parent.backgroundColor())
                .bold(child.bold() || parent.bold())
                .italic(child.italic() || parent.italic())
                .underline(child.underline() || parent.underline())
                .strikethrough(child.strikethrough() || parent.strikethrough())
                .inverse(child.inverse() || parent.inverse())
                .dimmed(child.dimmed() || parent.dimmed())
                .build();
    }

    /**
     * 文本片段（带样式信息）
     */
    private static final class StyledSpan {
        final String text;
        final Style style;
        StyledSpan(String text, Style style) {
            this.text = text;
            this.style = style;
        }
    }

    private static boolean isOverflowHidden(Style style) {
        if (style.overflow() == io.mybatis.jink.style.Overflow.HIDDEN) return true;
        if (style.overflowX() == io.mybatis.jink.style.Overflow.HIDDEN) return true;
        if (style.overflowY() == io.mybatis.jink.style.Overflow.HIDDEN) return true;
        return false;
    }

    /**
     * Transform 渲染：先将子节点渲染到临时屏幕，逐行调用 transform 函数后写回主屏幕
     */
    @SuppressWarnings("unchecked")
    private static void renderWithTransform(ElementNode node, VirtualScreen screen,
                                            int x, int y, int w, int h,
                                            BiFunction<String, Integer, String> transformFn) {
        // 创建临时屏幕渲染子节点
        VirtualScreen tempScreen = new VirtualScreen(w, h);
        for (Node child : node.getChildNodes()) {
            if (child instanceof ElementNode) {
                renderNode((ElementNode) child, tempScreen, 0, 0);
            }
        }

        // 对临时屏幕的每一行应用 transform
        String tempOutput = tempScreen.render();
        String[] lines = tempOutput.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String transformed = transformFn.apply(lines[i], i);
            if (transformed != null && !transformed.isEmpty()) {
                screen.write(x, y + i, transformed, null);
            }
        }
    }

    /**
     * 渲染滚动条，使用 ▲▼ 符号在右侧指示当前位置。
     * 滚动条显示在容器的最右侧一列。
     */
    private static void renderScrollbar(VirtualScreen screen, int x, int y, int w, int h,
                                        Style style, int scrollOffset, int viewportHeight, int contentHeight) {
        int scrollbarX = x + w - 1;
        int contentY = y + (style.hasBorder() ? 1 : 0);
        int contentH = h - style.verticalBorderWidth();

        if (contentH <= 0) return;

        Style scrollbarStyle = Style.builder()
                .dimmed(true)
                .build();

        boolean canScrollUp = scrollOffset > 0;
        boolean canScrollDown = scrollOffset + viewportHeight < contentHeight;

        if (canScrollUp) {
            screen.write(scrollbarX, contentY, "▲", scrollbarStyle);
        }

        if (canScrollDown) {
            screen.write(scrollbarX, contentY + contentH - 1, "▼", scrollbarStyle);
        }

        int thumbHeight = Math.max(1, (int) Math.round((double) viewportHeight * contentH / contentHeight));
        int thumbOffset = (int) Math.round((double) scrollOffset * (contentH - thumbHeight) / (contentHeight - viewportHeight));

        Style thumbStyle = Style.builder()
                .color(Color.WHITE)
                .build();

        int thumbStart = contentY + thumbOffset;
        int thumbEnd = thumbStart + thumbHeight;

        if (canScrollUp) {
            thumbStart = Math.max(thumbStart, contentY + 1);
        }
        if (canScrollDown) {
            thumbEnd = Math.min(thumbEnd, contentY + contentH - 1);
        }

        for (int i = thumbStart; i < thumbEnd; i++) {
            screen.write(scrollbarX, i, "█", thumbStyle);
        }
    }

}
