package io.mybatis.jink.ui;

import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Component;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.input.Key;
import io.mybatis.jink.style.Color;
import io.mybatis.jink.style.FlexDirection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 多选列表组件。
 *
 * <p>↑↓ 移动高亮，Space 切换选中状态，Enter 确认并触发回调。
 *
 * <p>使用示例：
 * <pre>
 * MultiSelect ms = new MultiSelect.Builder()
 *     .option("红色", "red")
 *     .option("绿色", "green", true)   // 初始选中
 *     .option("蓝色", "blue")
 *     .onChange(values -> System.out.println("已选: " + values))
 *     .build();
 * </pre>
 */
public class MultiSelect extends Component<MultiSelect.State> {

    // ── 颜色常量 ───────────────────────────────────────────────────────────────

    private static final Color C_FOCUS_ARROW   = Color.ansi256(51);   // 聚焦箭头（青蓝）
    private static final Color C_CHECK_ON      = Color.ansi256(46);   // 已选中 [x]（绿）
    private static final Color C_CHECK_OFF     = Color.ansi256(240);  // 未选中 [ ]（灰）
    private static final Color C_FOCUSED_LABEL = Color.ansi256(255);  // 高亮行标签（白/粗）
    private static final Color C_NORMAL_LABEL  = Color.ansi256(252);  // 普通行标签（浅灰）
    private static final Color C_ARROW         = Color.ansi256(240);  // 滚动箭头（灰）

    // ── 选项 ───────────────────────────────────────────────────────────────────

    /** 选项数据 */
    public static final class Option {
        public final String  label;
        public final String  value;
        public final boolean initialSelected;

        public Option(String label, String value, boolean initialSelected) {
            this.label           = label;
            this.value           = value;
            this.initialSelected = initialSelected;
        }
    }

    // ── 状态 ───────────────────────────────────────────────────────────────────

    public static final class State {
        /** 当前高亮行索引 */
        public final int      cursor;
        /** 滚动偏移 */
        public final int      scrollOffset;
        /** 每个选项的选中状态 */
        public final boolean[] selected;
        /** 是否处于聚焦状态 */
        public final boolean   focused;

        public State(int cursor, int scrollOffset, boolean[] selected, boolean focused) {
            this.cursor       = cursor;
            this.scrollOffset = scrollOffset;
            this.selected     = selected;
            this.focused      = focused;
        }
    }

    // ── 配置 ───────────────────────────────────────────────────────────────────

    private final List<Option>    options;
    private final int             visibleRows;
    private final ListCallback    onChange;

    // ── 构造 ───────────────────────────────────────────────────────────────────

    private MultiSelect(Builder builder) {
        super(initState(builder));
        this.options     = builder.options;
        this.visibleRows = builder.visibleRows;
        this.onChange    = builder.onChange;
    }

    private static State initState(Builder b) {
        boolean[] selected = new boolean[b.options.size()];
        for (int i = 0; i < b.options.size(); i++) {
            selected[i] = b.options.get(i).initialSelected;
        }
        return new State(0, 0, selected, false);
    }

    // ── 公开 API ───────────────────────────────────────────────────────────────

    public void setFocused(boolean focused) {
        State s = getState();
        if (s.focused != focused) {
            setState(new State(s.cursor, s.scrollOffset, s.selected, focused));
        }
    }

    public boolean isFocused() {
        return getState().focused;
    }

    /** 获取当前已选中项的 value 列表 */
    public List<String> getSelectedValues() {
        State s = getState();
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < options.size(); i++) {
            if (i < s.selected.length && s.selected[i]) {
                result.add(options.get(i).value);
            }
        }
        return Collections.unmodifiableList(result);
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
            Option opt      = options.get(i);
            boolean isCursor = (i == s.cursor);
            boolean isChosen = (i < s.selected.length && s.selected[i]);

            String arrow = isCursor ? "> " : "  ";
            String check = isChosen  ? "[x] " : "[ ] ";

            container.add(Box.of(
                    Text.of(arrow).color(isCursor ? C_FOCUS_ARROW : C_CHECK_OFF),
                    Text.of(check).color(isChosen ? C_CHECK_ON : C_CHECK_OFF),
                    Text.of(opt.label).color(isCursor ? C_FOCUSED_LABEL : C_NORMAL_LABEL)
                            .bold()
            ));
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
            setState(adjustScroll(newCursor, s.scrollOffset, s.selected));
            return;
        }
        if (key.downArrow()) {
            int newCursor = (s.cursor + 1) % total;
            setState(adjustScroll(newCursor, s.scrollOffset, s.selected));
            return;
        }
        // Space：切换当前项的选中状态
        if (" ".equals(input)) {
            boolean[] newSelected = copySelected(s.selected);
            if (s.cursor < newSelected.length) {
                newSelected[s.cursor] = !newSelected[s.cursor];
            }
            setState(new State(s.cursor, s.scrollOffset, newSelected, s.focused));
            return;
        }
        // Enter：确认，触发回调
        if (key.return_()) {
            if (onChange != null) {
                onChange.accept(getSelectedValues());
            }
        }
    }

    private boolean[] copySelected(boolean[] original) {
        boolean[] copy = new boolean[original.length];
        System.arraycopy(original, 0, copy, 0, original.length);
        return copy;
    }

    private State adjustScroll(int cursor, int scrollOffset, boolean[] selected) {
        if (cursor < scrollOffset) {
            scrollOffset = cursor;
        } else if (cursor >= scrollOffset + visibleRows) {
            scrollOffset = cursor - visibleRows + 1;
        }
        return new State(cursor, scrollOffset, selected, getState().focused);
    }

    // ── Builder ────────────────────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        final List<Option> options     = new ArrayList<Option>();
        int                visibleRows = 5;
        ListCallback       onChange;

        public Builder option(String label, String value) {
            return option(label, value, false);
        }

        public Builder option(String label, String value, boolean initialSelected) {
            options.add(new Option(label, value, initialSelected));
            return this;
        }

        public Builder visibleRows(int rows) {
            this.visibleRows = rows;
            return this;
        }

        public Builder onChange(ListCallback onChange) {
            this.onChange = onChange;
            return this;
        }

        public MultiSelect build() {
            return new MultiSelect(this);
        }
    }

    /** 列表回调 */
    @FunctionalInterface
    public interface ListCallback {
        void accept(List<String> selectedValues);
    }
}
