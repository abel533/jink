package io.mybatis.jink.input;

/**
 * 键盘按键事件。
 * 对应 ink 的 Key 接口。
 */
public final class Key {

    private final boolean upArrow;
    private final boolean downArrow;
    private final boolean leftArrow;
    private final boolean rightArrow;
    private final boolean pageUp;
    private final boolean pageDown;
    private final boolean home;
    private final boolean end;
    private final boolean return_;
    private final boolean escape;
    private final boolean tab;
    private final boolean backspace;
    private final boolean delete;
    private final boolean ctrl;
    private final boolean shift;
    private final boolean meta;
    private final boolean scrollUp;
    private final boolean scrollDown;

    public Key(boolean upArrow, boolean downArrow, boolean leftArrow, boolean rightArrow,
               boolean pageUp, boolean pageDown, boolean home, boolean end,
               boolean return_, boolean escape, boolean tab, boolean backspace, boolean delete,
               boolean ctrl, boolean shift, boolean meta, boolean scrollUp, boolean scrollDown) {
        this.upArrow = upArrow;
        this.downArrow = downArrow;
        this.leftArrow = leftArrow;
        this.rightArrow = rightArrow;
        this.pageUp = pageUp;
        this.pageDown = pageDown;
        this.home = home;
        this.end = end;
        this.return_ = return_;
        this.escape = escape;
        this.tab = tab;
        this.backspace = backspace;
        this.delete = delete;
        this.ctrl = ctrl;
        this.shift = shift;
        this.meta = meta;
        this.scrollUp = scrollUp;
        this.scrollDown = scrollDown;
    }

    public boolean upArrow()    { return upArrow; }
    public boolean downArrow()  { return downArrow; }
    public boolean leftArrow()  { return leftArrow; }
    public boolean rightArrow() { return rightArrow; }
    public boolean pageUp()     { return pageUp; }
    public boolean pageDown()   { return pageDown; }
    public boolean home()       { return home; }
    public boolean end()        { return end; }
    public boolean return_()    { return return_; }
    public boolean escape()     { return escape; }
    public boolean tab()        { return tab; }
    public boolean backspace()  { return backspace; }
    public boolean delete()     { return delete; }
    public boolean ctrl()       { return ctrl; }
    public boolean shift()      { return shift; }
    public boolean meta()       { return meta; }
    public boolean scrollUp()   { return scrollUp; }
    public boolean scrollDown() { return scrollDown; }

    /**
     * 创建普通字符按键（无特殊键和修饰符）
     */
    public static Key plain() {
        return new Key(false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false);
    }

    /**
     * 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean upArrow, downArrow, leftArrow, rightArrow;
        private boolean pageUp, pageDown, home, end;
        private boolean return_, escape, tab, backspace, delete;
        private boolean ctrl, shift, meta;
        private boolean scrollUp, scrollDown;

        public Builder upArrow() { this.upArrow = true; return this; }
        public Builder downArrow() { this.downArrow = true; return this; }
        public Builder leftArrow() { this.leftArrow = true; return this; }
        public Builder rightArrow() { this.rightArrow = true; return this; }
        public Builder pageUp() { this.pageUp = true; return this; }
        public Builder pageDown() { this.pageDown = true; return this; }
        public Builder home() { this.home = true; return this; }
        public Builder end() { this.end = true; return this; }
        public Builder return_() { this.return_ = true; return this; }
        public Builder escape() { this.escape = true; return this; }
        public Builder tab() { this.tab = true; return this; }
        public Builder backspace() { this.backspace = true; return this; }
        public Builder delete() { this.delete = true; return this; }
        public Builder ctrl() { this.ctrl = true; return this; }
        public Builder shift() { this.shift = true; return this; }
        public Builder meta() { this.meta = true; return this; }
        public Builder scrollUp() { this.scrollUp = true; return this; }
        public Builder scrollDown() { this.scrollDown = true; return this; }

        public Key build() {
            return new Key(upArrow, downArrow, leftArrow, rightArrow,
                    pageUp, pageDown, home, end,
                    return_, escape, tab, backspace, delete,
                    ctrl, shift, meta, scrollUp, scrollDown);
        }
    }
}
