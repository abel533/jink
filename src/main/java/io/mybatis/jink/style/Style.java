package io.mybatis.jink.style;

/**
 * 样式定义，对应 ink 的 Styles。
 * 使用不可变类模式，通过 Builder 构建。
 * 包含所有 Flexbox 布局属性和文本样式属性。
 */
public final class Style {
    // Flex 容器属性
    private final FlexDirection flexDirection;
    private final JustifyContent justifyContent;
    private final AlignItems alignItems;
    private final AlignItems alignSelf;
    private final AlignContent alignContent;
    private final FlexWrap flexWrap;

    // Flex 项目属性
    private final int flexGrow;
    private final int flexShrink;
    private final int flexBasis;

    // 尺寸
    private final int width;
    private final int height;
    private final int minWidth;
    private final int minHeight;
    private final int maxWidth;
    private final int maxHeight;
    private final float aspectRatio;

    // 定位
    private final Position position;
    private final int posTop;
    private final int posRight;
    private final int posBottom;
    private final int posLeft;

    // 内边距
    private final int paddingTop;
    private final int paddingRight;
    private final int paddingBottom;
    private final int paddingLeft;

    // 外边距
    private final int marginTop;
    private final int marginRight;
    private final int marginBottom;
    private final int marginLeft;

    // 间距
    private final int gap;
    private final int columnGap;
    private final int rowGap;

    // 边框
    private final BorderStyle borderStyle;
    private final Color borderColor;
    private final Color borderTopColor;
    private final Color borderRightColor;
    private final Color borderBottomColor;
    private final Color borderLeftColor;
    private final boolean borderDimColor;
    private final boolean borderTopDimColor;
    private final boolean borderRightDimColor;
    private final boolean borderBottomDimColor;
    private final boolean borderLeftDimColor;

    // 显示
    private final Display display;
    private final Overflow overflow;
    private final Overflow overflowX;
    private final Overflow overflowY;

    // 文本
    private final TextWrap textWrap;
    private final Color color;
    private final Color backgroundColor;
    private final boolean bold;
    private final boolean italic;
    private final boolean underline;
    private final boolean strikethrough;
    private final boolean inverse;
    private final boolean dimmed;

    // 全参构造器（由 Builder 调用）
    private Style(Builder b) {
        this.flexDirection = b.flexDirection;
        this.justifyContent = b.justifyContent;
        this.alignItems = b.alignItems;
        this.alignSelf = b.alignSelf;
        this.alignContent = b.alignContent;
        this.flexWrap = b.flexWrap;
        this.flexGrow = b.flexGrow;
        this.flexShrink = b.flexShrink;
        this.flexBasis = b.flexBasis;
        this.width = b.width;
        this.height = b.height;
        this.minWidth = b.minWidth;
        this.minHeight = b.minHeight;
        this.maxWidth = b.maxWidth;
        this.maxHeight = b.maxHeight;
        this.aspectRatio = b.aspectRatio;
        this.position = b.position;
        this.posTop = b.posTop;
        this.posRight = b.posRight;
        this.posBottom = b.posBottom;
        this.posLeft = b.posLeft;
        this.paddingTop = b.paddingTop;
        this.paddingRight = b.paddingRight;
        this.paddingBottom = b.paddingBottom;
        this.paddingLeft = b.paddingLeft;
        this.marginTop = b.marginTop;
        this.marginRight = b.marginRight;
        this.marginBottom = b.marginBottom;
        this.marginLeft = b.marginLeft;
        this.gap = b.gap;
        this.columnGap = b.columnGap;
        this.rowGap = b.rowGap;
        this.borderStyle = b.borderStyle;
        this.borderColor = b.borderColor;
        this.borderTopColor = b.borderTopColor;
        this.borderRightColor = b.borderRightColor;
        this.borderBottomColor = b.borderBottomColor;
        this.borderLeftColor = b.borderLeftColor;
        this.borderDimColor = b.borderDimColor;
        this.borderTopDimColor = b.borderTopDimColor;
        this.borderRightDimColor = b.borderRightDimColor;
        this.borderBottomDimColor = b.borderBottomDimColor;
        this.borderLeftDimColor = b.borderLeftDimColor;
        this.display = b.display;
        this.overflow = b.overflow;
        this.overflowX = b.overflowX;
        this.overflowY = b.overflowY;
        this.textWrap = b.textWrap;
        this.color = b.color;
        this.backgroundColor = b.backgroundColor;
        this.bold = b.bold;
        this.italic = b.italic;
        this.underline = b.underline;
        this.strikethrough = b.strikethrough;
        this.inverse = b.inverse;
        this.dimmed = b.dimmed;
    }

