package io.mybatis.jink.render;

import io.mybatis.jink.ansi.AnsiStringUtils;
import io.mybatis.jink.style.Color;
import io.mybatis.jink.style.Style;

import java.util.*;

/**
 * 虚拟屏幕，对应 ink 的 Output 类。
 * 维护一个二维字符网格，所有渲染操作先写入此缓冲，最后一次性转为 ANSI 字符串。
 */
public class VirtualScreen {

    private final int width;
    private final int height;
    private final StyledChar[][] grid;
    private final Deque<ClipRegion> clipStack = new ArrayDeque<>();

    public VirtualScreen(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.grid = new StyledChar[this.height][this.width];
        clear();
    }

    /**
     * 清空屏幕为空字符
     */
    public void clear() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                grid[y][x] = StyledChar.SPACE;
            }
        }
    }

    /**
     * 在指定位置写入纯文本
     */
    public void write(int x, int y, String text) {
        write(x, y, text, null);
    }

    /**
     * 在指定位置写入带样式的文本
     */
    public void write(int x, int y, String text, Style style) {
        if (text == null || text.isEmpty()) return;

        String stripped = AnsiStringUtils.stripAnsi(text);
        int col = x;

        for (int i = 0; i < stripped.length(); ) {
            int cp = stripped.codePointAt(i);
            int charWidth = AnsiStringUtils.isWideChar(cp) ? 2 : 1;

            if (isInClipRegion(col, y)) {
                putChar(col, y, new String(Character.toChars(cp)), style);
                // 宽字符占两列，第二列用占位符
                if (charWidth == 2 && col + 1 < width) {
                    putChar(col + 1, y, "", style);
                }
            }

            col += charWidth;
            i += Character.charCount(cp);
        }
    }

    /**
     * 写入多行文本
     */
    public void writeLines(int x, int y, String text, Style style) {
        if (text == null) return;
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            write(x, y + i, lines[i], style);
        }
    }

    /**
     * 在指定区域填充字符（用于背景色）
     */
    public void fill(int x, int y, int w, int h, char ch, Style style) {
        for (int row = y; row < y + h && row < height; row++) {
            for (int col = x; col < x + w && col < width; col++) {
                if (isInClipRegion(col, row)) {
                    putChar(col, row, String.valueOf(ch), style);
                }
            }
        }
    }

    /**
     * 在指定行水平填充字符
     */
    public void fillHorizontal(int x, int y, int length, String ch, Style style) {
        for (int col = x; col < x + length && col < width; col++) {
            if (isInClipRegion(col, y)) {
                putChar(col, y, ch, style);
            }
        }
    }

    /**
     * 在指定列垂直填充字符
     */
    public void fillVertical(int x, int y, int length, String ch, Style style) {
        for (int row = y; row < y + length && row < height; row++) {
            if (isInClipRegion(x, row)) {
                putChar(x, row, ch, style);
            }
        }
    }

    // ===== Clipping =====

    public void pushClip(int x, int y, int w, int h) {
        clipStack.push(new ClipRegion(x, y, x + w, y + h));
    }

    public void popClip() {
        if (!clipStack.isEmpty()) {
            clipStack.pop();
        }
    }

    private boolean isInClipRegion(int x, int y) {
        if (clipStack.isEmpty()) return true;
        ClipRegion clip = clipStack.peek();
        return x >= clip.x1 && x < clip.x2 && y >= clip.y1 && y < clip.y2;
    }

    // ===== 输出 =====

    /**
     * 将虚拟屏幕转为 ANSI 字符串输出
     */
    public String render() {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < height; y++) {
            if (y > 0) sb.append('\n');
            renderLine(sb, y);
        }
        return sb.toString();
    }

    /**
     * 渲染单行，合并相同样式的字符以减少 ANSI 序列
     */
    private void renderLine(StringBuilder sb, int y) {
        // 从行尾找到最后一个有效字符（跳过默认空格和宽字符占位符）
        int lastNonSpace = width - 1;
        while (lastNonSpace >= 0) {
            StyledChar sc = grid[y][lastNonSpace];
            if (sc.equals(StyledChar.SPACE) || sc.ch().isEmpty()) {
                lastNonSpace--;
            } else {
                break;
            }
        }

        Style currentStyle = null;
        for (int x = 0; x <= lastNonSpace; x++) {
            StyledChar sc = grid[y][x];
            // 跳过宽字符占位符（空字符串）
            if (sc.ch().isEmpty()) continue;

            if (!Objects.equals(sc.style(), currentStyle)) {
                // 样式变化，先关闭旧样式
                if (currentStyle != null) {
                    sb.append("\u001B[0m");
                }
                // 开启新样式
                if (sc.style() != null) {
                    sb.append(buildStyleSequence(sc.style()));
                }
                currentStyle = sc.style();
            }
            sb.append(sc.ch());
        }
        // 关闭最后的样式
        if (currentStyle != null) {
            sb.append("\u001B[0m");
        }
    }

    /**
     * 构建 ANSI 样式序列
     */
    private String buildStyleSequence(Style style) {
        StringBuilder sb = new StringBuilder();
        if (style.bold()) sb.append("\u001B[1m");
        if (style.dimmed()) sb.append("\u001B[2m");
        if (style.italic()) sb.append("\u001B[3m");
        if (style.underline()) sb.append("\u001B[4m");
        if (style.inverse()) sb.append("\u001B[7m");
        if (style.strikethrough()) sb.append("\u001B[9m");
        if (style.color() != null) sb.append(style.color().toForeground());
        if (style.backgroundColor() != null) sb.append(style.backgroundColor().toBackground());
        return sb.toString();
    }

    // ===== 内部方法 =====

    private void putChar(int x, int y, String ch, Style style) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            grid[y][x] = new StyledChar(ch, style);
        }
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    /**
     * 获取指定位置的字符（用于测试）
     */
    public StyledChar charAt(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return grid[y][x];
        }
        return StyledChar.SPACE;
    }

    // ===== 内部类型 =====

    /**
     * 带样式的字符
     */
    public record StyledChar(String ch, Style style) {
        public static final StyledChar SPACE = new StyledChar(" ", null);
    }

    private record ClipRegion(int x1, int y1, int x2, int y2) {}
}
