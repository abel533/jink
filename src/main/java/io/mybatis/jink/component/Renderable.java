package io.mybatis.jink.component;

import io.mybatis.jink.dom.ElementNode;

/**
 * 可渲染接口，所有能够产生 DOM 节点的对象都实现此接口。
 * 对应 ink 中 React.Element 的角色。
 */
public interface Renderable {

    /**
     * 将此可渲染对象转换为 DOM 节点树
     */
    ElementNode toNode();
}
