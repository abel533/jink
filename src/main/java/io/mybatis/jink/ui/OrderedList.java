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
 * 有序列表组件，支持多层嵌套。
 * 用法:
 * OrderedList.of()
 *     .item("Red")
 *     .item("Green", OrderedList.of().item("Light").item("Dark"))
 *     .item("Blue")
 */
public class OrderedList implements Renderable {

    private static final Color NUMBER_COLOR = Color.ansi256(246); // 灰色编号
    private static final Color TEXT_COLOR   = Color.ansi256(255); // 白色内容

    private final List<Item> items = new ArrayList<>();

    private OrderedList() {}

    public static OrderedList of() {
        return new OrderedList();
    }

    public OrderedList item(String label) {
        items.add(new Item(label, null));
        return this;
    }

    public OrderedList item(String label, OrderedList children) {
        items.add(new Item(label, children));
        return this;
    }

    private static class Item {
        final String label;
        final OrderedList children;

        Item(String label, OrderedList children) {
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
        // 每层缩进 3 个空格，与 "N. " 的宽度对齐
        String indent = repeat("   ", depth);

        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            column.add(Box.of(
                    Text.of(indent),
                    Text.of((i + 1) + ". ").color(NUMBER_COLOR),
                    Text.of(item.label).color(TEXT_COLOR)
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
