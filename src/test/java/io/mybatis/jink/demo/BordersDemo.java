package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.style.BorderStyle;
import io.mybatis.jink.style.FlexDirection;

/**
 * ink 官方示例 borders 的 jink 等效实现。
 *
 * <p>ink 原版: {@code render(<Borders />)} — 使用 boxen 风格展示所有边框样式。
 *
 * <p>运行:
 * <pre>
 * mvn exec:java -Dexec.mainClass=io.mybatis.jink.demo.BordersDemo -Dexec.classpathScope=test
 * </pre>
 */
public class BordersDemo {

    static io.mybatis.jink.component.Renderable build() {
        return Box.of(
                Box.of(
                        Box.of(Text.of("single")).borderStyle(BorderStyle.SINGLE).marginRight(2),
                        Box.of(Text.of("double")).borderStyle(BorderStyle.DOUBLE).marginRight(2),
                        Box.of(Text.of("round")).borderStyle(BorderStyle.ROUND).marginRight(2),
                        Box.of(Text.of("bold")).borderStyle(BorderStyle.BOLD)
                ),
                Box.of(
                        Box.of(Text.of("singleDouble")).borderStyle(BorderStyle.SINGLE_DOUBLE).marginRight(2),
                        Box.of(Text.of("doubleSingle")).borderStyle(BorderStyle.DOUBLE_SINGLE).marginRight(2),
                        Box.of(Text.of("classic")).borderStyle(BorderStyle.CLASSIC).marginRight(2),
                        Box.of(Text.of("arrow")).borderStyle(BorderStyle.ARROW)
                ).marginTop(1)
        ).flexDirection(FlexDirection.COLUMN).padding(2);
    }

    public static void main(String[] args) {
        Ink.renderOnce(build(), 80, 24);
    }
}
