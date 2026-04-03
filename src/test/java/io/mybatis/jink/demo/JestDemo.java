package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.*;
import io.mybatis.jink.style.Color;
import io.mybatis.jink.style.FlexDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

/**
 * ink 官方示例 jest (jest.tsx + test.tsx + summary.tsx) 的 jink 等效实现。
 *
 * <p>模拟 Jest 测试运行器：最多 4 个测试并发，随机延迟后标记 pass/fail。
 * 已完成测试通过 Static 组件永久输出，运行中测试动态显示，最后打印汇总。
 *
 * <p>运行:
 * <pre>
 * mvn exec:java -Dexec.mainClass=io.mybatis.jink.demo.JestDemo -Dexec.classpathScope=test
 * </pre>
 */
public class JestDemo extends Component<JestDemo.State> {

    record TestResult(String path, String status) {}

    record State(
            List<TestResult> completed,
            List<TestResult> running,
            int staticPrevCount,
            long startTime
    ) {}

    private static final String[] PATHS = {
        "tests/login.js", "tests/signup.js", "tests/forgot-password.js",
        "tests/reset-password.js", "tests/view-profile.js",
        "tests/edit-profile.js", "tests/delete-profile.js",
        "tests/posts.js", "tests/post.js", "tests/comments.js",
    };

    private static final Random RAND = new Random();

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(4, r -> {
                Thread t = new Thread(r, "jest-worker");
                t.setDaemon(true);
                return t;
            });

    public JestDemo() {
        super(new State(new ArrayList<>(), new ArrayList<>(), 0, System.currentTimeMillis()));
    }

    @Override
    public void onMount() {
        Semaphore semaphore = new Semaphore(4);
        for (String path : PATHS) {
            scheduler.execute(() -> {
                try {
                    semaphore.acquire();
                    // Mark as running
                    synchronized (this) {
                        State s = getState();
                        List<TestResult> running = new ArrayList<>(s.running());
                        running.add(new TestResult(path, "runs"));
                        setState(new State(s.completed(), running, s.staticPrevCount(), s.startTime()));
                    }
                    // Simulate test duration
                    Thread.sleep((long) (RAND.nextDouble() * 1000));
                    // Mark as completed
                    synchronized (this) {
                        State s = getState();
                        List<TestResult> running = new ArrayList<>(s.running());
                        running.removeIf(t -> t.path().equals(path));
                        List<TestResult> completed = new ArrayList<>(s.completed());
                        int prevCount = s.completed().size();
                        String status = RAND.nextDouble() < 0.5 ? "pass" : "fail";
                        completed.add(new TestResult(path, status));
                        setState(new State(completed, running, prevCount, s.startTime()));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    semaphore.release();
                }
            });
        }
    }

    @Override
    public void onUnmount() {
        scheduler.shutdownNow();
    }

    static Renderable testRow(TestResult t) {
        Color bg = switch (t.status()) {
            case "runs" -> Color.YELLOW;
            case "pass" -> Color.GREEN;
            case "fail" -> Color.RED;
            default -> Color.DEFAULT;
        };
        String[] parts = t.path().split("/");
        String dir = parts.length > 1 ? parts[0] + "/" : "";
        String file = parts.length > 1 ? parts[1] : t.path();
        return Box.of(
                Text.of(" " + t.status().toUpperCase() + " ").color(Color.BLACK).backgroundColor(bg),
                Box.of(Text.of(dir).dimmed(), Text.of(file).bold()).marginLeft(1)
        );
    }

    @Override
    public Renderable render() {
        State s = getState();
        long elapsedMs = System.currentTimeMillis() - s.startTime();

        // Static — completed tests printed permanently
        Static<TestResult> staticPart = Static.<TestResult>of(s.completed(), s.staticPrevCount())
                .render((t, i) -> testRow(t));

        // Running tests
        List<Renderable> runningNodes = new ArrayList<>();
        runningNodes.add(staticPart);
        if (!s.running().isEmpty()) {
            List<Renderable> runItems = new ArrayList<>();
            for (TestResult t : s.running()) runItems.add(testRow(t));
            runningNodes.add(
                    Box.of(runItems.toArray(new Renderable[0]))
                            .flexDirection(FlexDirection.COLUMN).marginTop(1)
            );
        }

        // Summary
        int passed = (int) s.completed().stream().filter(t -> "pass".equals(t.status())).count();
        int failed = (int) s.completed().stream().filter(t -> "fail".equals(t.status())).count();
        boolean finished = s.running().isEmpty() && s.completed().size() == PATHS.length;

        List<Renderable> summaryParts = new ArrayList<>();
        if (failed > 0) summaryParts.add(Text.of(failed + " failed, ").bold().color(Color.RED));
        if (passed > 0) summaryParts.add(Text.of(passed + " passed, ").bold().color(Color.GREEN));
        summaryParts.add(Text.of((passed + failed) + " total"));

        Box summary = Box.of(
                Box.of(Box.of(Text.of("Test Suites:").bold()).width(14),
                        Box.of(summaryParts.toArray(new Renderable[0]))),
                Box.of(Box.of(Text.of("Time:").bold()).width(14),
                        Text.of(elapsedMs + "ms")),
                finished ? Box.of(Text.of("Ran all test suites.").dimmed()) : Box.of()
        ).flexDirection(FlexDirection.COLUMN).marginTop(1);

        runningNodes.add(summary);
        return Box.of(runningNodes.toArray(new Renderable[0])).flexDirection(FlexDirection.COLUMN);
    }

    public static void main(String[] args) {
        Ink.render(new JestDemo()).waitUntilExit();
    }
}
