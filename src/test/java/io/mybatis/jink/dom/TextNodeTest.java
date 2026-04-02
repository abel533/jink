package io.mybatis.jink.dom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextNodeTest {

    @Test
    void createTextNode() {
        var node = new TextNode("Hello, World!");
        assertEquals("#text", node.nodeName());
        assertEquals("Hello, World!", node.getNodeValue());
        assertNull(node.getParentNode());
    }

    @Test
    void nullValueTreatedAsEmpty() {
        var node = new TextNode(null);
        assertEquals("", node.getNodeValue());
    }

    @Test
    void setNodeValue() {
        var node = new TextNode("old");
        node.setNodeValue("new");
        assertEquals("new", node.getNodeValue());
    }

    @Test
    void setNodeValueNull() {
        var node = new TextNode("text");
        node.setNodeValue(null);
        assertEquals("", node.getNodeValue());
    }

    @Test
    void toStringTruncation() {
        var shortNode = new TextNode("short");
        assertTrue(shortNode.toString().contains("short"));

        var longNode = new TextNode("This is a very long text that should be truncated");
        assertTrue(longNode.toString().contains("..."));
    }
}
