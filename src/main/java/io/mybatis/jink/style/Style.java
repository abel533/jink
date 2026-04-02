package io.mybatis.jink.style;

/**
 * 样式定义，对应 ink 的 Styles。
 * 使用不可变 record 模式，通过 Builder 构建。
 * 包含所有 Flexbox 布局属性和文本样式属性。
 */
public record Style(
        // Flex 容器属性
        FlexDirection flexDirection,
        JustifyContent justifyContent,
        AlignItems alignItems,
        AlignItems alignSelf,
        FlexWrap flexWrap,

        // Flex 项目属性
        int flexGrow,
        int flexShrink,
        int flexBasis,

        // 尺寸
        int width,
        int height,
        int minWidth,
        int minHeight,
        int maxWidth,
        int maxHeight,

        // 定位
        Position position,

        // 内边距
        int paddingTop,
        int paddingRight,
        int paddingBottom,
        int paddingLeft,

        // 外边距
        int marginTop,
        int marginRight,
        int marginBottom,
        int marginLeft,

        // 间距
        int gap,
        int columnGap,
        int rowGap,

        // 边框
        BorderStyle borderStyle,
        Color borderColor,

        // 显示
        Display display,
        Overflow overflow,
        Overflow overflowX,
        Overflow overflowY,

        // 文本
        TextWrap textWrap,
        Color color,
        Color backgroundColor,
        boolean bold,
        boolean italic,
        boolean underline,
        boolean strikethrough,
        boolean inverse,
        boolean dimmed
) {
    /** 使用 -1 表示 "auto"（未设置） */
    public static final int AUTO = -1;

    /** 空样式，所有属性为默认值 */
    public static final Style EMPTY = Style.builder().build();

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 基于当前样式创建新 Builder（用于修改部分属性）
     */
    public Builder toBuilder() {
        Builder b = new Builder();
        b.flexDirection = this.flexDirection;
        b.justifyContent = this.justifyContent;
        b.alignItems = this.alignItems;
        b.alignSelf = this.alignSelf;
        b.flexWrap = this.flexWrap;
        b.flexGrow = this.flexGrow;
        b.flexShrink = this.flexShrink;
        b.flexBasis = this.flexBasis;
        b.width = this.width;
        b.height = this.height;
        b.minWidth = this.minWidth;
        b.minHeight = this.minHeight;
        b.maxWidth = this.maxWidth;
        b.maxHeight = this.maxHeight;
        b.position = this.position;
        b.paddingTop = this.paddingTop;
        b.paddingRight = this.paddingRight;
        b.paddingBottom = this.paddingBottom;
        b.paddingLeft = this.paddingLeft;
        b.marginTop = this.marginTop;
        b.marginRight = this.marginRight;
        b.marginBottom = this.marginBottom;
        b.marginLeft = this.marginLeft;
        b.gap = this.gap;
        b.columnGap = this.columnGap;
        b.rowGap = this.rowGap;
        b.borderStyle = this.borderStyle;
        b.borderColor = this.borderColor;
        b.display = this.display;
        b.overflow = this.overflow;
        b.overflowX = this.overflowX;
        b.overflowY = this.overflowY;
        b.textWrap = this.textWrap;
        b.color = this.color;
        b.backgroundColor = this.backgroundColor;
        b.bold = this.bold;
        b.italic = this.italic;
        b.underline = this.underline;
        b.strikethrough = this.strikethrough;
        b.inverse = this.inverse;
        b.dimmed = this.dimmed;
        return b;
    }

    /**
     * 快捷方法：是否有边框
     */
    public boolean hasBorder() {
        return borderStyle != null && borderStyle.hasBorder();
    }

    /**
     * 计算水平方向内边距总和
     */
    public int horizontalPadding() {
        return paddingLeft + paddingRight;
    }

    /**
     * 计算垂直方向内边距总和
     */
    public int verticalPadding() {
        return paddingTop + paddingBottom;
    }

    /**
     * 计算水平方向边框宽度
     */
    public int horizontalBorderWidth() {
        return hasBorder() ? 2 : 0;
    }

    /**
     * 计算垂直方向边框宽度
     */
    public int verticalBorderWidth() {
        return hasBorder() ? 2 : 0;
    }

    /**
     * 计算水平方向外边距总和
     */
    public int horizontalMargin() {
        return marginLeft + marginRight;
    }

    /**
     * 计算垂直方向外边距总和
     */
    public int verticalMargin() {
        return marginTop + marginBottom;
    }

    // ===== Flex 换行 =====

    public enum FlexWrap {
        NO_WRAP,
        WRAP,
        WRAP_REVERSE
    }

    // ===== Builder =====

    public static class Builder {
        private FlexDirection flexDirection = FlexDirection.ROW;
        private JustifyContent justifyContent = JustifyContent.FLEX_START;
        private AlignItems alignItems = AlignItems.STRETCH;
        private AlignItems alignSelf = null;
        private FlexWrap flexWrap = FlexWrap.NO_WRAP;
        private int flexGrow = 0;
        private int flexShrink = 1;
        private int flexBasis = AUTO;
        private int width = AUTO;
        private int height = AUTO;
        private int minWidth = AUTO;
        private int minHeight = AUTO;
        private int maxWidth = AUTO;
        private int maxHeight = AUTO;
        private Position position = Position.RELATIVE;
        private int paddingTop = 0;
        private int paddingRight = 0;
        private int paddingBottom = 0;
        private int paddingLeft = 0;
        private int marginTop = 0;
        private int marginRight = 0;
        private int marginBottom = 0;
        private int marginLeft = 0;
        private int gap = 0;
        private int columnGap = AUTO;
        private int rowGap = AUTO;
        private BorderStyle borderStyle = BorderStyle.NONE;
        private Color borderColor = null;
        private Display display = Display.FLEX;
        private Overflow overflow = Overflow.VISIBLE;
        private Overflow overflowX = null;
        private Overflow overflowY = null;
        private TextWrap textWrap = TextWrap.WRAP;
        private Color color = null;
        private Color backgroundColor = null;
        private boolean bold = false;
        private boolean italic = false;
        private boolean underline = false;
        private boolean strikethrough = false;
        private boolean inverse = false;
        private boolean dimmed = false;

        public Builder flexDirection(FlexDirection v) { this.flexDirection = v; return this; }
        public Builder justifyContent(JustifyContent v) { this.justifyContent = v; return this; }
        public Builder alignItems(AlignItems v) { this.alignItems = v; return this; }
        public Builder alignSelf(AlignItems v) { this.alignSelf = v; return this; }
        public Builder flexWrap(FlexWrap v) { this.flexWrap = v; return this; }
        public Builder flexGrow(int v) { this.flexGrow = v; return this; }
        public Builder flexShrink(int v) { this.flexShrink = v; return this; }
        public Builder flexBasis(int v) { this.flexBasis = v; return this; }

        public Builder width(int v) { this.width = v; return this; }
        public Builder height(int v) { this.height = v; return this; }
        public Builder minWidth(int v) { this.minWidth = v; return this; }
        public Builder minHeight(int v) { this.minHeight = v; return this; }
        public Builder maxWidth(int v) { this.maxWidth = v; return this; }
        public Builder maxHeight(int v) { this.maxHeight = v; return this; }

        public Builder position(Position v) { this.position = v; return this; }

        public Builder padding(int all) {
            this.paddingTop = this.paddingRight = this.paddingBottom = this.paddingLeft = all;
            return this;
        }
        public Builder paddingX(int v) { this.paddingLeft = this.paddingRight = v; return this; }
        public Builder paddingY(int v) { this.paddingTop = this.paddingBottom = v; return this; }
        public Builder paddingTop(int v) { this.paddingTop = v; return this; }
        public Builder paddingRight(int v) { this.paddingRight = v; return this; }
        public Builder paddingBottom(int v) { this.paddingBottom = v; return this; }
        public Builder paddingLeft(int v) { this.paddingLeft = v; return this; }

        public Builder margin(int all) {
            this.marginTop = this.marginRight = this.marginBottom = this.marginLeft = all;
            return this;
        }
        public Builder marginX(int v) { this.marginLeft = this.marginRight = v; return this; }
        public Builder marginY(int v) { this.marginTop = this.marginBottom = v; return this; }
        public Builder marginTop(int v) { this.marginTop = v; return this; }
        public Builder marginRight(int v) { this.marginRight = v; return this; }
        public Builder marginBottom(int v) { this.marginBottom = v; return this; }
        public Builder marginLeft(int v) { this.marginLeft = v; return this; }

        public Builder gap(int v) { this.gap = v; return this; }
        public Builder columnGap(int v) { this.columnGap = v; return this; }
        public Builder rowGap(int v) { this.rowGap = v; return this; }

        public Builder borderStyle(BorderStyle v) { this.borderStyle = v; return this; }
        public Builder borderColor(Color v) { this.borderColor = v; return this; }

        public Builder display(Display v) { this.display = v; return this; }
        public Builder overflow(Overflow v) { this.overflow = v; return this; }
        public Builder overflowX(Overflow v) { this.overflowX = v; return this; }
        public Builder overflowY(Overflow v) { this.overflowY = v; return this; }

        public Builder textWrap(TextWrap v) { this.textWrap = v; return this; }
        public Builder color(Color v) { this.color = v; return this; }
        public Builder backgroundColor(Color v) { this.backgroundColor = v; return this; }
        public Builder bold(boolean v) { this.bold = v; return this; }
        public Builder italic(boolean v) { this.italic = v; return this; }
        public Builder underline(boolean v) { this.underline = v; return this; }
        public Builder strikethrough(boolean v) { this.strikethrough = v; return this; }
        public Builder inverse(boolean v) { this.inverse = v; return this; }
        public Builder dimmed(boolean v) { this.dimmed = v; return this; }

        public Style build() {
            return new Style(
                    flexDirection, justifyContent, alignItems, alignSelf, flexWrap,
                    flexGrow, flexShrink, flexBasis,
                    width, height, minWidth, minHeight, maxWidth, maxHeight,
                    position,
                    paddingTop, paddingRight, paddingBottom, paddingLeft,
                    marginTop, marginRight, marginBottom, marginLeft,
                    gap, columnGap, rowGap,
                    borderStyle, borderColor,
                    display, overflow, overflowX, overflowY,
                    textWrap, color, backgroundColor,
                    bold, italic, underline, strikethrough, inverse, dimmed
            );
        }
    }
}
