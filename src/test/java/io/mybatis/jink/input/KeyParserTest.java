package io.mybatis.jink.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeyParserTest {

    @Test
    void testArrowKeys() {
        var up = KeyParser.parseEscapeSequence("[A");
        assertEquals("up", up.name());
        assertTrue(up.toKey().upArrow());

        var down = KeyParser.parseEscapeSequence("[B");
        assertEquals("down", down.name());
        assertTrue(down.toKey().downArrow());

        var right = KeyParser.parseEscapeSequence("[C");
        assertEquals("right", right.name());
        assertTrue(right.toKey().rightArrow());

        var left = KeyParser.parseEscapeSequence("[D");
        assertEquals("left", left.name());
        assertTrue(left.toKey().leftArrow());
    }

    @Test
    void testNavigationKeys() {
        assertEquals("home", KeyParser.parseEscapeSequence("[H").name());
        assertEquals("end", KeyParser.parseEscapeSequence("[F").name());
        assertEquals("pageup", KeyParser.parseEscapeSequence("[5~").name());
        assertEquals("pagedown", KeyParser.parseEscapeSequence("[6~").name());
        assertEquals("delete", KeyParser.parseEscapeSequence("[3~").name());
        assertEquals("insert", KeyParser.parseEscapeSequence("[2~").name());
    }

    @Test
    void testFunctionKeys() {
        assertEquals("f1", KeyParser.parseEscapeSequence("OP").name());
        assertEquals("f2", KeyParser.parseEscapeSequence("OQ").name());
        assertEquals("f3", KeyParser.parseEscapeSequence("OR").name());
        assertEquals("f4", KeyParser.parseEscapeSequence("OS").name());
        assertEquals("f5", KeyParser.parseEscapeSequence("[15~").name());
        assertEquals("f12", KeyParser.parseEscapeSequence("[24~").name());
    }

    @Test
    void testControlChars() {
        var enter = KeyParser.parseControlChar('\r');
        assertEquals("return", enter.name());
        assertTrue(enter.toKey().return_());

        var tab = KeyParser.parseControlChar('\t');
        assertEquals("tab", tab.name());
        assertTrue(tab.toKey().tab());

        var backspace = KeyParser.parseControlChar(0x7f);
        assertEquals("backspace", backspace.name());
        assertTrue(backspace.toKey().backspace());

        var escape = KeyParser.parseControlChar(0x1b);
        assertEquals("escape", escape.name());
        assertTrue(escape.toKey().escape());
    }

    @Test
    void testCtrlLetter() {
        var ctrlA = KeyParser.parseControlChar(1);
        assertEquals("a", ctrlA.name());
        assertTrue(ctrlA.ctrl());
        assertTrue(ctrlA.toKey().ctrl());

        var ctrlC = KeyParser.parseControlChar(3);
        assertEquals("c", ctrlC.name());
        assertTrue(ctrlC.ctrl());

        var ctrlZ = KeyParser.parseControlChar(26);
        assertEquals("z", ctrlZ.name());
        assertTrue(ctrlZ.ctrl());
    }

    @Test
    void testMetaLetter() {
        var metaA = KeyParser.parseEscapeSequence("a");
        assertEquals("a", metaA.name());
        assertTrue(metaA.meta());
        assertTrue(metaA.toKey().meta());
    }

    @Test
    void testRegularChars() {
        var a = KeyParser.parseChar('a');
        assertEquals("a", a.name());
        assertFalse(a.ctrl());
        assertFalse(a.meta());
        assertEquals("a", a.inputText());

        var space = KeyParser.parseChar(' ');
        assertEquals(" ", space.name());
        assertEquals(" ", space.inputText());
    }

    @Test
    void testInputText() {
        assertEquals("", KeyParser.parseEscapeSequence("[A").inputText());
        assertEquals("", KeyParser.parseControlChar('\r').inputText());
        assertEquals("", KeyParser.parseControlChar(0x1b).inputText());
        // Ctrl+C: inputText 返回字母（匹配 ink 行为）
        assertEquals("c", KeyParser.parseControlChar(3).inputText());
        assertEquals("x", KeyParser.parseChar('x').inputText());
    }

    @Test
    void testCompleteSequence() {
        assertTrue(KeyParser.isCompleteSequence("[A"));
        assertTrue(KeyParser.isCompleteSequence("[3~"));
        assertTrue(KeyParser.isCompleteSequence("OP"));
        assertTrue(KeyParser.isCompleteSequence("a"));
        assertFalse(KeyParser.isCompleteSequence("["));
        assertFalse(KeyParser.isCompleteSequence("[3"));
    }

    @Test
    void testShiftArrows() {
        var shiftUp = KeyParser.parseEscapeSequence("[1;2A");
        assertEquals("up", shiftUp.name());
        assertTrue(shiftUp.shift());
        assertTrue(shiftUp.toKey().shift());
    }

    @Test
    void testPasteResult() {
        var paste = KeyParser.pasteResult("Hello World\nLine 2");
        assertEquals("paste", paste.name());
        assertTrue(paste.isPaste());
        assertEquals("Hello World\nLine 2", paste.inputText());
        assertFalse(paste.ctrl());
        assertFalse(paste.shift());
        assertFalse(paste.meta());
    }

    @Test
    void testPasteResultEmpty() {
        var paste = KeyParser.pasteResult("");
        assertEquals("paste", paste.name());
        assertTrue(paste.isPaste());
        assertEquals("", paste.inputText());
    }

    @Test
    void testNonPasteResultNotPaste() {
        var arrow = KeyParser.parseEscapeSequence("[A");
        assertFalse(arrow.isPaste());
        var ch = KeyParser.parseChar('a');
        assertFalse(ch.isPaste());
    }

    @Test
    void testF13ToF24ShiftFunctionKeys() {
        // F13 = Shift+F1
        var f13 = KeyParser.parseEscapeSequence("[1;2P");
        assertEquals("f13", f13.name());
        assertTrue(f13.shift());
        assertFalse(f13.ctrl());
        assertEquals("", f13.inputText());

        // F17 = Shift+F5
        var f17 = KeyParser.parseEscapeSequence("[15;2~");
        assertEquals("f17", f17.name());
        assertTrue(f17.shift());

        // F24 = Shift+F12
        var f24 = KeyParser.parseEscapeSequence("[24;2~");
        assertEquals("f24", f24.name());
        assertTrue(f24.shift());
    }

    @Test
    void testF25ToF36CtrlFunctionKeys() {
        // F25 = Ctrl+F1
        var f25 = KeyParser.parseEscapeSequence("[1;5P");
        assertEquals("f25", f25.name());
        assertTrue(f25.ctrl());
        assertFalse(f25.shift());
        assertEquals("", f25.inputText());

        // F29 = Ctrl+F5
        var f29 = KeyParser.parseEscapeSequence("[15;5~");
        assertEquals("f29", f29.name());
        assertTrue(f29.ctrl());

        // F36 = Ctrl+F12
        var f36 = KeyParser.parseEscapeSequence("[24;5~");
        assertEquals("f36", f36.name());
        assertTrue(f36.ctrl());
    }
}
