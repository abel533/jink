package io.mybatis.jink.util;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 拦截 System.out/err，将输出重定向到回调。
 * 对应 ink 的 patchConsole 功能。
 * 在 TUI 模式下，防止 System.out.println() 干扰渲染。
 */
public final class ConsolePatcher {

    private static PrintStream originalOut;
    private static PrintStream originalErr;
    private static boolean patched = false;

    private static final CopyOnWriteArrayList<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    private ConsolePatcher() {}

    /**
     * 启用 console 拦截。所有 System.out/err 输出将通过 listener 回调。
     * @param listener 接收拦截到的文本的回调
     */
    public static synchronized void patch(Consumer<String> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
        if (patched) return;

        originalOut = System.out;
        originalErr = System.err;
        patched = true;

        try {
            System.setOut(new PrintStream(new InterceptOutputStream(), true, "UTF-8"));
            System.setErr(new PrintStream(new InterceptOutputStream(), true, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            // UTF-8 is always supported; this branch is unreachable
            throw new RuntimeException(e);
        }
    }

    /**
     * 恢复原始 System.out/err
     */
    public static synchronized void restore() {
        if (!patched) return;
        System.setOut(originalOut);
        System.setErr(originalErr);
        patched = false;
        listeners.clear();
    }

    /**
     * 获取原始 System.out（绕过拦截）
     */
    public static PrintStream getOriginalOut() {
        return originalOut != null ? originalOut : System.out;
    }

    /**
     * 获取原始 System.err（绕过拦截）
     */
    public static PrintStream getOriginalErr() {
        return originalErr != null ? originalErr : System.err;
    }

    public static boolean isPatched() {
        return patched;
    }

    /**
     * 拦截输出流，将写入内容转发给 listeners
     */
    private static class InterceptOutputStream extends OutputStream {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public void write(int b) {
            char c = (char) b;
            buffer.append(c);
            if (c == '\n') {
                flush();
            }
        }

        @Override
        public void write(byte[] b, int off, int len) {
            String text = new String(b, off, len, StandardCharsets.UTF_8);
            buffer.append(text);
            if (text.contains("\n")) {
                flush();
            }
        }

        @Override
        public void flush() {
            if (buffer.length() == 0) return;
            String text = buffer.toString();
            buffer.setLength(0);
            for (Consumer<String> listener : listeners) {
                try {
                    listener.accept(text);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
