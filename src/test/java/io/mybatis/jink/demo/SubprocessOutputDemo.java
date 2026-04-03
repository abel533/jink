package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Component;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.style.Color;
import io.mybatis.jink.style.FlexDirection;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * ink 官方示例 subprocess-output 的 jink 等效实现。
 *
 * <p>使用 ProcessBuilder 执行 {@code java -version}，捕获 stderr 输出并实时显示。
 * 对应 ink 原版的 child_process.spawn + stdout/stderr 流事件。
 *
 * <p>运行:
 * <pre>
 * mvn exec:java -Dexec.mainClass=io.mybatis.jink.demo.SubprocessOutputDemo -Dexec.classpathScope=test
 * </pre>
 */
public class SubprocessOutputDemo extends Component<SubprocessOutputDemo.State> {

    record State(List<String> lines) {}

    public SubprocessOutputDemo() {
        super(new State(new ArrayList<>()));
    }

    @Override
    public void onMount() {
        Thread t = new Thread(() -> {
            try {
                // java -version writes to stderr on most JDKs
                ProcessBuilder pb = new ProcessBuilder("java", "-version");
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                InputStream is = proc.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = reader.readLine()) != null) {
                    State s = getState();
                    List<String> newLines = new ArrayList<>(s.lines());
                    newLines.add(line);
                    setState(new State(newLines));
                }
                proc.waitFor();
                // Add a finish marker
                State s = getState();
                List<String> newLines = new ArrayList<>(s.lines());
                newLines.add("[Process exited with code " + proc.exitValue() + "]");
                setState(new State(newLines));
            } catch (Exception e) {
                State s = getState();
                List<String> newLines = new ArrayList<>(s.lines());
                newLines.add("[Error: " + e.getMessage() + "]");
                setState(new State(newLines));
            }
        }, "subprocess-reader");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public Renderable render() {
        State s = getState();
        List<Renderable> lines = new ArrayList<>();
        lines.add(Text.of("Command: java -version").bold());
        lines.add(Text.of("Output:").color(Color.CYAN));
        if (s.lines().isEmpty()) {
            lines.add(Text.of("  (running...)").dimmed());
        } else {
            for (String line : s.lines()) {
                lines.add(Text.of("  " + line).color(Color.GREEN));
            }
        }
        lines.add(Text.of("Press Ctrl+C to exit.").dimmed());
        return Box.of(lines.toArray(new Renderable[0]))
                .flexDirection(FlexDirection.COLUMN).padding(1);
    }

    public static void main(String[] args) {
        Ink.render(new SubprocessOutputDemo()).waitUntilExit();
    }
}
