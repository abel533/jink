package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.*;
import io.mybatis.jink.style.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ink 官方示例 static 的 jink 等效实现。
 *
 * <p>每 100ms 新增一个已完成的测试条目，使用 Static 组件永久输出（不重渲染），
 * 同时在底部动态显示完成数量。对应 ink 的 {@code <Static items={tests}>} 用法。
 *
 * <p>运行:
 * <pre>
 * mvn exec:java -Dexec.mainClass=io.mybatis.jink.demo.StaticDemo -Dexec.classpathScope=test
 * </pre>
 */
public class StaticDemo extends Component<StaticDemo.State> {

    static final class State {
        private final List<String> tests;
        private final int previousCount;
        State(List<String> tests, int previousCount) {
            this.tests = tests;
            this.previousCount = previousCount;
        }
        List<String> tests() { return tests; }
        int previousCount() { return previousCount; }
    }

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "static-demo-timer");
                t.setDaemon(true);
                return t;
            });

    public StaticDemo() {
        super(new State(new ArrayList<>(), 0));
    }

    @Override
    public void onMount() {
        scheduler.scheduleAtFixedRate(() -> {
            State s = getState();
            if (s.tests().size() >= 10) {
                scheduler.shutdown();
                return;
            }
            int oldSize = s.tests().size();
            List<String> newTests = new ArrayList<>(s.tests());
            newTests.add("Test #" + (newTests.size() + 1));
            setState(new State(newTests, oldSize));
        }, 100, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onUnmount() {
        scheduler.shutdownNow();
    }

    @Override
    public Renderable render() {
        State s = getState();
        return Box.of(
                Static.<String>of(s.tests(), s.previousCount())
                        .render((test, i) ->
                                Box.of(Text.of("✔ " + test).color(Color.GREEN))),
                Box.of(
                        Text.of("Completed tests: " + s.tests().size()).dimmed()
                ).marginTop(1)
        );
    }

    public static void main(String[] args) {
        Ink.render(new StaticDemo()).waitUntilExit();
    }
}
