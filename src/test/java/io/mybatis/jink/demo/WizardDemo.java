package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.*;
import io.mybatis.jink.input.Key;
import io.mybatis.jink.style.Color;
import io.mybatis.jink.style.FlexDirection;
import io.mybatis.jink.ui.*;
import io.mybatis.jink.util.StringWidth;

/**
 * 交互组件向导 Demo。
 *
 * <p>逐步展示 TextInput → Select → ConfirmInput 三个交互组件。
 *
 * <p>运行:
 * <pre>
 * mvn exec:java -Dexec.mainClass=io.mybatis.jink.demo.WizardDemo -Dexec.classpathScope=test
 * </pre>
 */
public class WizardDemo extends Component<WizardDemo.State> {

    // ── 常量 ──────────────────────────────────────────────────────────────────

    private static final Color C_TITLE   = Color.ansi256(51);   // 青蓝标题
    private static final Color C_LABEL   = Color.ansi256(252);  // 浅灰标签
    private static final Color C_RESULT  = Color.ansi256(46);   // 绿色结果
    private static final Color C_DONE    = Color.ansi256(240);  // 暗灰已完成行

    // ── 状态 ──────────────────────────────────────────────────────────────────

    static final class State {
        /** 当前步骤（0~3，3=展示结果） */
        final int    step;
        final String name;
        final String tech;

        State(int step, String name, String tech) {
            this.step = step;
            this.name = name;
            this.tech = tech;
        }
    }

    // ── 子组件 ────────────────────────────────────────────────────────────────

    private final TextInput    nameInput;
    private final Select       techSelect;
    private final ConfirmInput confirmInput;

    /** Ink 设置的重绘回调，用于级联给子组件 */
    private Runnable rerender;

    // ── 构造 ──────────────────────────────────────────────────────────────────

    public WizardDemo() {
        super(new State(0, "", ""));

        nameInput = TextInput.builder()
                .placeholder("请输入您的姓名")
                .showBorder(true)
                .onSubmit(this::onNameSubmit)
                .build();

        techSelect = Select.builder()
                .option("Java",       "java")
                .option("TypeScript", "typescript")
                .option("Python",     "python")
                .option("Go",         "go")
                .option("Rust",       "rust")
                .option("C++",        "cpp")
                .visibleRows(4)
                .onChange(this::onTechSelected)
                .build();

        confirmInput = ConfirmInput.builder()
                .defaultConfirm(true)
                .onConfirm(this::onConfirmed)
                .onCancel(() -> exit())
                .build();
    }

    // ── 框架回调 ──────────────────────────────────────────────────────────────

    /** 级联 onStateChange：子组件状态变化时触发整体重绘 */
    @Override
    public void setOnStateChange(Runnable callback) {
        super.setOnStateChange(callback);
        this.rerender = callback;
        nameInput.setOnStateChange(callback);
        techSelect.setOnStateChange(callback);
        confirmInput.setOnStateChange(callback);
    }

    @Override
    public void onMount() {
        nameInput.setFocused(true);
    }

    @Override
    public void onInput(String input, Key key) {
        int step = getState().step;
        if (step == 0) {
            nameInput.onInput(input, key);
        } else if (step == 1) {
            techSelect.onInput(input, key);
        } else if (step == 2) {
            confirmInput.onInput(input, key);
        } else if (step == 3) {
            // 结果页：任意键退出
            if (!"".equals(input) || key.escape() || key.return_()) {
                exit();
            }
        }
    }

    // ── 子组件回调 ────────────────────────────────────────────────────────────

    private void onNameSubmit(String name) {
        if (name.trim().isEmpty()) return;
        nameInput.setFocused(false);
        techSelect.setFocused(true);
        setState(new State(1, name.trim(), ""));
    }

    private void onTechSelected(String tech) {
        // Select.onChange 在 Enter 时触发
        techSelect.setFocused(false);
        confirmInput.setFocused(true);
        setState(new State(2, getState().name, tech));
    }

    private void onConfirmed() {
        confirmInput.setFocused(false);
        setState(new State(3, getState().name, getState().tech));
    }

    // ── 渲染 ──────────────────────────────────────────────────────────────────

    @Override
    public Renderable render() {
        State s = getState();
        Box root = Box.of().flexDirection(FlexDirection.COLUMN);

        // 步骤 0 时在根组件计算绝对光标位置：
        // row 0: 标题, row 1: 空行(marginTop), row 2: 提示, row 3: 上边框, row 4: 内容行
        if (s.step == 0 && nameInput.getState().focused) {
            TextInput.State ts = nameInput.getState();
            String beforeCursor = ts.value.substring(0, ts.cursor);
            // borderOffset=2 (左圆角边框+paddingX), displayBefore 考虑滚动后与 value 同步
            int col = 2 + StringWidth.width(beforeCursor);
            setCursorPosition(4, col);
        }

        // 标题
        root.add(Text.of("交互向导 Demo").color(C_TITLE).bold());
        root.add(Box.of().marginTop(1));

        // 步骤 0：姓名输入
        if (s.step == 0) {
            root.add(Text.of("● 请输入您的姓名：").color(C_LABEL));
            root.add(nameInput);
            root.add(Text.of("  Enter 确认").dimmed());
        } else {
            root.add(Box.of(
                    Text.of("✓ 姓名: ").color(C_DONE),
                    Text.of(s.name).color(C_DONE)
            ));
        }

        root.add(Box.of().marginTop(1));

        // 步骤 1：技术选择
        if (s.step == 1) {
            root.add(Text.of("● 请选择主要技术栈：").color(C_LABEL));
            root.add(techSelect);
            root.add(Text.of("  ↑↓ 移动  Enter 选中").dimmed());
        } else if (s.step >= 2) {
            root.add(Box.of(
                    Text.of("✓ 技术: ").color(C_DONE),
                    Text.of(s.tech).color(C_DONE)
            ));
        }

        root.add(Box.of().marginTop(1));

        // 步骤 2：确认
        if (s.step == 2) {
            root.add(Box.of(
                    Text.of("● 确认提交以上信息？").color(C_LABEL),
                    Text.of("  "),
                    confirmInput
            ));
            root.add(Text.of("  y/Y 确认   n/N 取消   Enter 使用默认").dimmed());
        }

        // 步骤 3：结果
        if (s.step == 3) {
            root.add(Box.of().marginTop(1));
            root.add(Text.of("🎉 提交成功！").color(C_RESULT).bold());
            root.add(Box.of(
                    Text.of("  姓名: ").color(C_LABEL),
                    Text.of(s.name).color(C_RESULT)
            ));
            root.add(Box.of(
                    Text.of("  技术: ").color(C_LABEL),
                    Text.of(s.tech).color(C_RESULT)
            ));
            root.add(Box.of().marginTop(1));
            root.add(Text.of("按任意键退出").dimmed());
        }

        return root;
    }

    public static void main(String[] args) {
        Ink.render(new WizardDemo()).waitUntilExit();
    }
}
