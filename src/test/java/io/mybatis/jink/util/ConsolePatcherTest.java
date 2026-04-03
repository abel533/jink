package io.mybatis.jink.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConsolePatcherTest {

    @AfterEach
    void cleanup() {
        ConsolePatcher.restore();
    }

    @Test
    void patchInterceptsSystemOut() {
        List<String> captured = new ArrayList<>();
        PrintStream originalOut = System.out;

        ConsolePatcher.patch(captured::add);
        assertTrue(ConsolePatcher.isPatched());
        assertNotSame(originalOut, System.out);

        System.out.println("hello");
        assertFalse(captured.isEmpty());
        assertTrue(captured.get(0).contains("hello"));

        ConsolePatcher.restore();
        assertSame(originalOut, System.out);
        assertFalse(ConsolePatcher.isPatched());
    }

    @Test
    void patchInterceptsSystemErr() {
        List<String> captured = new ArrayList<>();
        ConsolePatcher.patch(captured::add);

        System.err.println("error msg");
        assertFalse(captured.isEmpty());
        assertTrue(captured.get(0).contains("error msg"));
    }

    @Test
    void getOriginalOutBypassesIntercept() {
        PrintStream originalOut = System.out;
        ConsolePatcher.patch(text -> {});

        assertSame(originalOut, ConsolePatcher.getOriginalOut());
    }

    @Test
    void restoreIsIdempotent() {
        ConsolePatcher.restore();
        ConsolePatcher.restore();
        // 无异常即通过
    }

    @Test
    void multipleListenersReceiveOutput() {
        List<String> captured1 = new ArrayList<>();
        List<String> captured2 = new ArrayList<>();

        ConsolePatcher.patch(captured1::add);
        ConsolePatcher.patch(captured2::add);

        System.out.println("test");
        assertFalse(captured1.isEmpty());
        assertFalse(captured2.isEmpty());
    }
}
