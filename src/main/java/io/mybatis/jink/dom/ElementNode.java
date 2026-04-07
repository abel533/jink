package io.mybatis.jink.dom;

import io.mybatis.jink.style.Style;

import java.util.*;
import java.util.function.Consumer;

/**
 * 元素节点，对应 ink 的 DOMElement。
 * 可以包含子节点，持有样式和布局信息。
 */
public final class ElementNode extends Node {

    private final NodeType nodeType;
    private final List<Node> childNodes;
    private final Map<String, Object> attributes;

    // 布局计算结果（由 Flexbox 布局引擎填充）
    private int computedLeft;
    private int computedTop;
    private int computedWidth;
    private int computedHeight;

    // 布局监听器
    private final Set<Runnable> layoutListeners;

    // 渲染回调
    private Runnable onComputeLayout;
    private Runnable onRender;
    private Runnable onImmediateRender;

    // Static 组件支持
    private boolean staticDirty;
    private ElementNode staticNode;

    public ElementNode(NodeType nodeType) {
        this.nodeType = nodeType;
        this.childNodes = new ArrayList<>();
        this.attributes = new LinkedHashMap<>();
        this.layoutListeners = new LinkedHashSet<>();
    }

    @Override
    public String nodeName() {
        return nodeType.getName();
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    // ===== 子节点操作 =====

    public List<Node> getChildNodes() {
        return Collections.unmodifiableList(childNodes);
    }

    public int getChildCount() {
        return childNodes.size();
    }

    /**
     * 添加子节点到末尾
     */
    public void appendChild(Node child) {
        Objects.requireNonNull(child, "child must not be null");
        if (child.getParentNode() != null) {
            child.getParentNode().removeChild(child);
        }
        child.setParentNode(this);
        childNodes.add(child);
    }

    /**
     * 在指定节点前插入子节点
     */
    public void insertBefore(Node newChild, Node beforeChild) {
        Objects.requireNonNull(newChild, "newChild must not be null");
        Objects.requireNonNull(beforeChild, "beforeChild must not be null");
        int index = childNodes.indexOf(beforeChild);
        if (index < 0) {
            throw new IllegalArgumentException("beforeChild is not a child of this node");
        }
        if (newChild.getParentNode() != null) {
            newChild.getParentNode().removeChild(newChild);
        }
        newChild.setParentNode(this);
        childNodes.add(index, newChild);
    }

    /**
     * 移除子节点
     */
    public void removeChild(Node child) {
        Objects.requireNonNull(child, "child must not be null");
        if (childNodes.remove(child)) {
            child.setParentNode(null);
        }
    }

    /**
     * 清空所有子节点
     */
    public void clearChildren() {
        for (Node child : childNodes) {
            child.setParentNode(null);
        }
        childNodes.clear();
    }

    // ===== 属性操作 =====

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    // ===== 布局结果 =====

    public int getComputedLeft() {
        return computedLeft;
    }

    public void setComputedLeft(int computedLeft) {
        this.computedLeft = computedLeft;
    }

    public int getComputedTop() {
        return computedTop;
    }

    public void setComputedTop(int computedTop) {
        this.computedTop = computedTop;
    }

    public int getComputedWidth() {
        return computedWidth;
    }

    public void setComputedWidth(int computedWidth) {
        this.computedWidth = computedWidth;
    }

    public int getComputedHeight() {
        return computedHeight;
    }

    public void setComputedHeight(int computedHeight) {
        this.computedHeight = computedHeight;
    }

    // ===== 布局监听 =====

    public void addLayoutListener(Runnable listener) {
        layoutListeners.add(listener);
    }

    public void removeLayoutListener(Runnable listener) {
        layoutListeners.remove(listener);
    }

    public void emitLayoutListeners() {
        for (Runnable listener : layoutListeners) {
            listener.run();
        }
    }

    // ===== 渲染回调 =====

    public Runnable getOnComputeLayout() {
        return onComputeLayout;
    }

    public void setOnComputeLayout(Runnable onComputeLayout) {
        this.onComputeLayout = onComputeLayout;
    }

    public Runnable getOnRender() {
        return onRender;
    }

    public void setOnRender(Runnable onRender) {
        this.onRender = onRender;
    }

    public Runnable getOnImmediateRender() {
        return onImmediateRender;
    }

    public void setOnImmediateRender(Runnable onImmediateRender) {
        this.onImmediateRender = onImmediateRender;
    }

    // ===== Static 支持 =====

    public boolean isStaticDirty() {
        return staticDirty;
    }

    public void setStaticDirty(boolean staticDirty) {
        this.staticDirty = staticDirty;
    }

    public ElementNode getStaticNode() {
        return staticNode;
    }

    public void setStaticNode(ElementNode staticNode) {
        this.staticNode = staticNode;
    }

    // ===== 便利方法 =====

    /**
     * 深度优先遍历所有后代节点
     */
    public void walk(Consumer<Node> visitor) {
        for (Node child : childNodes) {
            visitor.accept(child);
            if (child instanceof ElementNode) {
                ((ElementNode) child).walk(visitor);
            }
        }
    }

    /**
     * 创建根节点
     */
    public static ElementNode createRoot() {
        return new ElementNode(NodeType.INK_ROOT);
    }

    /**
     * 创建 Box 节点
     */
    public static ElementNode createBox() {
        return new ElementNode(NodeType.INK_BOX);
    }

    /**
     * 创建 Text 容器节点
     */
    public static ElementNode createText() {
        return new ElementNode(NodeType.INK_TEXT);
    }

    /**
     * 创建虚拟文本节点
     */
    public static ElementNode createVirtualText() {
        return new ElementNode(NodeType.INK_VIRTUAL_TEXT);
    }

    @Override
    public String toString() {
        return "ElementNode{" + nodeType.getName() +
                ", children=" + childNodes.size() +
                ", style=" + style +
                '}';
    }
}
