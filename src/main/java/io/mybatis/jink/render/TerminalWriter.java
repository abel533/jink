package io.mybatis.jink.render;

import io.mybatis.jink.ansi.Ansi;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;

/**
 * 差异化终端输出器，对应 ink 的 log-update.ts。
 * 通过比较前后输出内容，只更新变化的行。
 */
public class TerminalWriter {

    private final PrintStream stream;
    private String[] previousLines = new String[0];
    private int previousLineCount = 0;
    private boolean cursorHidden = false;

    public TerminalWriter(PrintStream stream) {
        this.stream = stream;
    }

    public TerminalWriter() {
        this(System.out);
    }

    /**
     * 标准模式渲染：清除旧内容并写入新内容
     */
    public void render(String output) {
        if (!cursorHidden) {
            stream.print(Ansi.CURSOR_HIDE);
            cursorHidden = true;
        }

        String[] nextLines = output.split("\n", -1);

        if (Arrays.equals(nextLines, previousLines)) {
            return; // 无变化
        }

        // 清除旧内容
        if (previousLineCount > 0) {
            stream.print(Ansi.eraseLines(previousLineCount));
        }

        // 写入新内容
        stream.print(output);
        stream.flush();

        previousLines = nextLines;
        previousLineCount = nextLines.length;
    }

    /**
     * 增量模式渲染：只更新变化的行
     */
    public void renderIncremental(String output) {
        if (!cursorHidden) {
            stream.print(Ansi.CURSOR_HIDE);
            cursorHidden = true;
        }

        String[] nextLines = output.split("\n", -1);

        if (Arrays.equals(nextLines, previousLines)) {
            return;
        }

        StringBuilder buffer = new StringBuilder();

        // 回到顶部
        if (previousLineCount > 0) {
            buffer.append(Ansi.cursorUp(previousLineCount - 1));
            buffer.append('\r');
        }

        // 逐行比较更新
        int maxLines = Math.max(nextLines.length, previousLineCount);
        for (int i = 0; i < maxLines; i++) {
            if (i > 0) {
                buffer.append('\n');
            }

            String nextLine = i < nextLines.length ? nextLines[i] : "";
            String prevLine = i < previousLines.length ? previousLines[i] : "";

            if (!nextLine.equals(prevLine)) {
                buffer.append('\r');
                buffer.append(nextLine);
                buffer.append(Ansi.ERASE_END_OF_LINE);
            }
        }

        // 清除多余的旧行
        if (nextLines.length < previousLineCount) {
            for (int i = nextLines.length; i < previousLineCount; i++) {
                buffer.append('\n');
                buffer.append(Ansi.ERASE_LINE);
            }
            // 回到正确位置
            int linesToMoveUp = previousLineCount - nextLines.length;
            buffer.append(Ansi.cursorUp(linesToMoveUp));
        }

        stream.print(buffer);
        stream.flush();

        previousLines = nextLines;
        previousLineCount = nextLines.length;
    }

    /**
     * 清除所有已渲染内容
     */
    public void clear() {
        if (previousLineCount > 0) {
            stream.print(Ansi.eraseLines(previousLineCount));
            stream.flush();
            previousLines = new String[0];
            previousLineCount = 0;
        }
    }

    /**
     * 完成渲染，显示光标
     */
    public void done() {
        if (cursorHidden) {
            stream.print(Ansi.CURSOR_SHOW);
            cursorHidden = true;
        }
        stream.println();
        stream.flush();
    }

    /**
     * 重置状态
     */
    public void reset() {
        previousLines = new String[0];
        previousLineCount = 0;
    }
}
