package io.mybatis.jink.component;

import io.mybatis.jink.dom.ElementNode;
import io.mybatis.jink.style.FlexDirection;
import io.mybatis.jink.style.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Static 组件 - 增量渲染不可变内容。
 * 对应 ink 的 Static 组件。
 * <p>
 * Static 组件用于渲染日志、已完成的任务等永久性内容。
 * 每次渲染时只输出新增的 items，已输出的内容不会重复渲染。
 * <p>
 * 示例:
 * <pre>
 * Static.&lt;String&gt;of(logMessages)
 *     .render((msg, idx) -> Text.of(msg).dimmed())
 * </pre>
 */
public class Static<T> implements Renderable {

    private final List<T> items;
    private final int previousCount;
    private BiFunction<T, Integer, Renderable> renderFn;

    private Static(List<T> items, int previousCount) {
        this.items = new ArrayList<>(items);
        this.previousCount = previousCount;
    }

    /**
     * 创建 Static 组件，首次渲染全部 items
     */
    public static <T> Static<T> of(List<T> items) {
        return new Static<>(items, 0);
    }

    /**
     * 创建 Static 组件，只渲染 previousCount 之后新增的 items
     */
    public static <T> Static<T> of(List<T> items, int previousCount) {
        return new Static<>(items, previousCount);
    }

    /**
     * 设置渲染函数，接收 (item, index)，返回可渲染对象
     */
    public Static<T> render(BiFunction<T, Integer, Renderable> fn) {
        this.renderFn = fn;
        return this;
    }

    /**
     * 获取当前 items 数量（调用者应保存此值，下次传入 previousCount）
     */
    public int getItemCount() {
        return items.size();
    }

    @Override
    public ElementNode toNode() {
        ElementNode node = ElementNode.createBox();
        node.setStyle(Style.builder().flexDirection(FlexDirection.COLUMN).build());

        if (renderFn == null) return node;

        // 只渲染新增的 items
        for (int i = previousCount; i < items.size(); i++) {
            Renderable child = renderFn.apply(items.get(i), i);
            if (child != null) {
                node.appendChild(child.toNode());
            }
        }

        return node;
    }
}
