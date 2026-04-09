package io.mybatis.jink.ui;

import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.dom.ElementNode;
import io.mybatis.jink.style.Color;

/**
 * 进度条组件。
 * 用法:
 * ProgressBar.of(75)
 * ProgressBar.of(75).width(60)
 * ProgressBar.of(75).label("下载中")
 */
public class ProgressBar implements Renderable {

    private static final Color FILL_COLOR  = Color.ansi256(46);   // 绿色
    private static final Color EMPTY_COLOR = Color.ansi256(238);  // 暗灰
    private static final Color TEXT_COLOR  = Color.ansi256(255);  // 白色

    private static final char FILL_CHAR  = '█';
    private static final char EMPTY_CHAR = '░';

    private final int percent;
    private int width = 40;
    private String label = null;

    private ProgressBar(int percent) {
        this.percent = Math.max(0, Math.min(100, percent));
    }

    public static ProgressBar of(int percent) {
        return new ProgressBar(percent);
    }

    public ProgressBar width(int width) {
        this.width = width;
        return this;
    }

    public ProgressBar label(String label) {
        this.label = label;
        return this;
    }

    @Override
    public ElementNode toNode() {
        int filled = (int) Math.round(width * percent / 100.0);
        int empty  = width - filled;

        StringBuilder fillSb = new StringBuilder();
        for (int i = 0; i < filled; i++) {
            fillSb.append(FILL_CHAR);
        }
        StringBuilder emptySb = new StringBuilder();
        for (int i = 0; i < empty; i++) {
            emptySb.append(EMPTY_CHAR);
        }

        String suffix = "  " + percent + "%";
        if (label != null && !label.isEmpty()) {
            suffix = suffix + " " + label;
        }

        Box bar = Box.of();
        if (filled > 0) {
            bar.add(Text.of(fillSb.toString()).color(FILL_COLOR));
        }
        if (empty > 0) {
            bar.add(Text.of(emptySb.toString()).color(EMPTY_COLOR));
        }
        bar.add(Text.of(suffix).color(TEXT_COLOR));

        return bar.toNode();
    }
}
