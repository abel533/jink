package io.mybatis.jink.dom;

/**
 * 文本节点，对应 ink 的 TextNode。
 * 只包含文本值，没有子节点。
 */
public final class TextNode extends Node {

    private String nodeValue;

    public TextNode(String nodeValue) {
        this.nodeValue = nodeValue != null ? nodeValue : "";
    }

    @Override
    public String nodeName() {
        return NodeType.TEXT.getName();
    }

    public String getNodeValue() {
        return nodeValue;
    }

    /**
     * 更新文本内容。
     * 对应 ink 的 setTextNodeValue，文本变化会触发布局重新计算。
     */
    public void setNodeValue(String nodeValue) {
        this.nodeValue = nodeValue != null ? nodeValue : "";
        markDirty();
    }

    /**
     * 标记父节点布局为脏，需要重新计算
     */
    private void markDirty() {
        ElementNode parent = getParentNode();
        if (parent != null) {
            // 向上冒泡，找到最近的文本容器节点
            while (parent != null && parent.getNodeType() == NodeType.INK_VIRTUAL_TEXT) {
                parent = parent.getParentNode();
            }
            // 这里后续会触发布局引擎重新测量
        }
    }

    @Override
    public String toString() {
        String preview = nodeValue.length() > 20
                ? nodeValue.substring(0, 20) + "..."
                : nodeValue;
        return "TextNode{\"" + preview + "\"}";
    }
}
