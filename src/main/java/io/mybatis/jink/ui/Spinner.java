package io.mybatis.jink.ui;

import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Component;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.input.Key;
import io.mybatis.jink.style.Color;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 加载动画组件（Spinner）。
 *
 * <p>通过后台定时任务驱动帧切换，实现流畅动画效果。
 *
 * <p>使用示例：
 * <pre>
 * Spinner spinner = new Spinner.Builder()
 *     .label("正在加载...")
 *     .style(Spinner.Style.DOTS)
 *     .build();
 * </pre>
 *
 * <p>预设动画样式：
 * <ul>
 *   <li>{@link Style#DOTS} — ⣾⣽⣻⢿⡿⣟⣯⣷（默认，80ms/帧）</li>
 *   <li>{@link Style#LINE} — |/-\（120ms/帧）</li>
 *   <li>{@link Style#ARC}  — ◜◠◝◞◡◟（100ms/帧）</li>
 *   <li>{@link Style#STAR} — ✶✸✹✺✹✷（100ms/帧）</li>
 * </ul>
 */
public class Spinner extends Component<Spinner.State> {

    // ── 颜色常量 ───────────────────────────────────────────────────────────────

    private static final Color C_SPIN  = Color.ansi256(51);   // 动画帧（青蓝）
    private static final Color C_LABEL = Color.ansi256(252);  // 标签文字（浅灰）

    // ── 动画样式 ───────────────────────────────────────────────────────────────

    /** 预设动画样式 */
    public enum Style {
        /** Braille 点阵旋转（⣾⣽⣻⢿⡿⣟⣯⣷），80ms/帧 */
        DOTS(new String[]{"⣾", "⣽", "⣻", "⢿", "⡿", "⣟", "⣯", "⣷"}, 80),
        /** 竖线旋转（|/-\），120ms/帧 */
        LINE(new String[]{"|", "/", "-", "\\"}, 120),
        /** 圆弧（◜◠◝◞◡◟），100ms/帧 */
        ARC(new String[]{"◜", "◠", "◝", "◞", "◡", "◟"}, 100),
        /** 星光（✶✸✹✺✹✷），100ms/帧 */
        STAR(new String[]{"✶", "✸", "✹", "✺", "✹", "✷"}, 100);

        final String[] frames;
        final int intervalMs;

        Style(String[] frames, int intervalMs) {
            this.frames     = frames;
            this.intervalMs = intervalMs;
        }
    }

    // ── 状态 ───────────────────────────────────────────────────────────────────

    public static final class State {
        /** 当前帧索引 */
        public final int frameIndex;

        public State(int frameIndex) {
            this.frameIndex = frameIndex;
        }
    }

    // ── 配置 ───────────────────────────────────────────────────────────────────

    private final String style_label;
    private final Style  style;

    private ScheduledExecutorService scheduler;

    // ── 构造 ───────────────────────────────────────────────────────────────────

    private Spinner(Builder builder) {
        super(new State(0));
        this.style_label = builder.label;
        this.style       = builder.style;
    }

    // ── 生命周期 ───────────────────────────────────────────────────────────────

    @Override
    public void onMount() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "jink-spinner");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(
                this::nextFrame,
                0,
                style.intervalMs,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void onUnmount() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private void nextFrame() {
        State s = getState();
        setState(new State((s.frameIndex + 1) % style.frames.length));
    }

    // ── 渲染 ───────────────────────────────────────────────────────────────────

    @Override
    public Renderable render() {
        State s = getState();
        String frame = style.frames[s.frameIndex % style.frames.length];

        if (style_label == null || style_label.isEmpty()) {
            return Text.of(frame).color(C_SPIN);
        }
        return Box.of(
                Text.of(frame).color(C_SPIN),
                Text.of("  " + style_label).color(C_LABEL)
        );
    }

    @Override
    public void onInput(String input, Key key) {
        // Spinner 不处理键盘输入
    }

    // ── Builder ────────────────────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        String label = "";
        Style  style = Style.DOTS;

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        public Spinner build() {
            return new Spinner(this);
        }
    }
}
