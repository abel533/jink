package io.mybatis.jink.layout;

/**
 * 布局计算结果，存储每个节点计算后的位置和尺寸。
 */
public record LayoutResult(
        int x,
        int y,
        int width,
        int height
) {
    public static final LayoutResult ZERO = new LayoutResult(0, 0, 0, 0);
}
