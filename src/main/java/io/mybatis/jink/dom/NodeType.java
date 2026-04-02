package io.mybatis.jink.dom;

/**
 * 节点类型枚举，对应 ink 的 ElementNames / TextName。
 */
public enum NodeType {
    /** 根节点 */
    INK_ROOT("ink-root"),
    /** 盒子容器节点 */
    INK_BOX("ink-box"),
    /** 文本容器节点 */
    INK_TEXT("ink-text"),
    /** 虚拟文本节点（Text 内嵌套的 Text） */
    INK_VIRTUAL_TEXT("ink-virtual-text"),
    /** 纯文本节点 */
    TEXT("#text");

    private final String name;

    NodeType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
