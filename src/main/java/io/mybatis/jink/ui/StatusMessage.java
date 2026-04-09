package io.mybatis.jink.ui;

import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.dom.ElementNode;
import io.mybatis.jink.style.Color;

/**
 * 带图标的状态消息。
 * 用法: StatusMessage.of("操作成功").variant(StatusMessage.Variant.SUCCESS)
 */
public class StatusMessage implements Renderable {

    private final String message;
    private Variant variant = Variant.INFO;

    private StatusMessage(String message) {
        this.message = message;
    }

    public static StatusMessage of(String message) {
        return new StatusMessage(message);
    }

    public enum Variant {
        SUCCESS("✓", Color.ansi256(46)),
        ERROR("✗", Color.ansi256(196)),
        WARNING("⚠", Color.ansi256(226)),
        INFO("ℹ", Color.ansi256(27));

        final String icon;
        final Color color;

        Variant(String icon, Color color) {
            this.icon = icon;
            this.color = color;
        }
    }

    public StatusMessage variant(Variant variant) {
        this.variant = variant;
        return this;
    }

    @Override
    public ElementNode toNode() {
        // 图标彩色 + 一个空格 + 消息文字
        return Box.of(
                Text.of(variant.icon).color(variant.color),
                Text.of(" " + message)
        ).toNode();
    }
}
