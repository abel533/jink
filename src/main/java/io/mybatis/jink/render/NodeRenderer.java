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
        if (clipped) {
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

        if (transformFn != null) {
            renderWithTransform(node, screen, x, y, w, h, transformFn);
        } else {
            // 递归渲染子节点
            for (Node child : node.getChildNodes()) {
                if (child instanceof ElementNode childElem) {
                    renderNode(childElem, screen, x, y);
                }
            }
        }

        if (clipped) {
            screen.popClip();
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
        Style borderTextStyle = style.borderColor() != null
                ? Style.builder().color(style.borderColor()).build()
                : null;

        // 四个角
        screen.write(x, y, border.getTopLeft(), borderTextStyle);
        screen.write(x + w - 1, y, border.getTopRight(), borderTextStyle);
        screen.write(x, y + h - 1, border.getBottomLeft(), borderTextStyle);
        screen.write(x + w - 1, y + h - 1, border.getBottomRight(), borderTextStyle);

        // 上下边
        for (int col = x + 1; col < x + w - 1; col++) {
            screen.write(col, y, border.getTop(), borderTextStyle);
            screen.write(col, y + h - 1, border.getBottom(), borderTextStyle);
        }

        // 左右边
        for (int row = y + 1; row < y + h - 1; row++) {
            screen.write(x, row, border.getLeft(), borderTextStyle);
            screen.write(x + w - 1, row, border.getRight(), borderTextStyle);
        }
    }

    /**
     * 渲染文本内容（支持嵌套 Text 的独立样式）
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

        // 逐字符渲染，支持换行和自动折行
        int curX = contentX;
        int curY = contentY;

        for (StyledSpan span : spans) {
            String text = span.text;
            Style spanStyle = span.style;

            for (int i = 0; i < text.length(); ) {
                if (curY >= y + h) break;

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
                    if (curY >= y + h) break;
                }

                screen.write(curX, curY, new String(Character.toChars(cp)), spanStyle);
                curX += charWidth;
                i += Character.charCount(cp);
            }
        }
    }

    /**
     * 递归收集文本节点树中所有带样式的文本片段
     */
    private static void collectStyledSpans(Node node, Style inheritedStyle,
                                           List<StyledSpan> spans) {
        if (node instanceof TextNode tn) {
            String text = tn.getNodeValue();
            if (text != null && !text.isEmpty()) {
                spans.add(new StyledSpan(text, inheritedStyle));
            }
        } else if (node instanceof ElementNode en) {
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
    private record StyledSpan(String text, Style style) {}

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
            if (child instanceof ElementNode childElem) {
                renderNode(childElem, tempScreen, 0, 0);
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

}
