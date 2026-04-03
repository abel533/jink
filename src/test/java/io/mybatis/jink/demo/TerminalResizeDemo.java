package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Component;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.style.Color;
import io.mybatis.jink.style.FlexDirection;

/**
 * ink 官方示例 terminal-resize 的 jink 等效实现。
 *
 * <p>实时显示终端列数和行数，调整终端窗口大小即可看到数值更新。
 * 对应 ink 的 useWindowSize()。在 jink 中通过 getColumns()/getRows() 获取。
 *
 * <p>运行:
 * <pre>
 * mvn exec:java -Dexec.mainClass=io.mybatis.jink.demo.TerminalResizeDemo -Dexec.classpathScope=test
 * </pre>
 */
public class TerminalResizeDemo extends Component<Void> {

    public TerminalResizeDemo() {
        super(null);
    }

    @Override
    public Renderable render() {
        int cols = getColumns();
        int rows = getRows();
        return Box.of(
                Text.of("Terminal Size").bold().color(Color.CYAN),
                Text.of("Columns: " + cols),
                Text.of("Rows: " + rows),
                Box.of(
                        Text.of("Resize your terminal to see values update. Press Ctrl+C to exit.").dimmed()
                ).marginTop(1)
        ).flexDirection(FlexDirection.COLUMN).padding(1);
    }

    public static void main(String[] args) {
        Ink.render(new TerminalResizeDemo()).waitUntilExit();
    }
}
