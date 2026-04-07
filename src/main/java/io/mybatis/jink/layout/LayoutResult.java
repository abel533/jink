package io.mybatis.jink.layout;

/**
 * 布局计算结果，存储每个节点计算后的位置和尺寸。
 */
public final class LayoutResult {
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public LayoutResult(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int x()      { return x; }
    public int y()      { return y; }
    public int width()  { return width; }
    public int height() { return height; }

    public static final LayoutResult ZERO = new LayoutResult(0, 0, 0, 0);
}
