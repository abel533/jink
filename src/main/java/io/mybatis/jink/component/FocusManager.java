package io.mybatis.jink.component;

import java.util.*;

/**
 * 焦点管理器，对应 ink 的 FocusContext + App.tsx 中的焦点逻辑。
 * <p>
 * 维护可聚焦组件的有序列表，支持：
 * - 注册/注销焦点组件
 * - Tab/Shift+Tab 循环导航
 * - 按 ID 程序化聚焦
 * - 启用/禁用焦点系统
 */
public class FocusManager {

    private final List<FocusEntry> focusables = new ArrayList<>();
    private String activeFocusId;
    private boolean focusEnabled = true;

    /**
     * 注册可聚焦组件
     */
    public void register(Focusable focusable) {
        String id = focusable.getFocusId();
        if (findEntry(id) != null) return; // 已注册

        focusables.add(new FocusEntry(id, focusable, true));

        // 自动聚焦：当前无焦点且组件声明 autoFocus
        if (activeFocusId == null && focusable.isAutoFocus()) {
            setFocus(id);
        }
    }

    /**
     * 注销可聚焦组件
     */
    public void unregister(String focusId) {
        FocusEntry entry = findEntry(focusId);
        if (entry == null) return;

        focusables.remove(entry);

        // 如果注销的是当前焦点，清除焦点
        if (focusId.equals(activeFocusId)) {
            activeFocusId = null;
            entry.focusable.onFocusChange(false);
        }
    }

    /**
     * 激活组件的焦点能力（允许被导航到）
     */
    public void activate(String focusId) {
        FocusEntry entry = findEntry(focusId);
        if (entry != null) {
            entry.active = true;
        }
    }

    /**
     * 停用组件的焦点能力（跳过此组件）
     */
    public void deactivate(String focusId) {
        FocusEntry entry = findEntry(focusId);
        if (entry != null) {
            entry.active = false;
            // 如果停用的是当前焦点，移到下一个
            if (focusId.equals(activeFocusId)) {
                focusNext();
            }
        }
    }

    /**
     * 聚焦到下一个可用组件（Tab 键）
     */
    public void focusNext() {
        if (!focusEnabled || focusables.isEmpty()) return;

        int currentIdx = findIndex(activeFocusId);
        int startIdx = currentIdx + 1;

        for (int i = 0; i < focusables.size(); i++) {
            int idx = (startIdx + i) % focusables.size();
            FocusEntry entry = focusables.get(idx);
            if (entry.active) {
                setFocus(entry.id);
                return;
            }
        }
    }

    /**
     * 聚焦到上一个可用组件（Shift+Tab）
     */
    public void focusPrevious() {
        if (!focusEnabled || focusables.isEmpty()) return;

        int currentIdx = findIndex(activeFocusId);
        int startIdx = currentIdx < 0 ? focusables.size() - 1 : currentIdx - 1;

        for (int i = 0; i < focusables.size(); i++) {
            int idx = (startIdx - i + focusables.size()) % focusables.size();
            FocusEntry entry = focusables.get(idx);
            if (entry.active) {
                setFocus(entry.id);
                return;
            }
        }
    }

    /**
     * 按 ID 程序化聚焦
     */
    public void focus(String focusId) {
        FocusEntry entry = findEntry(focusId);
        if (entry != null && entry.active) {
            setFocus(focusId);
        }
    }

    /**
     * 启用焦点系统
     */
    public void enableFocus() {
        this.focusEnabled = true;
    }

    /**
     * 禁用焦点系统
     */
    public void disableFocus() {
        this.focusEnabled = false;
    }

    /**
     * 获取当前焦点 ID
     */
    public String getActiveFocusId() {
        return activeFocusId;
    }

    /**
     * 判断指定组件是否拥有焦点
     */
    public boolean isFocused(String focusId) {
        return focusId != null && focusId.equals(activeFocusId);
    }

    /**
     * 焦点系统是否启用
     */
    public boolean isFocusEnabled() {
        return focusEnabled;
    }

    /**
     * 获取已注册的焦点组件数
     */
    public int getFocusableCount() {
        return focusables.size();
    }

    // ===== 内部方法 =====

    private void setFocus(String newId) {
        String oldId = activeFocusId;
        activeFocusId = newId;

        // 通知旧组件失去焦点
        if (oldId != null && !oldId.equals(newId)) {
            FocusEntry oldEntry = findEntry(oldId);
            if (oldEntry != null) {
                oldEntry.focusable.onFocusChange(false);
            }
        }

        // 通知新组件获得焦点
        if (newId != null && !newId.equals(oldId)) {
            FocusEntry newEntry = findEntry(newId);
            if (newEntry != null) {
                newEntry.focusable.onFocusChange(true);
            }
        }
    }

    private FocusEntry findEntry(String id) {
        if (id == null) return null;
        for (FocusEntry entry : focusables) {
            if (entry.id.equals(id)) return entry;
        }
        return null;
    }

    private int findIndex(String id) {
        if (id == null) return -1;
        for (int i = 0; i < focusables.size(); i++) {
            if (focusables.get(i).id.equals(id)) return i;
        }
        return -1;
    }

    /**
     * 焦点注册条目
     */
    private static class FocusEntry {
        final String id;
        final Focusable focusable;
        boolean active;

        FocusEntry(String id, Focusable focusable, boolean active) {
            this.id = id;
            this.focusable = focusable;
            this.active = active;
        }
    }
}
