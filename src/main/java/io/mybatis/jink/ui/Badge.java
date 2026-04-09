package io.mybatis.jink.ui;

import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.dom.ElementNode;

/**
 * 彩色背景标签，用于状态指示。
 * 用法: Badge.of("Pass").color(Badge.Color.GREEN)
 */
public class Badge implements Renderable {

    // 使用全限定名，避免与内部枚举 Color 产生命名冲突
    private static final io.mybatis.jink.style.Color FG_BLACK = io.mybatis.jink.style.Color.ansi256(0);
    private static final io.mybatis.jink.style.Color FG_WHITE = io.mybatis.jink.style.Color.ansi256(15);

    private final String label;
    private io.mybatis.jink.style.Color bgColor = io.mybatis.jink.style.Color.ansi256(27); // 默认 BLUE
    private io.mybatis.jink.style.Color fgColor = FG_WHITE;

    private Badge(String label) {
        this.label = label;
    }

    public static Badge of(String label) {
        return new Badge(label);
    }

    public enum Color {
        BLACK(0), RED(196), GREEN(46), YELLOW(226), BLUE(27),
        MAGENTA(201), CYAN(51), WHITE(15), GRAY(240), LIGHT_GRAY(250);

        private final int code;

        Color(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        /** 亮色背景搭配黑色前景以保证对比度 */
        public boolean isLight() {
            return this == GREEN || this == YELLOW || this == CYAN || this == WHITE || this == LIGHT_GRAY;
        }
    }

    public Badge color(Color color) {
        this.bgColor = io.mybatis.jink.style.Color.ansi256(color.getCode());
        this.fgColor = color.isLight() ? FG_BLACK : FG_WHITE;
        return this;
    }

    @Override
    public ElementNode toNode() {
        return Text.of(" " + label + " ")
                .color(fgColor)
                .backgroundColor(bgColor)
                .toNode();
    }
}
