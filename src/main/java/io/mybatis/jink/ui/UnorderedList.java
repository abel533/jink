package io.mybatis.jink.ui;

import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.dom.ElementNode;
import io.mybatis.jink.style.Color;
import io.mybatis.jink.style.FlexDirection;

import java.util.ArrayList;
import java.util.List;

/**
 * 无序列表组件，支持多层嵌套。
 * 用法:
 * UnorderedList.of()
 *     .item("Red")
 *     .item("Green", UnorderedList.of().item("Light").item("Dark"))
 *     .item("Blue")
 */
public class UnorderedList implements Renderable {

    // 各层级符号颜色：第0层绿色，第1层灰色，第2层及更深暗灰
    private static final Color LEVEL0_COLOR = Color.ansi256(46);
    private static final Color LEVEL1_COLOR = Color.ansi256(245);
    private static final Color LEVEL2_COLOR = Color.ansi256(240);

    private static final String LEVEL0_SYMBOL = "*";
    private static final String LEVEL1_SYMBOL = "-";
    private static final String LEVEL2_SYMBOL = ">";

    private final List<Item> items = new ArrayList<>();

    private UnorderedList() {}

    public static UnorderedList of() {
        return new UnorderedList();
    }

    public UnorderedList item(String label) {
        items.add(new Item(label, null));
        return this;
    }

    public UnorderedList item(String label, UnorderedList children) {
        items.add(new Item(label, children));
        return this;
    }

    private static class Item {
        final String label;
        final UnorderedList children;

        Item(String label, UnorderedList children) {
            this.label = label;
            this.children = children;
        }
    }

    @Override
    public ElementNode toNode() {
        return renderAtDepth(0).toNode();
    }

    private Box renderAtDepth(int depth) {
        Box column = Box.of().flexDirection(FlexDirection.COLUMN);
        String indent = repeat("  ", depth);

        String symbol;
        Color symbolColor;
        if (depth == 0) {
            symbol = LEVEL0_SYMBOL;
            symbolColor = LEVEL0_COLOR;
        } else if (depth == 1) {
            symbol = LEVEL1_SYMBOL;
            symbolColor = LEVEL1_COLOR;
        } else {
            symbol = LEVEL2_SYMBOL;
            symbolColor = LEVEL2_COLOR;
        }

        for (Item item : items) {
            // 缩进（无色）+ 符号（彩色）+ 文字
            column.add(Box.of(
                    Text.of(indent),
                    Text.of(symbol).color(symbolColor),
                    Text.of(" " + item.label)
            ));
            if (item.children != null && !item.children.items.isEmpty()) {
                column.add(item.children.renderAtDepth(depth + 1));
            }
        }

        return column;
    }

    private static String repeat(String str, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}
