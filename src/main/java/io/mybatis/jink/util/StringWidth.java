package io.mybatis.jink.util;

/**
 * 计算字符串在终端中的显示宽度。
 * CJK（中日韩）字符和全角字符占 2 列，其他字符占 1 列。
 */
public final class StringWidth {

    private StringWidth() {
    }

    /**
     * 计算字符串的终端显示宽度
     */
    public static int width(String text) {
        if (text == null || text.isEmpty()) return 0;
        int width = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            width += charWidth(cp);
            i += Character.charCount(cp);
        }
        return width;
    }

    /**
     * 判断单个 Unicode 码点的显示宽度（0, 1 或 2）
     */
    public static int charWidth(int codePoint) {
        if (codePoint < 0x20 || (codePoint >= 0x7F && codePoint < 0xA0)) return 0;
        if (codePoint == 0x200B || codePoint == 0x200C || codePoint == 0x200D
                || codePoint == 0xFEFF) return 0;
        if (Character.getType(codePoint) == Character.NON_SPACING_MARK
                || Character.getType(codePoint) == Character.ENCLOSING_MARK) return 0;
        if (isWideChar(codePoint)) return 2;
        return 1;
    }

    /**
     * 判断是否为双宽度字符（East Asian Wide/Fullwidth）
     */
    private static boolean isWideChar(int cp) {
        // CJK 统一表意文字
        if (cp >= 0x4E00 && cp <= 0x9FFF) return true;
        // CJK 扩展 A
        if (cp >= 0x3400 && cp <= 0x4DBF) return true;
        // CJK 兼容表意文字
        if (cp >= 0xF900 && cp <= 0xFAFF) return true;
        // CJK 统一表意文字扩展 B-F
        if (cp >= 0x20000 && cp <= 0x2FA1F) return true;
        // 韩文音节
        if (cp >= 0xAC00 && cp <= 0xD7AF) return true;
        // 全角 ASCII 和标点
        if (cp >= 0xFF01 && cp <= 0xFF60) return true;
        if (cp >= 0xFFE0 && cp <= 0xFFE6) return true;
        // CJK 符号和标点
        if (cp >= 0x3000 && cp <= 0x303F) return true;
        // 平假名
        if (cp >= 0x3040 && cp <= 0x309F) return true;
        // 片假名
        if (cp >= 0x30A0 && cp <= 0x30FF) return true;
        // 注音符号
        if (cp >= 0x3100 && cp <= 0x312F) return true;
        // CJK 笔画
        if (cp >= 0x31C0 && cp <= 0x31EF) return true;
        // 围棋、象形等符号
        if (cp >= 0x3200 && cp <= 0x32FF) return true;
        // CJK 兼容
        if (cp >= 0x3300 && cp <= 0x33FF) return true;
        // 部首补充
        if (cp >= 0x2E80 && cp <= 0x2FDF) return true;
        // 竖排标点
        if (cp >= 0xFE30 && cp <= 0xFE4F) return true;
        return false;
    }
}
