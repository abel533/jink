package io.mybatis.jink.ui;

import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Component;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.input.Key;
import io.mybatis.jink.style.BorderStyle;
import io.mybatis.jink.style.Color;
import io.mybatis.jink.util.StringWidth;

import java.util.Arrays;

/**
 * 单行文本输入组件。
 *
 * <p>提供完整的终端文本编辑体验：
 * <ul>
 *   <li>← → 移动光标（按码点，正确处理 CJK/emoji）</li>
 *   <li>Home/End 跳到行首/尾</li>
 *   <li>Backspace 删除光标前字符，Delete 删除光标处字符</li>
 *   <li>字符输入在光标位置插入</li>
 *   <li>{@link Component#setCursorPosition} 精确定位光标</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>
 * TextInput input = new TextInput.Builder()
 *     .placeholder("请输入姓名")
 *     .maxLength(20)
 *     .onChange(value -> System.out.println("当前值: " + value))
 *     .onSubmit(value -> System.out.println("提交: " + value))
 *     .build();
 * </pre>
 */
public class TextInput extends Component<TextInput.State> {

    // ── 颜色常量 ───────────────────────────────────────────────────────────────

    private static final Color C_VALUE      = Color.ansi256(255);  // 已输入值（白）
    private static final Color C_HINT       = Color.ansi256(238);  // 占位提示（暗灰）
    private static final Color C_FOCUS_BOX  = Color.ansi256(51);   // 聚焦边框（青蓝）
    private static final Color C_NORMAL_BOX = Color.ansi256(240);  // 普通边框（灰）

    // ── 状态 ───────────────────────────────────────────────────────────────────

    /** 组件内部状态 */
    public static final class State {
        /** 当前输入值 */
        public final String  value;
        /** 光标字符偏移（0 = 最左，value.length() = 末尾） */
        public final int     cursor;
        /** 是否处于聚焦状态 */
        public final boolean focused;

        public State(String value, int cursor, boolean focused) {
            this.value   = value;
            this.cursor  = cursor;
            this.focused = focused;
        }

        State withValue(String v, int c) {
            return new State(v, c, focused);
        }

        State withCursor(int c) {
            return new State(value, c, focused);
        }

        State withFocused(boolean f) {
            // 聚焦时光标跳到末尾，失焦时保持
            int c = f ? value.length() : cursor;
            return new State(value, c, f);
        }
    }

    // ── 配置 ───────────────────────────────────────────────────────────────────

    private final String   placeholder;
    private final int      maxLength;   // 最大显示列宽，-1 = 无限制
    private final Callback onChange;
    private final Callback onSubmit;
    private final boolean  showBorder;
    private final char     maskChar;    // 0 = 不 mask；其他字符用于 PasswordInput

    // ── 构造 ───────────────────────────────────────────────────────────────────

    protected TextInput(Builder builder) {
        super(new State(
                builder.initialValue,
                builder.initialValue.length(),
                false
        ));
        this.placeholder = builder.placeholder;
        this.maxLength   = builder.maxLength;
        this.onChange    = builder.onChange;
        this.onSubmit    = builder.onSubmit;
        this.showBorder  = builder.showBorder;
        this.maskChar    = builder.maskChar;
    }

    // ── 公开 API ───────────────────────────────────────────────────────────────

    /** 获取当前输入值 */
    public String getValue() {
        return getState().value;
    }

    /** 以编程方式设置值（光标移到末尾） */
    public void setValue(String value) {
        String v = value == null ? "" : value;
        setState(getState().withValue(v, v.length()));
    }

    /** 设置聚焦状态 */
    public void setFocused(boolean focused) {
        State s = getState();
        if (s.focused != focused) {
            setState(s.withFocused(focused));
        }
    }

    public boolean isFocused() {
        return getState().focused;
    }

    // ── 生命周期 ───────────────────────────────────────────────────────────────

    @Override
    public void onMount() {}

    @Override
    public void onUnmount() {}

    /** 公开 State 访问，供父组件读取值与光标位置 */
    @Override
    public State getState() {
        return super.getState();
    }

    // ── 渲染 ───────────────────────────────────────────────────────────────────

    @Override
    public Renderable render() {
        State s = getState();

        // 精确定位光标（仅在聚焦时显示光标）
        if (s.focused) {
            String beforeCursor = s.value.substring(0, s.cursor);
            String displayBefore = getDisplayValue(beforeCursor);
            int borderOffset = showBorder ? 2 : 0; // 圆角边框左侧 + paddingX(1)
            setCursorPosition(showBorder ? 1 : 0, borderOffset + StringWidth.width(displayBefore));
        }

        String displayValue = getDisplayValue(s.value);
        Renderable content = s.value.isEmpty()
                ? Text.of(placeholder).color(C_HINT)
                : Text.of(displayValue).color(C_VALUE);

        if (showBorder) {
            return Box.of(content)
                    .borderStyle(BorderStyle.ROUND)
                    .borderColor(s.focused ? C_FOCUS_BOX : C_NORMAL_BOX)
                    .paddingX(1);
        }
        return content;
    }

