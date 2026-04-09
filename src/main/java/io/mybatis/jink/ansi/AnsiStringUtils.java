package io.mybatis.jink.ansi;

import io.mybatis.jink.util.StringWidth;

import java.util.regex.Pattern;

/**
 * ANSI 字符串工具类。
 * 用于测量含 ANSI 转义序列的文本的可见宽度、截取等。
 */
public final class AnsiStringUtils {

    private AnsiStringUtils() {}

    /** 匹配所有 ANSI 转义序列的正则 */
    private static final Pattern ANSI_PATTERN = Pattern.compile(
            "\u001B\\[[0-9;]*[a-zA-Z]|\u001B\\][^\u0007]*\u0007|\u001B\\[[0-9;]*m"
    );

    /**
     * 去除字符串中所有 ANSI 转义序列
     */
    public static String stripAnsi(String text) {
        if (text == null || text.isEmpty()) return "";
        return ANSI_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * 计算字符串的可见宽度（去除 ANSI 后）。
     * 考虑中文等宽字符占 2 列。
     */
    public static int visibleWidth(String text) {
        if (text == null || text.isEmpty()) return 0;
        return StringWidth.width(stripAnsi(text));
    }

    /**
     * 计算多行文本中最宽一行的可见宽度
     */
    public static int widestLine(String text) {
        if (text == null || text.isEmpty()) return 0;
        int max = 0;
        for (String line : text.split("\n", -1)) {
            max = Math.max(max, visibleWidth(line));
        }
        return max;
    }

    /**
     * 判断字符是否为宽字符（CJK等，占 2 列）
     */
    public static boolean isWideChar(int codePoint) {
        return StringWidth.charWidth(codePoint) == 2;
    }

    /**
     * 按可见宽度截取字符串（保留 ANSI 序列）
     */
    public static String sliceAnsi(String text, int start, int end) {
        if (text == null || text.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        StringBuilder currentAnsi = new StringBuilder();
        int visiblePos = 0;
        boolean inAnsi = false;

        for (int i = 0; i < text.length(); ) {
            char ch = text.charAt(i);

            // 检测 ANSI 转义序列开始
            if (ch == '\u001B' && i + 1 < text.length() && text.charAt(i + 1) == '[') {
                inAnsi = true;
                currentAnsi.setLength(0);
                currentAnsi.append(ch);
                i++;
                continue;
            }

            if (inAnsi) {
                currentAnsi.append(ch);
                if (Character.isLetter(ch)) {
                    inAnsi = false;
                    // 在可见范围内的 ANSI 序列保留
                    if (visiblePos >= start && visiblePos < end) {
                        result.append(currentAnsi);
                    }
                }
                i++;
                continue;
            }

            int cp = text.codePointAt(i);
            int charWidth = StringWidth.charWidth(cp);

            if (visiblePos >= start && visiblePos + charWidth <= end) {
                result.appendCodePoint(cp);
            }

            visiblePos += charWidth;
            i += Character.charCount(cp);

            if (visiblePos >= end) break;
        }

        return result.toString();
    }
}
