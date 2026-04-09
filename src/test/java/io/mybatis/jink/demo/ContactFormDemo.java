package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Component;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.input.Key;
import io.mybatis.jink.style.Color;
import io.mybatis.jink.style.FlexDirection;
import io.mybatis.jink.ui.TextInput;
import io.mybatis.jink.util.StringWidth;

import java.util.regex.Pattern;

/**
 * 联系人信息录入表单 Demo。
 *
 * <p>重构版：字段输入逻辑委托给 {@link TextInput} 组件，ContactFormDemo 只管理
 * 焦点切换、表单验证和整体布局，大幅精简代码。
 *
 * <p>快捷键：
 * <pre>
 *  Tab / ↓      下一字段 / 按钮
 *  Shift+Tab / ↑ 上一字段 / 按钮
 *  Enter        字段内：移到下一个；提交/清空按钮：执行
 *  ← → Home End  字段内光标移动（由 TextInput 处理）
 *  Backspace    删除光标前字符（由 TextInput 处理）
 *  q            退出
 * </pre>
 *
 * <p>运行方式：
 * <pre>
 * .\scripts\run-demo.ps1 io.mybatis.jink.demo.ContactFormDemo
 * </pre>
 */
public class ContactFormDemo extends Component<ContactFormDemo.State> {

    // ── 颜色常量 ───────────────────────────────────────────────────────────────

    private static final Color C_TITLE    = Color.ansi256(82);   // 标题亮绿
    private static final Color C_LABEL    = Color.ansi256(246);  // 普通字段标签（灰）
    private static final Color C_FOCUS_L  = Color.ansi256(51);   // 聚焦字段标签（青蓝）
    private static final Color C_SEP      = Color.ansi256(239);  // 分隔符（暗灰）
    private static final Color C_VALUE    = Color.ansi256(255);  // 已输入值（白）
    private static final Color C_HINT_V   = Color.ansi256(238);  // 空字段占位提示（极暗灰）
    private static final Color C_BTN_N    = Color.ansi256(252);  // 普通按钮文字
    private static final Color C_BTN_F    = Color.ansi256(16);   // 聚焦按钮文字（黑）
    private static final Color C_BTN_NBG  = Color.ansi256(237);  // 普通按钮背景
    private static final Color C_BTN_FBG  = Color.ansi256(82);   // 聚焦按钮背景（亮绿）
    private static final Color C_ERROR    = Color.ansi256(196);  // 错误（红）
    private static final Color C_SUCCESS  = Color.ansi256(46);   // 成功（亮绿）
    private static final Color C_HINT_S   = Color.ansi256(240);  // 状态栏提示（暗灰）
    private static final Color C_CURSOR   = Color.ansi256(220);  // 光标指示符颜色

    // ── 字段定义 ───────────────────────────────────────────────────────────────

    /** 4 个输入字段：姓名/手机/地址/邮件 */
    private static final int FIELD_COUNT = 4;

    /** 字段标签（各 6 display cols：2 CJK 字符 + 2 空格） */
    private static final String[] LABELS = { "姓  名", "手  机", "地  址", "邮  件" };

    /** 字段输入最大显示列数 */
    private static final int[] MAX_COLS = { 20, 11, 50, 50 };

    /** 空字段占位提示文字 */
    private static final String[] HINTS = {
        "请输入姓名", "如: 13800138000", "请输入地址", "如: name@example.com"
    };

    /**
     * 前缀显示宽度：
     * {@code "  " (2) + "姓  名" (6) + " ›  " (4) = 12 display cols}
     */
    private static final int PREFIX_W = 12;

    /** 焦点索引：0-3 = 字段，4 = 提交按钮，5 = 清空按钮 */
    private static final int FOCUS_SUBMIT = 4;
    private static final int FOCUS_CLEAR  = 5;
    private static final int FOCUS_TOTAL  = 6;

    // 行布局（0-based，setCursorPosition 使用相同索引）：
    // 0: 标题行
    // 1: 空行
    // 2: 字段 0 (姓名)   ←── cursor row = 2 + fieldIdx
    // 3: 字段 1 (手机)
    // 4: 字段 2 (地址)
    // 5: 字段 3 (邮件)
    // 6: 空行
    // 7: 按钮行
    // 8: 消息行
    // 9: 空行

