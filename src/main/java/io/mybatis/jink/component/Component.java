package io.mybatis.jink.component;

import io.mybatis.jink.dom.ElementNode;
import io.mybatis.jink.input.Key;

/**
 * 有状态组件基类。
 * 对应 ink 中的 React 类组件概念。
 * <p>
 * 使用示例:
 * <pre>
 * public class Counter extends Component&lt;Counter.State&gt; {
 *     record State(int count) {}
 *
 *     public Counter() { super(new State(0)); }
 *
 *     public Renderable render() {
 *         return Box.of(
 *             Text.of("Count: " + getState().count()).color(Color.Basic.GREEN)
 *         );
 *     }
 *
 *     public void increment() {
 *         setState(new State(getState().count() + 1));
 *     }
 * }
 * </pre>
 */
public abstract class Component<S> implements Renderable {

    private S state;
    private Runnable onStateChange;
    private int columns = 80;
    private int rows = 24;
    private int cursorRow = -1;
    private int cursorCol = -1;

    protected Component(S initialState) {
        this.state = initialState;
    }

    /**
     * 渲染组件，返回可渲染描述
     */
    public abstract Renderable render();

    /**
     * 获取当前状态
     */
    protected S getState() {
        return state;
    }

    /**
     * 更新状态并触发重渲染
     */
    protected void setState(S newState) {
        this.state = newState;
        if (onStateChange != null) {
            onStateChange.run();
        }
    }

    /**
     * 设置状态变化回调（由 Ink 框架内部调用）
     */
    public void setOnStateChange(Runnable callback) {
        this.onStateChange = callback;
    }

    /**
     * 组件挂载时调用
     */
    public void onMount() {
    }

    /**
     * 组件卸载时调用
     */
    public void onUnmount() {
    }

    /**
     * 键盘输入处理
     */
    public void onInput(String input, Key key) {
    }

    /**
     * 粘贴事件处理（Bracketed Paste Mode）。
     * 当用户在终端中粘贴文本时触发，text 包含完整的粘贴内容。
     * 默认实现将粘贴文本逐字符作为 onInput 分发。
     * 子类可覆盖以批量处理粘贴内容。
     */
    public void onPaste(String text) {
        // 默认将粘贴文本逐字符通过 onInput 分发
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            onInput(ch, Key.plain());
        }
    }

    /**
     * 获取终端列数（由 Ink 框架设置）
     */
    protected int getColumns() {
        return columns;
    }

    /**
     * 获取终端行数（由 Ink 框架设置）
     */
    protected int getRows() {
        return rows;
    }

    /**
     * 设置终端尺寸（由 Ink 框架内部调用）
     */
    public void setTerminalSize(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
    }

    /**
     * 设置光标位置（0-indexed）。-1 表示隐藏光标。
     * 组件在 render() 中调用此方法告知框架光标应显示的位置。
     */
    protected void setCursorPosition(int row, int col) {
        this.cursorRow = row;
        this.cursorCol = col;
    }

    /**
     * 获取光标行位置（0-indexed，-1=隐藏）
     */
    public int getCursorRow() {
        return cursorRow;
    }

    /**
     * 获取光标列位置（0-indexed，-1=隐藏）
     */
    public int getCursorCol() {
        return cursorCol;
    }

    @Override
    public ElementNode toNode() {
        Renderable rendered = render();
        return rendered.toNode();
    }
}
