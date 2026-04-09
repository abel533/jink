package io.mybatis.jink.ui;

import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Component;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.input.Key;
import io.mybatis.jink.style.Color;
import io.mybatis.jink.style.FlexDirection;

import java.util.ArrayList;
import java.util.List;

/**
 * 单选列表组件。
 *
 * <p>↑↓ 移动高亮，Enter 选中并触发回调，支持超出可视行数时滚动。
 *
 * <p>使用示例：
 * <pre>
 * Select select = new Select.Builder()
 *     .option("红色", "red")
 *     .option("绿色", "green")
 *     .option("蓝色", "blue")
 *     .visibleRows(5)
 *     .onChange(value -> System.out.println("选中: " + value))
 *     .build();
 * </pre>
 */
public class Select extends Component<Select.State> {

    // ── 颜色常量 ───────────────────────────────────────────────────────────────

    private static final Color C_FOCUS_FG    = Color.ansi256(0);   // 高亮行前景（黑）
    private static final Color C_FOCUS_BG    = Color.ansi256(51);  // 高亮行背景（青蓝）
    private static final Color C_NORMAL_FG   = Color.ansi256(252); // 普通行前景（浅灰）
    private static final Color C_ARROW       = Color.ansi256(240); // 滚动箭头（灰）

    // ── 选项 ───────────────────────────────────────────────────────────────────

    /** 选项数据 */
    public static final class Option {
        public final String label;
        public final String value;

        public Option(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    // ── 状态 ───────────────────────────────────────────────────────────────────

    public static final class State {
        /** 当前高亮行索引（0-based） */
        public final int     cursor;
        /** 滚动偏移（第一个可见行的索引） */
        public final int     scrollOffset;
        /** 是否处于聚焦状态 */
        public final boolean focused;

        public State(int cursor, int scrollOffset, boolean focused) {
            this.cursor       = cursor;
            this.scrollOffset = scrollOffset;
            this.focused      = focused;
        }
    }

    // ── 配置 ───────────────────────────────────────────────────────────────────

    private final List<Option>           options;
    private final int                    visibleRows;
    private final StringCallback         onChange;

    // ── 构造 ───────────────────────────────────────────────────────────────────

    private Select(Builder builder) {
        super(new State(0, 0, false));
        this.options     = builder.options;
        this.visibleRows = builder.visibleRows;
        this.onChange    = builder.onChange;
    }

    // ── 公开 API ───────────────────────────────────────────────────────────────

    public void setFocused(boolean focused) {
        State s = getState();
        if (s.focused != focused) {
            setState(new State(s.cursor, s.scrollOffset, focused));
        }
    }

    public boolean isFocused() {
        return getState().focused;
    }

    /** 获取当前高亮项的 value（未选中时返回 null） */
    public String getCurrentValue() {
        State s = getState();
        if (s.cursor >= 0 && s.cursor < options.size()) {
            return options.get(s.cursor).value;
        }
        return null;
    }

    // ── 生命周期 ───────────────────────────────────────────────────────────────

    @Override public void onMount()   {}
    @Override public void onUnmount() {}

    // ── 渲染 ───────────────────────────────────────────────────────────────────

    @Override
    public Renderable render() {
        State s = getState();
        int total   = options.size();
        int visible = Math.min(visibleRows, total);
        int start   = s.scrollOffset;
        int end     = Math.min(start + visible, total);

        Box container = Box.of().flexDirection(FlexDirection.COLUMN);

        // 向上滚动提示
        if (start > 0) {
            container.add(Text.of("  ^ ").color(C_ARROW));
        }

        for (int i = start; i < end; i++) {
            Option opt = options.get(i);
            boolean focused = (i == s.cursor);
            if (focused) {
                container.add(Box.of(
                        Text.of(" > " + opt.label + " ")
                                .color(C_FOCUS_FG)
                                .backgroundColor(C_FOCUS_BG)
                ));
            } else {
                container.add(Box.of(
                        Text.of("   " + opt.label + " ").color(C_NORMAL_FG)
                ));
            }
        }

        // 向下滚动提示
        if (end < total) {
            container.add(Text.of("  v ").color(C_ARROW));
        }

        return container;
    }

    // ── 键盘输入 ───────────────────────────────────────────────────────────────

    @Override
    public void onInput(String input, Key key) {
        State s = getState();
        if (!s.focused) return;

        int total = options.size();
        if (total == 0) return;

        if (key.upArrow()) {
            int newCursor = (s.cursor - 1 + total) % total;
            setState(adjustScroll(newCursor, s.scrollOffset));
            return;
        }
        if (key.downArrow()) {
            int newCursor = (s.cursor + 1) % total;
            setState(adjustScroll(newCursor, s.scrollOffset));
            return;
        }
        if (key.return_()) {
            if (onChange != null && s.cursor < total) {
                onChange.accept(options.get(s.cursor).value);
            }
            return;
        }
    }

    /** 调整滚动偏移以确保 cursor 可见 */
    private State adjustScroll(int cursor, int scrollOffset) {
        if (cursor < scrollOffset) {
            scrollOffset = cursor;
        } else if (cursor >= scrollOffset + visibleRows) {
            scrollOffset = cursor - visibleRows + 1;
        }
        return new State(cursor, scrollOffset, getState().focused);
    }

    // ── Builder ────────────────────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        final List<Option> options     = new ArrayList<Option>();
        int                visibleRows = 5;
        StringCallback     onChange;

        public Builder option(String label, String value) {
            options.add(new Option(label, value));
            return this;
        }

        public Builder visibleRows(int rows) {
            this.visibleRows = rows;
            return this;
        }

        public Builder onChange(StringCallback onChange) {
            this.onChange = onChange;
            return this;
        }

        public Select build() {
            return new Select(this);
        }
    }

    /** 字符串回调 */
    @FunctionalInterface
    public interface StringCallback {
        void accept(String value);
    }
}
