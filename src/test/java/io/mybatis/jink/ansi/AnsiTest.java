package io.mybatis.jink.ansi;

import io.mybatis.jink.style.Color;
import io.mybatis.jink.style.Style;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnsiTest {

    @Test
    void cursorMovement() {
        assertEquals("\u001B[3A", Ansi.cursorUp(3));
        assertEquals("\u001B[2B", Ansi.cursorDown(2));
        assertEquals("\u001B[5C", Ansi.cursorForward(5));
        assertEquals("\u001B[1D", Ansi.cursorBackward(1));
        assertEquals("", Ansi.cursorUp(0));
    }

    @Test
    void cursorTo() {
        assertEquals("\u001B[1;1H", Ansi.cursorTo(0, 0));
        assertEquals("\u001B[11;6H", Ansi.cursorTo(5, 10));
    }

    @Test
    void eraseLines() {
        String result = Ansi.eraseLines(3);
        assertTrue(result.contains("\u001B[2K"));
        assertTrue(result.contains("\u001B[1A"));
    }

    @Test
    void styledText() {
        Style boldGreen = Style.builder()
                .bold(true)
                .color(Color.GREEN)
                .build();

        String result = Ansi.styled("Hello", boldGreen);
        assertTrue(result.contains("\u001B[1m"));   // bold
        assertTrue(result.contains("\u001B[32m"));  // green
        assertTrue(result.contains("Hello"));
    }

    @Test
    void noStyleReturnsPlainText() {
        String result = Ansi.styled("Hello", Style.EMPTY);
        assertEquals("Hello", result);
    }
}

class AnsiStringUtilsTest {

    @Test
    void stripAnsi() {
        String colored = "\u001B[32mHello\u001B[0m World";
        assertEquals("Hello World", AnsiStringUtils.stripAnsi(colored));
    }

    @Test
    void visibleWidthAscii() {
        assertEquals(5, AnsiStringUtils.visibleWidth("Hello"));
        assertEquals(5, AnsiStringUtils.visibleWidth("\u001B[32mHello\u001B[0m"));
    }

    @Test
    void visibleWidthChinese() {
        assertEquals(4, AnsiStringUtils.visibleWidth("你好"));
        assertEquals(7, AnsiStringUtils.visibleWidth("你好abc"));
    }

    @Test
    void visibleWidthEmpty() {
        assertEquals(0, AnsiStringUtils.visibleWidth(""));
        assertEquals(0, AnsiStringUtils.visibleWidth(null));
    }

    @Test
    void widestLine() {
        assertEquals(5, AnsiStringUtils.widestLine("Hello\nHi"));
        assertEquals(4, AnsiStringUtils.widestLine("你好\nabc"));
    }

    @Test
    void isWideChar() {
        assertTrue(AnsiStringUtils.isWideChar('你'));
        assertTrue(AnsiStringUtils.isWideChar('好'));
        assertFalse(AnsiStringUtils.isWideChar('A'));
        assertFalse(AnsiStringUtils.isWideChar(' '));
    }

    @Test
    void sliceAnsi() {
        assertEquals("ell", AnsiStringUtils.sliceAnsi("Hello", 1, 4));
        assertEquals("你", AnsiStringUtils.sliceAnsi("你好", 0, 2));
    }
}
