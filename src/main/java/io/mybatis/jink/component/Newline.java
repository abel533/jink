package io.mybatis.jink.component;

import io.mybatis.jink.dom.ElementNode;
import io.mybatis.jink.dom.TextNode;

/**
 * Newline 组件 - 插入换行符。
 * 对应 ink 的 Newline 组件，必须在 Text 内部使用。
 * <p>
 * 示例: Text.of("Line 1", Newline.create(), "Line 2")
 */
public class Newline implements Renderable {

    private final int count;

    private Newline(int count) {
        this.count = Math.max(1, count);
    }

    public static Newline create() {
        return new Newline(1);
    }

    public static Newline create(int count) {
        return new Newline(count);
    }

    @Override
    public ElementNode toNode() {
        ElementNode node = ElementNode.createVirtualText();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append('\n');
        }
        node.appendChild(new TextNode(sb.toString()));
        return node;
    }
}
