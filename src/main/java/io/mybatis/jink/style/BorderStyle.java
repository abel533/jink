package io.mybatis.jink.style;

/**
 * 边框样式，对应 ink 的 borderStyle。
 * 使用 Unicode box-drawing 字符绘制。
 */
public enum BorderStyle {
    /** 无边框 */
    NONE("", "", "", "", "", "", "", ""),
    /** 单线边框 ┌─┐│└─┘ */
    SINGLE("┌", "─", "┐", "│", "└", "─", "┘", "│"),
    /** 双线边框 ╔═╗║╚═╝ */
    DOUBLE("╔", "═", "╗", "║", "╚", "═", "╝", "║"),
    /** 圆角边框 ╭─╮│╰─╯ */
    ROUND("╭", "─", "╮", "│", "╰", "─", "╯", "│"),
    /** 粗线边框 ┏━┓┃┗━┛ */
    BOLD("┏", "━", "┓", "┃", "┗", "━", "┛", "┃"),
    /** 单双混合 ╓─╖║╙─╜ */
    SINGLE_DOUBLE("╓", "─", "╖", "║", "╙", "─", "╜", "║"),
    /** 双单混合 ╒═╕│╘═╛ */
    DOUBLE_SINGLE("╒", "═", "╕", "│", "╘", "═", "╛", "│"),
    /** 经典边框 +--+|+--+ */
    CLASSIC("+", "-", "+", "|", "+", "-", "+", "|"),
    /** 箭头边框 */
    ARROW("↘", "↓", "↙", "←", "↗", "↑", "↖", "→");

    private final String topLeft;
    private final String top;
    private final String topRight;
    private final String right;
    private final String bottomLeft;
    private final String bottom;
    private final String bottomRight;
    private final String left;

    BorderStyle(String topLeft, String top, String topRight, String right,
                String bottomLeft, String bottom, String bottomRight, String left) {
        this.topLeft = topLeft;
        this.top = top;
        this.topRight = topRight;
        this.right = right;
        this.bottomLeft = bottomLeft;
        this.bottom = bottom;
        this.bottomRight = bottomRight;
        this.left = left;
    }

    public String getTopLeft() { return topLeft; }
    public String getTop() { return top; }
    public String getTopRight() { return topRight; }
    public String getRight() { return right; }
    public String getBottomLeft() { return bottomLeft; }
    public String getBottom() { return bottom; }
    public String getBottomRight() { return bottomRight; }
    public String getLeft() { return left; }

    /**
     * 边框是否占用布局空间（宽度1）
     */
    public boolean hasBorder() {
        return this != NONE;
    }
}
