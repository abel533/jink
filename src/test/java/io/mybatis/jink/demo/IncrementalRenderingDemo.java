package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Component;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.input.Key;
import io.mybatis.jink.style.BorderStyle;
import io.mybatis.jink.style.Color;
import io.mybatis.jink.style.FlexDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ink 官方示例 incremental-rendering 的 jink 等效实现。
 *
 * <p>展示高频渲染场景：三个进度条（~60fps 更新）、日志行随机更新、
 * 服务列表可用上下键选择，按 q 退出。
 *
 * <p>运行:
 * <pre>
 * mvn exec:java -Dexec.mainClass=io.mybatis.jink.demo.IncrementalRenderingDemo -Dexec.classpathScope=test
 * </pre>
 */
public class IncrementalRenderingDemo extends Component<IncrementalRenderingDemo.State> {

    private static final String[] SERVICES = {
        "Server Authentication Module", "Database Connection Pool", "API Gateway Service",
        "User Profile Manager", "Payment Processing Engine", "Email Notification Queue",
        "File Storage Handler", "Search Indexer Service", "Metrics Aggregation Pipeline",
        "WebSocket Connection Manager"
    };

    private static final String[] ACTIONS = {"PROCESSING", "COMPLETED", "UPDATING", "SYNCING"};
    private static final Random RAND = new Random();

    record State(int p1, int p2, int p3, int counter, int selected, List<String> logs) {}

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "inc-render-timer");
                t.setDaemon(true);
                return t;
            });

    public IncrementalRenderingDemo() {
        super(new State(0, 0, 0, 0, 0, buildInitialLogs()));
    }

    private static List<String> buildInitialLogs() {
        List<String> logs = new ArrayList<>();
        for (int i = 0; i < 4; i++) logs.add(generateLog(i));
        return logs;
    }

    private static String generateLog(int idx) {
        String action = ACTIONS[RAND.nextInt(ACTIONS.length)];
        return String.format("[Worker-%d] %s batch=%d throughput=%dreq/s",
                idx, action, RAND.nextInt(1000), RAND.nextInt(1000));
    }

    @Override
    public void onMount() {
        scheduler.scheduleAtFixedRate(() -> {
            State s = getState();
            List<String> newLogs = new ArrayList<>(s.logs());
            int updateIdx = RAND.nextInt(newLogs.size());
            newLogs.set(updateIdx, generateLog(updateIdx));
            setState(new State(
                    (s.p1() + 1) % 101,
                    (s.p2() + 2) % 101,
                    (s.p3() + 3) % 101,
                    s.counter() + 1,
                    s.selected(),
                    newLogs
            ));
        }, 16, 16, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onUnmount() {
        scheduler.shutdownNow();
    }

    @Override
    public void onInput(String input, Key key) {
        State s = getState();
        if ("q".equals(input)) System.exit(0);
        int sel = s.selected();
        if (key.upArrow())   sel = sel == 0 ? SERVICES.length - 1 : sel - 1;
        if (key.downArrow()) sel = sel == SERVICES.length - 1 ? 0 : sel + 1;
        setState(new State(s.p1(), s.p2(), s.p3(), s.counter(), sel, s.logs()));
    }

    private static String bar(int v) {
        int filled = v / 5;
        return "█".repeat(filled) + "░".repeat(20 - filled);
    }

    @Override
    public Renderable render() {
        State s = getState();

        // Header
        Box header = Box.of(
                Text.of("Incremental Rendering Demo").bold().color(Color.CYAN),
                Text.of("Use ↑/↓ to navigate • Press q to quit • Updates: " + s.counter()).dimmed(),
                Text.of("Progress 1: ", Text.of(bar(s.p1()) + " " + s.p1() + "%").color(Color.GREEN)),
                Text.of("Progress 2: ", Text.of(bar(s.p2()) + " " + s.p2() + "%").color(Color.YELLOW)),
                Text.of("Progress 3: ", Text.of(bar(s.p3()) + " " + s.p3() + "%").color(Color.RED))
        ).flexDirection(FlexDirection.COLUMN)
                .borderStyle(BorderStyle.ROUND).borderColor(Color.CYAN).paddingX(2).paddingY(1);

        // Logs
        List<Renderable> logNodes = new ArrayList<>();
        logNodes.add(Text.of("Live Logs:").bold().color(Color.YELLOW));
        for (String l : s.logs()) logNodes.add(Text.of(l).color(Color.GREEN));
        Box logsBox = Box.of(logNodes.toArray(new Renderable[0]))
                .flexDirection(FlexDirection.COLUMN)
                .borderStyle(BorderStyle.SINGLE).borderColor(Color.YELLOW)
                .paddingX(2).paddingY(1).marginTop(1);

        // Services
        List<Renderable> svcItems = new ArrayList<>();
        svcItems.add(Text.of("System Services Monitor:").bold().color(Color.MAGENTA));
        for (int i = 0; i < SERVICES.length; i++) {
            boolean sel = i == s.selected();
            svcItems.add(Text.of((sel ? "> " : "  ") + SERVICES[i])
                    .color(sel ? Color.BLUE : Color.WHITE));
        }
        Box services = Box.of(svcItems.toArray(new Renderable[0]))
                .flexDirection(FlexDirection.COLUMN)
                .borderStyle(BorderStyle.SINGLE).paddingX(2).paddingY(1).marginTop(1);

        return Box.of(header, logsBox, services).flexDirection(FlexDirection.COLUMN);
    }

    public static void main(String[] args) {
        Ink.render(new IncrementalRenderingDemo()).waitUntilExit();
    }
}
