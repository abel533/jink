package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Component;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.input.Key;
import io.mybatis.jink.style.FlexDirection;

/**
 * ink 官方示例 cursor-ime 的 jink 等效实现。
 *
 * <p>展示 IME（输入法）输入场景下的光标定位：输入文字时光标跟随到正确位置。
 * 对应 ink 原版的 {@code useCursor} + {@code stringWidth} 计算光标列号。
 * 在 jink 中通过 {@link Component#setCursorPosition(int, int)} 设置光标。
 *
 * <p>注意：宽字符（中文/emoji）占 2 列，此处使用简单长度计算；
 * 精确宽字符宽度需引入 string-width 等效库。
 *
 * <p>运行:
 * <pre>
 * mvn exec:java -Dexec.mainClass=io.mybatis.jink.demo.CursorImeDemo -Dexec.classpathScope=test
 * </pre>
 */
public class CursorImeDemo extends Component<CursorImeDemo.State> {

    private static final String PROMPT = "> ";

    record State(String text) {}

    public CursorImeDemo() {
        super(new State(""));
    }

    @Override
    public Renderable render() {
        State s = getState();
        // Place cursor after the last character in the input line (row 1, after prompt)
        int cursorCol = PROMPT.length() + displayWidth(s.text());
        setCursorPosition(1, cursorCol);
        return Box.of(
                Text.of("Type text (supports CJK/wide chars). Ctrl+C to exit:"),
                Text.of(PROMPT + s.text())
        ).flexDirection(FlexDirection.COLUMN);
    }

    /** Approximate display width: ASCII=1 col, others (CJK/emoji)=2 cols. */
    private static int displayWidth(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            w += isWide(cp) ? 2 : 1;
            i += Character.charCount(cp);
        }
        return w;
    }

    private static boolean isWide(int cp) {
        // CJK Unified Ideographs, Hangul, Fullwidth forms, etc.
        return (cp >= 0x1100 && cp <= 0x115F)
                || (cp >= 0x2E80 && cp <= 0x303E)
                || (cp >= 0x3041 && cp <= 0x33BF)
                || (cp >= 0x33FF && cp <= 0xA4CF)
                || (cp >= 0xA960 && cp <= 0xA97F)
                || (cp >= 0xAC00 && cp <= 0xD7FF)
                || (cp >= 0xF900 && cp <= 0xFAFF)
                || (cp >= 0xFE10 && cp <= 0xFE1F)
                || (cp >= 0xFE30 && cp <= 0xFE6F)
                || (cp >= 0xFF00 && cp <= 0xFF60)
                || (cp >= 0xFFE0 && cp <= 0xFFE6)
                || (cp >= 0x1F300 && cp <= 0x1F9FF);
    }

    @Override
    public void onInput(String input, Key key) {
        State s = getState();
        if (key.backspace() || key.delete()) {
            if (!s.text().isEmpty()) {
                // Remove last code point (handles surrogate pairs)
                String t = s.text();
                int lastCp = t.codePointBefore(t.length());
                setState(new State(t.substring(0, t.length() - Character.charCount(lastCp))));
            }
        } else if (!key.ctrl() && !key.meta() && !input.isEmpty()) {
            setState(new State(s.text() + input));
        }
    }

    public static void main(String[] args) {
        Ink.render(new CursorImeDemo()).waitUntilExit();
    }
}