    // ===== Getters =====

    public FlexDirection flexDirection()   { return flexDirection; }
    public JustifyContent justifyContent() { return justifyContent; }
    public AlignItems alignItems()         { return alignItems; }
    public AlignItems alignSelf()          { return alignSelf; }
    public AlignContent alignContent()     { return alignContent; }
    public FlexWrap flexWrap()             { return flexWrap; }
    public int flexGrow()                  { return flexGrow; }
    public int flexShrink()                { return flexShrink; }
    public int flexBasis()                 { return flexBasis; }
    public int width()                     { return width; }
    public int height()                    { return height; }
    public int minWidth()                  { return minWidth; }
    public int minHeight()                 { return minHeight; }
    public int maxWidth()                  { return maxWidth; }
    public int maxHeight()                 { return maxHeight; }
    public float aspectRatio()             { return aspectRatio; }
    public Position position()             { return position; }
    public int posTop()                    { return posTop; }
    public int posRight()                  { return posRight; }
    public int posBottom()                 { return posBottom; }
    public int posLeft()                   { return posLeft; }
    public int paddingTop()                { return paddingTop; }
    public int paddingRight()              { return paddingRight; }
    public int paddingBottom()             { return paddingBottom; }
    public int paddingLeft()               { return paddingLeft; }
    public int marginTop()                 { return marginTop; }
    public int marginRight()               { return marginRight; }
    public int marginBottom()              { return marginBottom; }
    public int marginLeft()                { return marginLeft; }
    public int gap()                       { return gap; }
    public int columnGap()                 { return columnGap; }
    public int rowGap()                    { return rowGap; }
    public BorderStyle borderStyle()       { return borderStyle; }
    public Color borderColor()             { return borderColor; }
    public Color borderTopColor()          { return borderTopColor; }
    public Color borderRightColor()        { return borderRightColor; }
    public Color borderBottomColor()       { return borderBottomColor; }
    public Color borderLeftColor()         { return borderLeftColor; }
    public boolean borderDimColor()        { return borderDimColor; }
    public boolean borderTopDimColor()     { return borderTopDimColor; }
    public boolean borderRightDimColor()   { return borderRightDimColor; }
    public boolean borderBottomDimColor()  { return borderBottomDimColor; }
    public boolean borderLeftDimColor()    { return borderLeftDimColor; }
    public Display display()               { return display; }
    public Overflow overflow()             { return overflow; }
    public Overflow overflowX()            { return overflowX; }
    public Overflow overflowY()            { return overflowY; }
    public TextWrap textWrap()             { return textWrap; }
    public Color color()                   { return color; }
    public Color backgroundColor()         { return backgroundColor; }
    public boolean bold()                  { return bold; }
    public boolean italic()                { return italic; }
    public boolean underline()             { return underline; }
    public boolean strikethrough()         { return strikethrough; }
    public boolean inverse()               { return inverse; }
    public boolean dimmed()                { return dimmed; }
    /** 使用 -1 表示 "auto"（未设置） */
    public static final int AUTO = -1;

    /**
     * 将百分比值编码为 int。50% → percent(50) = -51。
     * 编码规则：-(pct + 1)，与 AUTO(-1) 不冲突。
     */
    public static int percent(int pct) {
        if (pct < 0 || pct > 100) throw new IllegalArgumentException("百分比须在 0-100 之间: " + pct);
        return -(pct + 1);
    }

    /** 判断值是否为百分比编码 */
    public static boolean isPercent(int value) {
        return value <= -2;
    }

    /** 从编码中解出百分比数值（0-100） */
    public static int getPercent(int value) {
        return -(value + 1);
    }

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
        b.alignContent = this.alignContent;
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
        b.aspectRatio = this.aspectRatio;
        b.position = this.position;
        b.posTop = this.posTop;
        b.posRight = this.posRight;
        b.posBottom = this.posBottom;
        b.posLeft = this.posLeft;
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
        b.borderTopColor = this.borderTopColor;
        b.borderRightColor = this.borderRightColor;
        b.borderBottomColor = this.borderBottomColor;
        b.borderLeftColor = this.borderLeftColor;
        b.borderDimColor = this.borderDimColor;
        b.borderTopDimColor = this.borderTopDimColor;
        b.borderRightDimColor = this.borderRightDimColor;
        b.borderBottomDimColor = this.borderBottomDimColor;
        b.borderLeftDimColor = this.borderLeftDimColor;
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