    // ── 验证规则 ───────────────────────────────────────────────────────────────

    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@.\\s]{2,}$");

    // ── 状态（仅保留焦点 + 消息，字段值由 TextInput 自身管理） ────────────────────

    static final class State {
        final int     focused;  // 当前焦点（0-5）
        final String  message;  // 底部提示消息
        final boolean success;

        State(int focused, String message, boolean success) {
            this.focused = focused;
            this.message = message;
            this.success = success;
        }

        State withFocus(int f)            { return new State(f, message, success); }
        State withMsg(String m, boolean s){ return new State(focused, m, s); }

        static State initial() { return new State(0, "", false); }
    }

    // ── 子组件：4 个 TextInput ─────────────────────────────────────────────────

    private final TextInput[] fieldInputs;

    // ── 构造 ───────────────────────────────────────────────────────────────────

    public ContactFormDemo() {
        super(State.initial());
        fieldInputs = new TextInput[FIELD_COUNT];
        for (int i = 0; i < FIELD_COUNT; i++) {
            fieldInputs[i] = TextInput.builder()
                    .placeholder(HINTS[i])
                    .maxLength(MAX_COLS[i])
                    .build();
        }
        fieldInputs[0].setFocused(true); // 初始聚焦第一个字段
    }

    /** 级联 onStateChange：TextInput 字符输入时触发整体重绘 */
    @Override
    public void setOnStateChange(Runnable callback) {
        super.setOnStateChange(callback);
        for (TextInput fi : fieldInputs) {
            fi.setOnStateChange(callback);
        }
    }

    @Override public void onMount()   {}
    @Override public void onUnmount() {}

    // ── 渲染 ───────────────────────────────────────────────────────────────────

    @Override
    public Renderable render() {
        State s = getState();

        // 精确定位光标：从当前聚焦的 TextInput 读取光标字符偏移，由根组件统一设置
        if (s.focused < FIELD_COUNT) {
            TextInput.State ts = fieldInputs[s.focused].getState();
            int colOffset = StringWidth.width(ts.value.substring(0, ts.cursor));
            setCursorPosition(2 + s.focused, PREFIX_W + colOffset);
        }

        Box root = Box.of().flexDirection(FlexDirection.COLUMN);

        // Row 0: 标题
        root.add(Box.of(
                Text.of("  ● ").color(C_TITLE),
                Text.of("联系人信息录入").color(C_TITLE).bold(),
                Text.of("  [↑↓] 切换字段  [Tab] 同上  [Enter] 确认  [鼠标点击] 选择  [q] 退出").color(C_HINT_S)
        ));

        // Row 1: 空行
        root.add(Text.of(" "));

        // Rows 2-5: 4 个字段
        for (int i = 0; i < FIELD_COUNT; i++) {
            TextInput.State ts = fieldInputs[i].getState();
            root.add(buildField(i, ts.value, s.focused == i, hasError(s, i)));
        }

        // Row 6: 空行
        root.add(Text.of(" "));

        // Row 7: 按钮行
        root.add(buildButtons(s.focused));

        // Row 8: 消息行（始终占位，保持光标行号稳定）
        root.add(s.message.isEmpty()
                ? Text.of(" ")
                : Text.of("  " + (s.success ? "✓  " : "✗  ") + s.message)
                        .color(s.success ? C_SUCCESS : C_ERROR));

        // Row 9: 空行
        root.add(Text.of(" "));

        return root;
    }

    /** 某字段是否处于错误高亮状态（提交失败后焦点落在该字段）*/
    private static boolean hasError(State s, int idx) {
        return !s.success && !s.message.isEmpty() && s.focused == idx;
    }

    /**
     * 构建字段行。
     *
     * <p>前缀 = {@code "  " + label + " ›  "} = 2 + 6 + 4 = 12 display cols = {@link #PREFIX_W}。
     */
    private Box buildField(int idx, String value, boolean focused, boolean error) {
        Color labelColor = error ? C_ERROR : (focused ? C_FOCUS_L : C_LABEL);
        Color arrowColor = focused ? C_CURSOR : C_SEP;

        Box row = Box.of(
                Text.of("  ").color(C_SEP),
                Text.of(LABELS[idx]).color(labelColor),
                Text.of(" ").color(C_SEP),
                Text.of(focused ? "›" : " ").color(arrowColor),
                Text.of("  ").color(C_SEP)
        );

        if (value.isEmpty()) {
            row.add(focused
                    ? Text.of(" ")           // 光标由 setCursorPosition 精确放置于此
                    : Text.of(HINTS[idx]).color(C_HINT_V));
        } else {
            row.add(Text.of(value).color(focused ? C_FOCUS_L : C_VALUE));
        }
        return row;
    }

    /** 构建按钮行 */
    private Box buildButtons(int focused) {
        boolean submitFocused = focused == FOCUS_SUBMIT;
        boolean clearFocused  = focused == FOCUS_CLEAR;
        return Box.of(
                Text.of("  "),
                Text.of(" 提  交 ")
                        .color(submitFocused ? C_BTN_F : C_BTN_N)
                        .backgroundColor(submitFocused ? C_BTN_FBG : C_BTN_NBG),
                Text.of("    "),
                Text.of(" 清  空 ")
                        .color(clearFocused ? C_BTN_F : C_BTN_N)
                        .backgroundColor(clearFocused ? C_BTN_FBG : C_BTN_NBG)
        );
    }

    // ── 焦点管理 ───────────────────────────────────────────────────────────────

    private void switchFocus(State s, int newFocus) {
        if (s.focused < FIELD_COUNT) fieldInputs[s.focused].setFocused(false);
        if (newFocus  < FIELD_COUNT) fieldInputs[newFocus].setFocused(true);
        setState(s.withFocus(newFocus));
    }

    // ── 键盘输入 ───────────────────────────────────────────────────────────────

    @Override
    public void onInput(String input, Key key) {
        if ("q".equals(input)) { exit(); return; }
        State s = getState();

        // ↑↓：切换焦点
        if (key.downArrow()) {
            switchFocus(s, (s.focused + 1) % FOCUS_TOTAL);
            return;
        }
        if (key.upArrow()) {
            switchFocus(s, (s.focused - 1 + FOCUS_TOTAL) % FOCUS_TOTAL);
            return;
        }

        // Tab / Shift+Tab：与 ↑↓ 等效
        if (key.tab()) {
            int next = key.shift()
                    ? (s.focused - 1 + FOCUS_TOTAL) % FOCUS_TOTAL
                    : (s.focused + 1) % FOCUS_TOTAL;
            switchFocus(s, next);
            return;
        }

        // Enter：字段内移到下一个；按钮执行操作
        if (key.return_()) {
            if (s.focused == FOCUS_SUBMIT) { handleSubmit(); return; }
            if (s.focused == FOCUS_CLEAR)  { handleClear();  return; }
            switchFocus(s, (s.focused + 1) % FOCUS_TOTAL);
            return;
        }

        // 字段键盘输入：委托给当前聚焦的 TextInput（光标移动/Backspace/Delete/字符输入）
        if (s.focused < FIELD_COUNT) {
            fieldInputs[s.focused].onInput(input, key);
            // 输入时清除错误消息
            if (!s.message.isEmpty() && !s.success) {
                setState(s.withMsg("", false));
            }
        }
    }

    // ── 鼠标点击 ────────────────────────────────────────────────────────────────

    /**
     * 鼠标点击处理：根据终端坐标直接跳转到对应字段/按钮。
     * 行布局：0=标题，1=空，2-5=字段 0-3，6=空，7=按钮，8=消息，9=空
     * 按钮列布局：col 2-9 = 提交，col 14-21 = 清空（display cols）
     */
    @Override
    public void onMouseClick(int x, int y) {
        State s = getState();
        if (y >= 2 && y <= 5) {
            switchFocus(s, y - 2);
        } else if (y == 7) {
            if (x >= 2 && x <= 9)   handleSubmit();
            else if (x >= 14 && x <= 21) handleClear();
        }
    }

    // ── 表单操作 ───────────────────────────────────────────────────────────────

    /** 校验所有字段并提交 */
    private void handleSubmit() {
        String name  = fieldInputs[0].getValue().trim();
        String phone = fieldInputs[1].getValue().trim();
        String addr  = fieldInputs[2].getValue().trim();
        String email = fieldInputs[3].getValue().trim();
        State s = getState();

        if (name.isEmpty()) {
            switchFocus(s, 0);
            setState(getState().withMsg("姓名不能为空", false)); return;
        }
        if (!PHONE.matcher(phone).matches()) {
            switchFocus(s, 1);
            setState(getState().withMsg("手机号格式错误（需 11 位，以 1[3-9] 开头）", false)); return;
        }
        if (addr.isEmpty()) {
            switchFocus(s, 2);
            setState(getState().withMsg("地址不能为空", false)); return;
        }
        if (!EMAIL.matcher(email).matches()) {
            switchFocus(s, 3);
            setState(getState().withMsg("邮件格式错误", false)); return;
        }
        setState(getState().withFocus(FOCUS_SUBMIT)
                .withMsg("提交成功！  " + name + "  |  " + phone + "  |  " + email, true));
    }

    /** 清空所有字段并复位焦点到第一个字段 */
    private void handleClear() {
        for (int i = 0; i < FIELD_COUNT; i++) {
            fieldInputs[i].setValue("");
            fieldInputs[i].setFocused(i == 0);
        }
        setState(State.initial());
    }

    // ── 入口 ───────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        Ink.render(new ContactFormDemo()).enableMouseTracking().waitUntilExit();
    }
}