    /**
     * 将原始值转换为显示值。
     * PasswordInput 覆盖此方法将字符替换为 maskChar。
     */
    protected String getDisplayValue(String value) {
        if (maskChar == 0 || value.isEmpty()) {
            return value;
        }
        int codePointCount = value.codePointCount(0, value.length());
        char[] masked = new char[codePointCount];
        Arrays.fill(masked, maskChar);
        return new String(masked);
    }

    // ── 键盘输入 ───────────────────────────────────────────────────────────────

    @Override
    public void onInput(String input, Key key) {
        State s = getState();
        if (!s.focused) return;

        // ← →：移动光标（按码点）
        if (key.leftArrow()) {
            if (s.cursor > 0) {
                int prevCp = s.value.codePointBefore(s.cursor);
                setState(s.withCursor(s.cursor - Character.charCount(prevCp)));
            }
            return;
        }
        if (key.rightArrow()) {
            if (s.cursor < s.value.length()) {
                int cp = s.value.codePointAt(s.cursor);
                setState(s.withCursor(s.cursor + Character.charCount(cp)));
            }
            return;
        }

        // Home/End：跳到行首/尾
        if (key.home()) { setState(s.withCursor(0)); return; }
        if (key.end())  { setState(s.withCursor(s.value.length())); return; }

        // Enter：提交
        if (key.return_()) {
            if (onSubmit != null) onSubmit.accept(s.value);
            return;
        }

        // Backspace：删除光标前字符
        if (key.backspace()) {
            if (s.cursor > 0) {
                int prevCp = s.value.codePointBefore(s.cursor);
                int cpCount = Character.charCount(prevCp);
                String nv = s.value.substring(0, s.cursor - cpCount) + s.value.substring(s.cursor);
                setState(s.withValue(nv, s.cursor - cpCount));
                if (onChange != null) onChange.accept(nv);
            }
            return;
        }

        // Delete：删除光标处字符
        if (key.delete()) {
            if (s.cursor < s.value.length()) {
                int cp = s.value.codePointAt(s.cursor);
                int cpCount = Character.charCount(cp);
                String nv = s.value.substring(0, s.cursor) + s.value.substring(s.cursor + cpCount);
                setState(s.withValue(nv, s.cursor));
                if (onChange != null) onChange.accept(nv);
            }
            return;
        }

        // 可见字符：在光标处插入（过滤控制键和导航键）
        if (!key.ctrl() && !key.meta() && !key.escape() && !key.tab()
                && !key.upArrow() && !key.downArrow() && !input.isEmpty()) {
            if (maxLength < 0 || StringWidth.width(s.value) < maxLength) {
                String nv = s.value.substring(0, s.cursor) + input + s.value.substring(s.cursor);
                setState(s.withValue(nv, s.cursor + input.length()));
                if (onChange != null) onChange.accept(nv);
            }
        }
    }

    // ── Builder ────────────────────────────────────────────────────────────────

    /** 创建 Builder */
    public static Builder builder() {
        return new Builder();
    }

    /** TextInput 构建器 */
    public static class Builder {
        String   placeholder  = "";
        int      maxLength    = -1;
        String   initialValue = "";
        Callback onChange;
        Callback onSubmit;
        boolean  showBorder   = false;
        char     maskChar     = 0;

        public Builder placeholder(String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public Builder maxLength(int cols) {
            this.maxLength = cols;
            return this;
        }

        public Builder value(String value) {
            this.initialValue = value == null ? "" : value;
            return this;
        }

        public Builder onChange(Callback onChange) {
            this.onChange = onChange;
            return this;
        }

        public Builder onSubmit(Callback onSubmit) {
            this.onSubmit = onSubmit;
            return this;
        }

        public Builder showBorder(boolean show) {
            this.showBorder = show;
            return this;
        }

        /** 仅供 PasswordInput.Builder 调用 */
        Builder maskChar(char c) {
            this.maskChar = c;
            return this;
        }

        /** 构建 TextInput */
        public TextInput build() {
            return new TextInput(this);
        }
    }

    // ── 回调接口（Java 8 兼容）─────────────────────────────────────────────────

    /** 字符串回调接口（避免直接依赖 java.util.function.Consumer） */
    @FunctionalInterface
    public interface Callback {
        void accept(String value);
    }
}