    /** 获取某边的有效边框颜色（优先使用独立颜色，回退到 borderColor） */
    public Color effectiveBorderTopColor() {
        return borderTopColor != null ? borderTopColor : borderColor;
    }
    public Color effectiveBorderRightColor() {
        return borderRightColor != null ? borderRightColor : borderColor;
    }
    public Color effectiveBorderBottomColor() {
        return borderBottomColor != null ? borderBottomColor : borderColor;
    }
    public Color effectiveBorderLeftColor() {
        return borderLeftColor != null ? borderLeftColor : borderColor;
    }

    /** 获取某边的有效边框 dimmed 状态（独立 dim 或全局 borderDimColor） */
    public boolean effectiveBorderTopDimmed() {
        return borderTopDimColor || borderDimColor;
    }
    public boolean effectiveBorderRightDimmed() {
        return borderRightDimColor || borderDimColor;
    }
    public boolean effectiveBorderBottomDimmed() {
        return borderBottomDimColor || borderDimColor;
    }
    public boolean effectiveBorderLeftDimmed() {
        return borderLeftDimColor || borderDimColor;
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
        private AlignContent alignContent = AlignContent.FLEX_START;
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
        private float aspectRatio = 0;
        private Position position = Position.RELATIVE;
        private int posTop = AUTO;
        private int posRight = AUTO;
        private int posBottom = AUTO;
        private int posLeft = AUTO;
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
        private Color borderTopColor = null;
        private Color borderRightColor = null;
        private Color borderBottomColor = null;
        private Color borderLeftColor = null;
        private boolean borderDimColor = false;
        private boolean borderTopDimColor = false;
        private boolean borderRightDimColor = false;
        private boolean borderBottomDimColor = false;
        private boolean borderLeftDimColor = false;
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
        public Builder alignContent(AlignContent v) { this.alignContent = v; return this; }
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
        /** 宽高比约束（width / height）。0 表示不设置。 */
        public Builder aspectRatio(float v) { this.aspectRatio = v; return this; }

        /** 百分比宽度，如 widthPercent(50) 表示 50% */
        public Builder widthPercent(int pct) { this.width = percent(pct); return this; }
        /** 百分比高度 */
        public Builder heightPercent(int pct) { this.height = percent(pct); return this; }
        /** 百分比最小宽度 */
        public Builder minWidthPercent(int pct) { this.minWidth = percent(pct); return this; }
        /** 百分比最小高度 */
        public Builder minHeightPercent(int pct) { this.minHeight = percent(pct); return this; }
        /** 百分比最大宽度 */
        public Builder maxWidthPercent(int pct) { this.maxWidth = percent(pct); return this; }
        /** 百分比最大高度 */
        public Builder maxHeightPercent(int pct) { this.maxHeight = percent(pct); return this; }
        /** 百分比 flexBasis */
        public Builder flexBasisPercent(int pct) { this.flexBasis = percent(pct); return this; }

        public Builder position(Position v) { this.position = v; return this; }
        public Builder posTop(int v) { this.posTop = v; return this; }
        public Builder posRight(int v) { this.posRight = v; return this; }
        public Builder posBottom(int v) { this.posBottom = v; return this; }
        public Builder posLeft(int v) { this.posLeft = v; return this; }

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
        public Builder borderTopColor(Color v) { this.borderTopColor = v; return this; }
        public Builder borderRightColor(Color v) { this.borderRightColor = v; return this; }
        public Builder borderBottomColor(Color v) { this.borderBottomColor = v; return this; }
        public Builder borderLeftColor(Color v) { this.borderLeftColor = v; return this; }
        public Builder borderDimColor(boolean v) { this.borderDimColor = v; return this; }
        public Builder borderTopDimColor(boolean v) { this.borderTopDimColor = v; return this; }
        public Builder borderRightDimColor(boolean v) { this.borderRightDimColor = v; return this; }
        public Builder borderBottomDimColor(boolean v) { this.borderBottomDimColor = v; return this; }
        public Builder borderLeftDimColor(boolean v) { this.borderLeftDimColor = v; return this; }

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
            return new Style(this);
        }
    }
}
