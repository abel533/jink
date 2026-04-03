package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.style.*;

/**
 * ink 官方示例 box-backgrounds 的 jink 等效实现。
 *
 * <p>展示 Box 的各种背景色用法，包括固定尺寸、边框、内边距、对齐和嵌套继承。
 *
 * <p>运行:
 * <pre>
 * mvn exec:java -Dexec.mainClass=io.mybatis.jink.demo.BoxBackgroundsDemo -Dexec.classpathScope=test
 * </pre>
 */
public class BoxBackgroundsDemo {

    static Renderable build() {
        return Box.of(
                Text.of("Box Background Examples:").bold(),
                Text.of("1. Standard red background (10x3):"),
                Box.of(Text.of("Hello"))
                        .backgroundColor(Color.RED).width(10).height(3),
                Text.of("2. Blue background with border (12x4):"),
                Box.of(Text.of("Border"))
                        .backgroundColor(Color.BLUE).borderStyle(BorderStyle.ROUND)
                        .width(12).height(4),
                Text.of("3. Green background with padding (14x4):"),
                Box.of(Text.of("Padding"))
                        .backgroundColor(Color.GREEN).padding(1).width(14).height(4),
                Text.of("4. Yellow background, centered (16x3):"),
                Box.of(Text.of("Centered"))
                        .backgroundColor(Color.YELLOW).width(16).height(3)
                        .justifyContent(JustifyContent.CENTER),
                Text.of("5. Magenta background, column layout (12x5):"),
                Box.of(Text.of("Line 1"), Text.of("Line 2"))
                        .backgroundColor(Color.MAGENTA).flexDirection(FlexDirection.COLUMN)
                        .width(12).height(5),
                Text.of("6. Hex color background #FF8800 (10x3):"),
                Box.of(Text.of("Hex"))
                        .backgroundColor(Color.hex("FF8800")).width(10).height(3),
                Text.of("7. RGB background rgb(0,255,0) (10x3):"),
                Box.of(Text.of("RGB"))
                        .backgroundColor(Color.rgb(0, 255, 0)).width(10).height(3),
                Text.of("8. Text inheritance test:"),
                Box.of(
                        Text.of("Inherited "),
                        Text.of("Override ").color(Color.WHITE),
                        Text.of("Back to inherited")
                ).backgroundColor(Color.CYAN),
                Text.of("9. Nested background:"),
                Box.of(
                        Text.of("Outer: "),
                        Box.of(Text.of("Inner: "), Text.of("Deep").color(Color.WHITE))
                                .backgroundColor(Color.YELLOW)
                ).backgroundColor(Color.BLUE),
                Text.of("Press Ctrl+C to exit").dimmed()
        ).flexDirection(FlexDirection.COLUMN).gap(1);
    }

    public static void main(String[] args) {
        Ink.renderOnce(build(), 80, 40);
    }
}
