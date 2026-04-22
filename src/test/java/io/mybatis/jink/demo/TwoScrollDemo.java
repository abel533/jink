package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Component;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Scroll;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.dom.ElementNode;
import io.mybatis.jink.input.Key;
import io.mybatis.jink.style.BorderStyle;
import io.mybatis.jink.style.Color;
import io.mybatis.jink.style.FlexDirection;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 多滚动区域综合演示：
 * - 左侧：10 行静态内容，用 ↑↓ 手动滚动
 * - 右侧：30 行静态内容，用 ↑↓ 手动滚动
 * - 底部：自动滚动日志区，每秒追加一条，始终显示最新内容
 * <p>
 * 操作说明：Tab 切换左右焦点，↑↓ 滚动，Ctrl+C 退出
 */
public class TwoScrollDemo extends Component<TwoScrollDemo.State> {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int LOG_VIEWPORT = 4;

    public static final class State {
        /** 当前聚焦的 Scroll 索引（0=左，1=右） */
        public final int focusedIndex;
        /** 日志记录（每秒新增一条） */
        public final List<String> logs;

        public State(int focusedIndex, List<String> logs) {
            this.focusedIndex = focusedIndex;
            this.logs = logs;
        }
    }

    private final Scroll scrollLeft;
    private final Scroll scrollRight;
    private final Scroll scrollLog;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "log-timer");
        t.setDaemon(true);
        return t;
    });

    /** Java 8 兼容的字符串重复方法（String.repeat 是 Java 11+） */
    private static String repeat(String s, int count) {
        StringBuilder sb = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    public TwoScrollDemo() {
        super(new State(0, new ArrayList<>()));

        // 左侧：10 行静态内容，viewport=5
        List<Renderable> leftItems = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            leftItems.add(Text.of(String.format("Left-%02d: %s", i, repeat("■", i))).color(Color.Basic.CYAN));
        }
        Box leftContent = Box.of(leftItems.toArray(new Renderable[0])).flexDirection(FlexDirection.COLUMN);
        this.scrollLeft = leftContent.scroll(5).scrollMode(true);

        // 右侧：30 行静态内容，viewport=5
        List<Renderable> rightItems = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            rightItems.add(Text.of(String.format("Right-%02d: Data-%d", i, i)).color(Color.Basic.YELLOW));
        }
        Box rightContent = Box.of(rightItems.toArray(new Renderable[0])).flexDirection(FlexDirection.COLUMN);
        this.scrollRight = rightContent.scroll(5).scrollMode(false);

        // 日志区：动态内容，autoScrollToBottom，viewport=LOG_VIEWPORT
        // DynamicLogs 每次 toNode() 时从 Demo 的最新状态读取日志
        DynamicLogs logContent = new DynamicLogs(this::getState);
        Box logWrapper = Box.of(logContent).flexDirection(FlexDirection.COLUMN);
        this.scrollLog = logWrapper.scroll(LOG_VIEWPORT).autoScrollToBottom(true);
    }

    /** 动态日志渲染器 — 每次 toNode() 从 Demo 当前状态读取最新日志列表 */
    private static class DynamicLogs implements Renderable {
        private final Supplier<State> stateSupplier;

        DynamicLogs(Supplier<State> stateSupplier) {
            this.stateSupplier = stateSupplier;
        }

        @Override
        public ElementNode toNode() {
            List<String> logs = stateSupplier.get().logs;
            if (logs.isEmpty()) {
                return Text.of("等待日志...").dimmed().toNode();
            }
            List<Renderable> lines = new ArrayList<>(logs.size());
            for (int i = 0; i < logs.size(); i++) {
                Text line = i % 2 == 0
                        ? Text.of(logs.get(i)).color(Color.Basic.WHITE)
                        : Text.of(logs.get(i)).color(Color.Basic.CYAN);
                lines.add(line);
            }
            return Box.of(lines.toArray(new Renderable[0])).flexDirection(FlexDirection.COLUMN).toNode();
        }
    }

    @Override
    public void onMount() {
        // 每秒追加一条日志；Demo 是根组件，setState 会通过 onStateChange 触发重渲染
        scheduler.scheduleAtFixedRate(() -> {
            State s = getState();
            List<String> newLogs = new ArrayList<>(s.logs);
            newLogs.add("[" + LocalTime.now().format(TIME_FMT) + "] 日志 #" + (newLogs.size() + 1));
            setState(new State(s.focusedIndex, newLogs));
        }, 0, 1, TimeUnit.SECONDS);
    }

    @Override
    public void onUnmount() {
        scheduler.shutdownNow();
    }

    @Override
    public Renderable render() {
        State s = getState();
        boolean leftFocused = s.focusedIndex == 0;

        // 左右滚动区并排
        Box leftBox = Box.of(scrollLeft)
                .flexDirection(FlexDirection.COLUMN)
                .borderStyle(leftFocused ? BorderStyle.DOUBLE : BorderStyle.SINGLE)
                .borderColor(leftFocused ? Color.Basic.GREEN : Color.Basic.WHITE)
                .width(36);

        Box rightBox = Box.of(scrollRight)
                .flexDirection(FlexDirection.COLUMN)
                .borderStyle(!leftFocused ? BorderStyle.DOUBLE : BorderStyle.SINGLE)
                .borderColor(!leftFocused ? Color.Basic.GREEN : Color.Basic.WHITE)
                .width(36);

        Box scrollsRow = Box.of(leftBox, rightBox).paddingX(1);

        // 日志区（自动滚动到底部）
        Box logBox = Box.of(scrollLog)
                .flexDirection(FlexDirection.COLUMN)
                .borderStyle(BorderStyle.SINGLE)
                .borderColor(Color.Basic.MAGENTA)
                .width(74)
                .paddingX(1);

        // 帮助文本
        Box help = Box.of(
                Text.of(Text.of(" Tab ").bold().inverse(), Text.of(" 切换焦点").dimmed()),
                Text.of("  "),
                Text.of(Text.of(" ↑↓ ").bold().inverse(), Text.of(" 滚动").dimmed()),
                Text.of("  "),
                Text.of(Text.of(" Ctrl+C ").bold().inverse(), Text.of(" 退出").dimmed())
        ).paddingX(1);

        // 状态栏
        Box status = Box.of(
                Text.of("焦点: " + (leftFocused ? "Left" : "Right")).color(Color.Basic.GREEN),
                Text.of("  "),
                Text.of("Left=" + scrollLeft.getScrollOffset() + "/" + scrollLeft.getContentHeight()).dimmed(),
                Text.of("  "),
                Text.of("Right=" + scrollRight.getScrollOffset() + "/" + scrollRight.getContentHeight()).dimmed(),
                Text.of("  "),
                Text.of("日志数: " + s.logs.size()).color(Color.Basic.MAGENTA)
        ).paddingX(1);

        return Box.of(
                Text.of("=== 多 Scroll 综合演示 ===").bold().color(Color.Basic.MAGENTA),
                scrollsRow,
                Text.of(repeat("── 实时日志（自动追加滚动）─", 3)),
                logBox,
                Text.of(repeat("─", 74)),
                help,
                status
        ).flexDirection(FlexDirection.COLUMN).width(74);
    }

    @Override
    public void onInput(String input, Key key) {
        State s = getState();

        if (key.tab()) {
            int nextIndex = 1 - s.focusedIndex;
            scrollLeft.scrollMode(nextIndex == 0);
            scrollRight.scrollMode(nextIndex == 1);
            setState(new State(nextIndex, s.logs));
            return;
        }

        if (s.focusedIndex == 0) {
            scrollLeft.onInput(input, key);
        } else {
            scrollRight.onInput(input, key);
        }
    }

    public static void main(String[] args) {
        Ink.Instance app = Ink.render(new TwoScrollDemo());
        app.waitUntilExit();
    }
}
