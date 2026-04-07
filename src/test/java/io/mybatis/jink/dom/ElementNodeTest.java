package io.mybatis.jink.dom;

import io.mybatis.jink.style.Style;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ElementNodeTest {

    @Test
    void createRoot() {
        ElementNode root = ElementNode.createRoot();
        assertEquals("ink-root", root.nodeName());
        assertEquals(NodeType.INK_ROOT, root.getNodeType());
        assertEquals(0, root.getChildCount());
        assertNull(root.getParentNode());
    }

    @Test
    void appendChild() {
        ElementNode parent = ElementNode.createBox();
        ElementNode child1 = ElementNode.createBox();
        ElementNode child2 = ElementNode.createText();

        parent.appendChild(child1);
        parent.appendChild(child2);

        assertEquals(2, parent.getChildCount());
        assertSame(parent, child1.getParentNode());
        assertSame(parent, child2.getParentNode());
        assertSame(child1, parent.getChildNodes().get(0));
        assertSame(child2, parent.getChildNodes().get(1));
    }

    @Test
    void insertBefore() {
        ElementNode parent = ElementNode.createBox();
        ElementNode child1 = ElementNode.createBox();
        ElementNode child2 = ElementNode.createBox();
        ElementNode child3 = ElementNode.createBox();

        parent.appendChild(child1);
        parent.appendChild(child3);
        parent.insertBefore(child2, child3);

        assertEquals(3, parent.getChildCount());
        assertSame(child1, parent.getChildNodes().get(0));
        assertSame(child2, parent.getChildNodes().get(1));
        assertSame(child3, parent.getChildNodes().get(2));
    }

    @Test
    void removeChild() {
        ElementNode parent = ElementNode.createBox();
        ElementNode child = ElementNode.createText();

        parent.appendChild(child);
        assertEquals(1, parent.getChildCount());

        parent.removeChild(child);
        assertEquals(0, parent.getChildCount());
        assertNull(child.getParentNode());
    }

    @Test
    void clearChildren() {
        ElementNode parent = ElementNode.createBox();
        parent.appendChild(ElementNode.createBox());
        parent.appendChild(new TextNode("hello"));
        parent.appendChild(ElementNode.createText());

        assertEquals(3, parent.getChildCount());
        parent.clearChildren();
        assertEquals(0, parent.getChildCount());
    }

    @Test
    void reparenting() {
        ElementNode parent1 = ElementNode.createBox();
        ElementNode parent2 = ElementNode.createBox();
        ElementNode child = ElementNode.createBox();

        parent1.appendChild(child);
        assertSame(parent1, child.getParentNode());
        assertEquals(1, parent1.getChildCount());

        // 添加到新父节点会自动从旧父节点移除
        parent2.appendChild(child);
        assertSame(parent2, child.getParentNode());
        assertEquals(0, parent1.getChildCount());
        assertEquals(1, parent2.getChildCount());
    }

    @Test
    void walk() {
        ElementNode root = ElementNode.createRoot();
        ElementNode box = ElementNode.createBox();
        ElementNode text = ElementNode.createText();
        TextNode textNode = new TextNode("hello");

        root.appendChild(box);
        box.appendChild(text);
        text.appendChild(textNode);

        java.util.ArrayList<Node> visited = new java.util.ArrayList<Node>();
        root.walk(visited::add);

        assertEquals(3, visited.size());
        assertSame(box, visited.get(0));
        assertSame(text, visited.get(1));
        assertSame(textNode, visited.get(2));
    }

    @Test
    void attributes() {
        ElementNode node = ElementNode.createBox();
        node.setAttribute("key", "value");
        assertEquals("value", node.getAttribute("key"));
    }

    @Test
    void computedLayout() {
        ElementNode node = ElementNode.createBox();
        node.setComputedWidth(80);
        node.setComputedHeight(24);
        node.setComputedLeft(5);
        node.setComputedTop(10);

        assertEquals(80, node.getComputedWidth());
        assertEquals(24, node.getComputedHeight());
        assertEquals(5, node.getComputedLeft());
        assertEquals(10, node.getComputedTop());
    }

    @Test
    void layoutListeners() {
        ElementNode node = ElementNode.createRoot();
        boolean[] called = new boolean[]{false};

        node.addLayoutListener(() -> called[0] = true);
        assertFalse(called[0]);

        node.emitLayoutListeners();
        assertTrue(called[0]);
    }

    @Test
    void isInsideText() {
        ElementNode root = ElementNode.createRoot();
        ElementNode text = ElementNode.createText();
        ElementNode virtualText = ElementNode.createVirtualText();
        TextNode textNode = new TextNode("hello");

        root.appendChild(text);
        text.appendChild(virtualText);
        virtualText.appendChild(textNode);

        assertFalse(text.isInsideText());
        assertTrue(virtualText.isInsideText());
        assertTrue(textNode.isInsideText());
    }
}
