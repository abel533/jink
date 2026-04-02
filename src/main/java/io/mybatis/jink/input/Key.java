package io.mybatis.jink.input;

/**
 * 键盘按键事件。
 * 对应 ink 的 Key 接口。
 */
public record Key(
        boolean upArrow,
        boolean downArrow,
        boolean leftArrow,
        boolean rightArrow,
        boolean pageUp,
        boolean pageDown,
        boolean home,
        boolean end,
        boolean return_,
        boolean escape,
        boolean tab,
        boolean backspace,
        boolean delete,
        boolean ctrl,
        boolean shift,
        boolean meta
) {

    /**
     * 创建普通字符按键（无特殊键和修饰符）
     */
    public static Key plain() {
        return new Key(false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false);
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

        public Key build() {
            return new Key(upArrow, downArrow, leftArrow, rightArrow,
                    pageUp, pageDown, home, end,
                    return_, escape, tab, backspace, delete,
                    ctrl, shift, meta);
        }
    }
}
