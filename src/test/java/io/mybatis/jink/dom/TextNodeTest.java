package io.mybatis.jink.dom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextNodeTest {

    @Test
    void createTextNode() {
        TextNode node = new TextNode("Hello, World!");
        assertEquals("#text", node.nodeName());
        assertEquals("Hello, World!", node.getNodeValue());
        assertNull(node.getParentNode());
    }

    @Test
    void nullValueTreatedAsEmpty() {
        TextNode node = new TextNode(null);
        assertEquals("", node.getNodeValue());
    }

    @Test
    void setNodeValue() {
        TextNode node = new TextNode("old");
        node.setNodeValue("new");
        assertEquals("new", node.getNodeValue());
    }

    @Test
    void setNodeValueNull() {
        TextNode node = new TextNode("text");
        node.setNodeValue(null);
        assertEquals("", node.getNodeValue());
    }

    @Test
    void toStringTruncation() {
        TextNode shortNode = new TextNode("short");
        assertTrue(shortNode.toString().contains("short"));

        TextNode longNode = new TextNode("This is a very long text that should be truncated");
        assertTrue(longNode.toString().contains("..."));
    }
}
