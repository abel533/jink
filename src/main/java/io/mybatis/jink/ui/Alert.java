package io.mybatis.jink.ui;

import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.dom.ElementNode;
import io.mybatis.jink.style.AlignItems;
import io.mybatis.jink.style.BorderStyle;
import io.mybatis.jink.style.Color;

/**
 * 带圆角边框的提醒框，更突出的状态显示。
 * 用法: Alert.of("A new version is available").variant(Alert.Variant.SUCCESS)
 */
public class Alert implements Renderable {

    private final String message;
    private Variant variant = Variant.INFO;

    private Alert(String message) {
        this.message = message;
    }

    public static Alert of(String message) {
        return new Alert(message);
    }

    public enum Variant {
        SUCCESS("+", Color.ansi256(46)),
        ERROR("x", Color.ansi256(196)),
        WARNING("!", Color.ansi256(226)),
        INFO("i", Color.ansi256(27));

        final String icon;
        final Color color;

        Variant(String icon, Color color) {
            this.icon = icon;
            this.color = color;
        }
    }

    public Alert variant(Variant variant) {
        this.variant = variant;
        return this;
    }

    @Override
    public ElementNode toNode() {
        // alignSelf(FLEX_START) 配合框架修复，让 Box 宽度由内容决定而不是撑满父容器
        return Box.of(
                Text.of(variant.icon).color(variant.color),
                Text.of("  " + message)
        ).borderStyle(BorderStyle.ROUND)
                .borderColor(variant.color)
                .paddingX(1)
                .alignSelf(AlignItems.FLEX_START)
                .toNode();
    }
}
