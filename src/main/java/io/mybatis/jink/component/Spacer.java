package io.mybatis.jink.component;

import io.mybatis.jink.dom.ElementNode;
import io.mybatis.jink.style.Style;

/**
 * Spacer 组件 - 弹性空白。
 * 对应 ink 的 Spacer 组件，本质是一个 flexGrow=1 的空 Box。
 * 用于将内容推到容器两端或均匀分布。
 * <p>
 * 示例: Box.of(Text.of("Left"), Spacer.create(), Text.of("Right"))
 * 效果: Left                                              Right
 */
public class Spacer implements Renderable {

    private static final Spacer INSTANCE = new Spacer();

    private Spacer() {}

    public static Spacer create() {
        return INSTANCE;
    }

    @Override
    public ElementNode toNode() {
        ElementNode node = ElementNode.createBox();
        node.setStyle(Style.builder().flexGrow(1).build());
        return node;
    }
}
