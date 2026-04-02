package io.mybatis.jink.component;

/**
 * 可聚焦组件接口。
 * 实现此接口的组件可以参与 Tab/Shift+Tab 焦点导航。
 */
public interface Focusable {

    /**
     * 获取焦点 ID（在同一应用内唯一）
     */
    String getFocusId();

    /**
     * 是否自动获取焦点（当无组件聚焦时，自动聚焦此组件）
     */
    default boolean isAutoFocus() {
        return false;
    }

    /**
     * 焦点状态变化通知
     *
     * @param focused true=获得焦点, false=失去焦点
     */
    default void onFocusChange(boolean focused) {}
}
