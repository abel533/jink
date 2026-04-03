package io.mybatis.jink.style;

/**
 * 多行交叉轴对齐方式，对应 CSS align-content。
 * 仅在 flexWrap 启用且存在多行时生效。
 */
public enum AlignContent {
    FLEX_START,
    CENTER,
    FLEX_END,
    STRETCH,
    SPACE_BETWEEN,
    SPACE_AROUND,
    SPACE_EVENLY
}
