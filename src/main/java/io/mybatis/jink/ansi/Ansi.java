package io.mybatis.jink.ansi;

import io.mybatis.jink.style.Color;
import io.mybatis.jink.style.Style;

/**
 * ANSI 转义序列工具类，对应 ink 的 ansi-escapes 功能。
 * 提供终端控制所需的所有 ANSI 转义码。
 */
public final class Ansi {

    private Ansi() {}

    // ===== 转义序列前缀 =====
    public static final String ESC = "\u001B";
    public static final String CSI = ESC + "[";

    // ===== 样式重置 =====
    public static final String RESET = CSI + "0m";

    // ===== 文本样式 =====
    public static final String BOLD_ON = CSI + "1m";
    public static final String BOLD_OFF = CSI + "22m";
    public static final String DIM_ON = CSI + "2m";
    public static final String DIM_OFF = CSI + "22m";
    public static final String ITALIC_ON = CSI + "3m";
    public static final String ITALIC_OFF = CSI + "23m";
    public static final String UNDERLINE_ON = CSI + "4m";
    public static final String UNDERLINE_OFF = CSI + "24m";
    public static final String INVERSE_ON = CSI + "7m";
    public static final String INVERSE_OFF = CSI + "27m";
    public static final String STRIKETHROUGH_ON = CSI + "9m";
    public static final String STRIKETHROUGH_OFF = CSI + "29m";

    // ===== 光标控制 =====

    /** 隐藏光标 */
    public static final String CURSOR_HIDE = CSI + "?25l";
    /** 显示光标 */
    public static final String CURSOR_SHOW = CSI + "?25h";

    /** 光标移动到指定位置 (1-based) */
    public static String cursorTo(int x, int y) {
        return CSI + (y + 1) + ";" + (x + 1) + "H";
    }

    /** 光标移动到指定列 (0-based) */
    public static String cursorToColumn(int x) {
        return CSI + (x + 1) + "G";
    }

    /** 光标上移 n 行 */
    public static String cursorUp(int n) {
        return n > 0 ? CSI + n + "A" : "";
    }

    /** 光标下移 n 行 */
    public static String cursorDown(int n) {
        return n > 0 ? CSI + n + "B" : "";
    }

    /** 光标右移 n 列 */
    public static String cursorForward(int n) {
        return n > 0 ? CSI + n + "C" : "";
    }

    /** 光标左移 n 列 */
    public static String cursorBackward(int n) {
        return n > 0 ? CSI + n + "D" : "";
    }

    /** 光标移到下一行开头 */
    public static String cursorNextLine(int n) {
        return n > 0 ? CSI + n + "E" : "";
    }

    /** 保存光标位置 */
    public static final String CURSOR_SAVE = CSI + "s";
    /** 恢复光标位置 */
    public static final String CURSOR_RESTORE = CSI + "u";

    // ===== 清除操作 =====

    /** 清除从光标到行尾 */
    public static final String ERASE_END_OF_LINE = CSI + "0K";
    /** 清除从行首到光标 */
    public static final String ERASE_START_OF_LINE = CSI + "1K";
    /** 清除整行 */
    public static final String ERASE_LINE = CSI + "2K";
    /** 清除从光标到屏幕底部 */
    public static final String ERASE_DOWN = CSI + "0J";
    /** 清除从光标到屏幕顶部 */
    public static final String ERASE_UP = CSI + "1J";
    /** 清除整个屏幕 */
    public static final String ERASE_SCREEN = CSI + "2J";

    /**
     * 清除指定行数（从当前位置向上擦除）。
     * 对应 ink 的 ansiEscapes.eraseLines。
     */
    public static String eraseLines(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(ERASE_LINE);
            if (i < count - 1) {
                sb.append(cursorUp(1));
            }
        }
        if (count > 0) {
            sb.append(cursorToColumn(0));
        }
        return sb.toString();
    }

    // ===== 备用屏幕 =====

    /** 进入备用屏幕 */
    public static final String ENTER_ALT_SCREEN = CSI + "?1049h";
    /** 离开备用屏幕 */
    public static final String EXIT_ALT_SCREEN = CSI + "?1049l";

    // ===== 粘贴模式 =====

    /** 启用 Bracketed Paste Mode */
    public static final String ENABLE_BRACKETED_PASTE = CSI + "?2004h";
    /** 禁用 Bracketed Paste Mode */
    public static final String DISABLE_BRACKETED_PASTE = CSI + "?2004l";

    // ===== 样式应用 =====

    /**
     * 将文本样式属性应用为 ANSI 开始序列
     */
    public static String styleOpen(Style style) {
        StringBuilder sb = new StringBuilder();
        if (style.bold()) sb.append(BOLD_ON);
        if (style.dimmed()) sb.append(DIM_ON);
        if (style.italic()) sb.append(ITALIC_ON);
        if (style.underline()) sb.append(UNDERLINE_ON);
        if (style.inverse()) sb.append(INVERSE_ON);
        if (style.strikethrough()) sb.append(STRIKETHROUGH_ON);
        if (style.color() != null) sb.append(style.color().toForeground());
        if (style.backgroundColor() != null) sb.append(style.backgroundColor().toBackground());
        return sb.toString();
    }

    /**
     * 生成样式关闭序列
     */
    public static String styleClose(Style style) {
        StringBuilder sb = new StringBuilder();
        if (style.strikethrough()) sb.append(STRIKETHROUGH_OFF);
        if (style.inverse()) sb.append(INVERSE_OFF);
        if (style.underline()) sb.append(UNDERLINE_OFF);
        if (style.italic()) sb.append(ITALIC_OFF);
        if (style.dimmed()) sb.append(DIM_OFF);
        if (style.bold()) sb.append(BOLD_OFF);
        if (style.color() != null || style.backgroundColor() != null) sb.append(RESET);
        return sb.toString();
    }

    /**
     * 用 ANSI 样式包裹文本
     */
    public static String styled(String text, Style style) {
        String open = styleOpen(style);
        String close = styleClose(style);
        if (open.isEmpty() && close.isEmpty()) {
            return text;
        }
        return open + text + close;
    }
}
