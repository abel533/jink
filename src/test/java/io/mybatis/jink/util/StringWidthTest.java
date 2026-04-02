package io.mybatis.jink.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringWidthTest {

    @Test
    void asciiWidth() {
        assertEquals(0, StringWidth.width(""));
        assertEquals(5, StringWidth.width("hello"));
        assertEquals(3, StringWidth.width("abc"));
    }

    @Test
    void cjkWidth() {
        // 每个中文字符占 2 列
        assertEquals(4, StringWidth.width("你好"));
        assertEquals(6, StringWidth.width("汉字宽"));
        assertEquals(16, StringWidth.width("汉字光标位置不对"));
    }

    @Test
    void mixedWidth() {
        // "Hi你好" = 2(H,i) + 4(你,好) = 6
        assertEquals(6, StringWidth.width("Hi你好"));
        // "abc中文def" = 3 + 4 + 3 = 10
        assertEquals(10, StringWidth.width("abc中文def"));
    }

    @Test
    void fullWidthChars() {
        // 全角英文字母
        assertEquals(2, StringWidth.width("Ａ")); // U+FF21
    }

    @Test
    void japaneseKana() {
        // 平假名
        assertEquals(2, StringWidth.width("あ"));
        // 片假名
        assertEquals(2, StringWidth.width("ア"));
    }

    @Test
    void nullSafe() {
        assertEquals(0, StringWidth.width(null));
    }
}
