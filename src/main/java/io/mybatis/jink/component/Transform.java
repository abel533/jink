package io.mybatis.jink.component;

import io.mybatis.jink.dom.ElementNode;
import io.mybatis.jink.style.Style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Transform 组件 - 对子组件的渲染输出执行字符串变换。
 * 对应 ink 的 Transform 组件。
 * <p>
 * transform 函数签名: (line, index) -> transformedLine
 * 每一行输出都会经过此函数处理。
 * <p>
 * 示例:
 * <pre>
 * Transform.of(Text.of("hello"))
 *     .transform((line, idx) -> line.toUpperCase())
 * </pre>
 */
public class Transform implements Renderable {

    private final List<Renderable> children = new ArrayList<>();
    private BiFunction<String, Integer, String> transformFn;

    private Transform() {}

    public static Transform of(Renderable... children) {
        Transform t = new Transform();
        Collections.addAll(t.children, children);
        return t;
    }

    /**
     * 设置变换函数，接收 (行文本, 行索引)，返回变换后的文本
     */
    public Transform transform(BiFunction<String, Integer, String> fn) {
        this.transformFn = fn;
        return this;
    }

    @Override
    public ElementNode toNode() {
        ElementNode node = ElementNode.createBox();
        node.setStyle(Style.builder().build());
        if (transformFn != null) {
            node.setAttribute("internal_transform", transformFn);
        }
        for (Renderable child : children) {
            node.appendChild(child.toNode());
        }
        return node;
    }
}
