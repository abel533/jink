package io.mybatis.jink.component;

import io.mybatis.jink.dom.ElementNode;
import io.mybatis.jink.dom.TextNode;
import io.mybatis.jink.style.Color;
import io.mybatis.jink.style.Style;
import io.mybatis.jink.style.TextWrap;

import java.util.ArrayList;
import java.util.List;

/**
 * Text 组件 - 文本显示。
 * 对应 ink 的 Text 组件。
 * <p>
 * 使用示例:
 * <pre>
 * Text.of("Hello World").color(Color.Basic.GREEN).bold()
 * Text.of("Hello ", Text.of("World").bold()).color(Color.Basic.GREEN)
 * </pre>
 */
public class Text implements Renderable {

    private final List<Object> children = new ArrayList<>(); // String 或 Text
    private final Style.Builder style = Style.builder();

    private Text() {}

    /**
     * 创建文本组件，支持混合内容（字符串、嵌套 Text、其他 Renderable）
     */
    public static Text of(Object... contents) {
        Text text = new Text();
        for (Object content : contents) {
            if (content instanceof String) {
                text.children.add((String) content);
            } else if (content instanceof Text) {
                text.children.add((Text) content);
            } else if (content instanceof Renderable) {
                // 其他 Renderable（如 Newline）直接作为子元素
                text.children.add((Renderable) content);
            } else {
                text.children.add(String.valueOf(content));
            }
        }
        return text;
    }

    // === 文本颜色 ===

    public Text color(Color color) {
        style.color(color);
        return this;
    }

    public Text backgroundColor(Color color) {
        style.backgroundColor(color);
        return this;
    }

    // === 文本修饰 ===

    public Text bold() {
        style.bold(true);
        return this;
    }

    public Text italic() {
        style.italic(true);
        return this;
    }

    public Text underline() {
        style.underline(true);
        return this;
    }

    public Text strikethrough() {
        style.strikethrough(true);
        return this;
    }

    public Text dimmed() {
        style.dimmed(true);
        return this;
    }

    public Text inverse() {
        style.inverse(true);
        return this;
    }

    // === 文本换行 ===

    public Text wrap(TextWrap wrap) {
        style.textWrap(wrap);
        return this;
    }

    @Override
    public ElementNode toNode() {
        return toNode(false);
    }

    /**
     * 转换为 DOM 节点
     *
     * @param isNested 是否嵌套在另一个 Text 内部（使用 VIRTUAL_TEXT 类型）
     */
    ElementNode toNode(boolean isNested) {
        ElementNode node = isNested
                ? ElementNode.createVirtualText()
                : ElementNode.createText();
        node.setStyle(style.build());

        for (Object child : children) {
            if (child instanceof String) {
                node.appendChild(new TextNode((String) child));
            } else if (child instanceof Text) {
                node.appendChild(((Text) child).toNode(true));
            } else if (child instanceof Renderable) {
                node.appendChild(((Renderable) child).toNode());
            }
        }
        return node;
    }
}
