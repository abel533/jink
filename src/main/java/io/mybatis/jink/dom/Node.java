package io.mybatis.jink.dom;

import io.mybatis.jink.style.Style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 虚拟 DOM 节点基类，对应 ink 的 InkNode。
 * 所有节点（元素节点、文本节点）的公共父类。
 */
public abstract sealed class Node permits ElementNode, TextNode {

    protected ElementNode parentNode;
    protected Style style;

    protected Node() {
        this.style = Style.EMPTY;
    }

    /**
     * 节点类型名称
     */
    public abstract String nodeName();

    public ElementNode getParentNode() {
        return parentNode;
    }

    void setParentNode(ElementNode parentNode) {
        this.parentNode = parentNode;
    }

    public Style getStyle() {
        return style;
    }

    public void setStyle(Style style) {
        this.style = Objects.requireNonNull(style);
    }

    /**
     * 检测节点是否在文本容器内
     */
    public boolean isInsideText() {
        ElementNode parent = this.parentNode;
        while (parent != null) {
            if (parent.getNodeType() == NodeType.INK_TEXT) {
                return true;
            }
            parent = parent.getParentNode();
        }
        return false;
    }
}
