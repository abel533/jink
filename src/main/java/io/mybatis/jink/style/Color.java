package io.mybatis.jink.style;

/**
 * 终端颜色定义，支持基础16色和256色及RGB真彩色。
 */
public sealed interface Color {

    /** 生成前景色 ANSI 转义码 */
    String toForeground();

    /** 生成背景色 ANSI 转义码 */
    String toBackground();

    // ===== 基础 16 色 =====

    Color BLACK = new Basic(30);
    Color RED = new Basic(31);
    Color GREEN = new Basic(32);
    Color YELLOW = new Basic(33);
    Color BLUE = new Basic(34);
    Color MAGENTA = new Basic(35);
    Color CYAN = new Basic(36);
    Color WHITE = new Basic(37);
    Color DEFAULT = new Basic(39);

    // 亮色
    Color BRIGHT_BLACK = new Basic(90);
    Color BRIGHT_RED = new Basic(91);
    Color BRIGHT_GREEN = new Basic(92);
    Color BRIGHT_YELLOW = new Basic(93);
    Color BRIGHT_BLUE = new Basic(94);
    Color BRIGHT_MAGENTA = new Basic(95);
    Color BRIGHT_CYAN = new Basic(96);
    Color BRIGHT_WHITE = new Basic(97);

    // 别名
    Color GRAY = BRIGHT_BLACK;
    Color GREY = BRIGHT_BLACK;

    /**
     * 256 色（0-255）
     */
    static Color ansi256(int code) {
        if (code < 0 || code > 255) {
            throw new IllegalArgumentException("ANSI 256 color code must be 0-255, got: " + code);
        }
        return new Ansi256(code);
    }

    /**
     * RGB 真彩色
     */
    static Color rgb(int r, int g, int b) {
        if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
            throw new IllegalArgumentException("RGB values must be 0-255");
        }
        return new Rgb(r, g, b);
    }

    /**
     * 十六进制颜色 (#RRGGBB 或 RRGGBB)
     */
    static Color hex(String hex) {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() != 6) {
            throw new IllegalArgumentException("Hex color must be 6 characters, got: " + hex);
        }
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return new Rgb(r, g, b);
    }

    // ===== 实现类 =====

    record Basic(int code) implements Color {
        @Override
        public String toForeground() {
            return "\u001B[" + code + "m";
        }

        @Override
        public String toBackground() {
            return "\u001B[" + (code + 10) + "m";
        }
    }

    record Ansi256(int code) implements Color {
        @Override
        public String toForeground() {
            return "\u001B[38;5;" + code + "m";
        }

        @Override
        public String toBackground() {
            return "\u001B[48;5;" + code + "m";
        }
    }

    record Rgb(int r, int g, int b) implements Color {
        @Override
        public String toForeground() {
            return "\u001B[38;2;" + r + ";" + g + ";" + b + "m";
        }

        @Override
        public String toBackground() {
            return "\u001B[48;2;" + r + ";" + g + ";" + b + "m";
        }
    }
}
