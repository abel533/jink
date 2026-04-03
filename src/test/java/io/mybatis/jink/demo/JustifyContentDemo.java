package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.style.FlexDirection;
import io.mybatis.jink.style.JustifyContent;

/**
 * ink 官方示例 justify-content 的 jink 等效实现。
 *
 * <p>展示所有 justifyContent 对齐方式，每行 20 字符宽度显示 X/Y 元素分布。
 *
 * <p>运行:
 * <pre>
 * mvn exec:java -Dexec.mainClass=io.mybatis.jink.demo.JustifyContentDemo -Dexec.classpathScope=test
 * </pre>
 */
public class JustifyContentDemo {

    static Renderable row(JustifyContent jc, String label) {
        return Box.of(
                Text.of("["),
                Box.of(Text.of("X"), Text.of("Y")).justifyContent(jc).width(20).height(1),
                Text.of("] " + label)
        );
    }

    static Renderable build() {
        return Box.of(
                row(JustifyContent.FLEX_START,   "flex-start"),
                row(JustifyContent.FLEX_END,     "flex-end"),
                row(JustifyContent.CENTER,       "center"),
                row(JustifyContent.SPACE_AROUND, "space-around"),
                row(JustifyContent.SPACE_BETWEEN,"space-between"),
                row(JustifyContent.SPACE_EVENLY, "space-evenly")
        ).flexDirection(FlexDirection.COLUMN);
    }

    public static void main(String[] args) {
        Ink.renderOnce(build(), 80, 24);
    }
}
