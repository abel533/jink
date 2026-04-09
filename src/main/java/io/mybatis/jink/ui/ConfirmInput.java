package io.mybatis.jink.ui;

import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Component;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.input.Key;
import io.mybatis.jink.style.Color;

/**
 * Y/n 确认输入组件。
 *
 * <p>显示一个常见的确认/取消选择，y/Y 表示确认，n/N 表示取消，Enter 执行默认选项。
 *
 * <p>使用示例：
 * <pre>
 * ConfirmInput confirm = new ConfirmInput.Builder()
 *     .defaultConfirm(true)   // 默认是 Y
 *     .onConfirm(() -> System.out.println("已确认"))
 *     .onCancel(() -> System.out.println("已取消"))
 *     .build();
 * </pre>
 *
 * <p>渲染效果：{@code (Y/n)}（默认确认）或 {@code (y/N)}（默认取消）
 */
public class ConfirmInput extends Component<ConfirmInput.State> {

    // ── 颜色常量 ───────────────────────────────────────────────────────────────

    private static final Color C_YES     = Color.ansi256(46);   // Y（绿）
    private static final Color C_NO      = Color.ansi256(196);  // N（红）
    private static final Color C_SEP     = Color.ansi256(240);  // 分隔符（灰）
    private static final Color C_DEFAULT = Color.ansi256(255);  // 默认选项（白/粗）
    private static final Color C_OTHER   = Color.ansi256(245);  // 非默认选项（灰）

    // ── 状态 ───────────────────────────────────────────────────────────────────

    public static final class State {
        public final boolean focused;

        public State(boolean focused) {
            this.focused = focused;
        }
    }

    // ── 配置 ───────────────────────────────────────────────────────────────────

    private final boolean    defaultConfirm; // true = Y 为默认，false = N 为默认
    private final Runnable   onConfirm;
    private final Runnable   onCancel;

    // ── 构造 ───────────────────────────────────────────────────────────────────

    private ConfirmInput(Builder builder) {
        super(new State(false));
        this.defaultConfirm = builder.defaultConfirm;
        this.onConfirm      = builder.onConfirm;
        this.onCancel       = builder.onCancel;
    }

    // ── 公开 API ───────────────────────────────────────────────────────────────

    public void setFocused(boolean focused) {
        if (getState().focused != focused) {
            setState(new State(focused));
        }
    }

    public boolean isFocused() {
        return getState().focused;
    }

    // ── 生命周期 ───────────────────────────────────────────────────────────────

    @Override public void onMount()   {}
    @Override public void onUnmount() {}

    // ── 渲染 ───────────────────────────────────────────────────────────────────

    @Override
    public Renderable render() {
        // 格式: "(Y/n)" 或 "(y/N)"，默认选项大写且加粗
        String yLabel = defaultConfirm ? "Y" : "y";
        String nLabel = defaultConfirm ? "n" : "N";

        Text yText = Text.of(yLabel).color(defaultConfirm ? C_YES : C_OTHER);
        Text nText = Text.of(nLabel).color(!defaultConfirm ? C_NO : C_OTHER);
        if (defaultConfirm)  yText = yText.bold();
        if (!defaultConfirm) nText = nText.bold();

        return Box.of(
                Text.of("(").color(C_SEP),
                yText,
                Text.of("/").color(C_SEP),
                nText,
                Text.of(")").color(C_SEP)
        );
    }

    // ── 键盘输入 ───────────────────────────────────────────────────────────────

    @Override
    public void onInput(String input, Key key) {
        if (!getState().focused) return;

        if ("y".equalsIgnoreCase(input)) {
            if (onConfirm != null) onConfirm.run();
        } else if ("n".equalsIgnoreCase(input)) {
            if (onCancel != null) onCancel.run();
        } else if (key.return_()) {
            // Enter 执行默认选项
            if (defaultConfirm) {
                if (onConfirm != null) onConfirm.run();
            } else {
                if (onCancel != null) onCancel.run();
            }
        }
    }

    // ── Builder ────────────────────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        boolean  defaultConfirm = true;
        Runnable onConfirm;
        Runnable onCancel;

        public Builder defaultConfirm(boolean confirm) {
            this.defaultConfirm = confirm;
            return this;
        }

        public Builder onConfirm(Runnable onConfirm) {
            this.onConfirm = onConfirm;
            return this;
        }

        public Builder onCancel(Runnable onCancel) {
            this.onCancel = onCancel;
            return this;
        }

        public ConfirmInput build() {
            return new ConfirmInput(this);
        }
    }
}
