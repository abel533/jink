package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.*;
import io.mybatis.jink.input.Key;
import io.mybatis.jink.style.FlexDirection;
import io.mybatis.jink.ui.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * UI 组件库画廊，展示所有纯渲染组件 + Spinner。
 *
 * <p>按 q / Esc 退出。
 *
 * <p>运行:
 * <pre>
 * mvn exec:java -Dexec.mainClass=io.mybatis.jink.demo.UiGallery -Dexec.classpathScope=test
 * </pre>
 */
public class UiGallery extends Component<Integer> {

    private final Spinner spinner = Spinner.builder()
            .label("正在加载组件...")
            .style(Spinner.Style.DOTS)
            .build();

    private ScheduledExecutorService progressScheduler;

    public UiGallery() {
        super(0);
    }

    /** 级联 onStateChange：Spinner 帧切换时也触发父组件重绘 */
    @Override
    public void setOnStateChange(Runnable callback) {
        super.setOnStateChange(callback);
        spinner.setOnStateChange(callback);
    }

    @Override
    public void onMount() {
        spinner.onMount();
        // 每 50ms 进度 +1%，超过 100 回到 0，实现循环动画
        progressScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "jink-progress");
            t.setDaemon(true);
            return t;
        });
        progressScheduler.scheduleAtFixedRate(
                () -> setState((getState() + 1) % 101),
                0, 50, TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void onUnmount() {
        spinner.onUnmount();
        if (progressScheduler != null) {
            progressScheduler.shutdownNow();
        }
    }

    @Override
    public void onInput(String input, Key key) {
        if ("q".equals(input) || key.escape()) {
            exit();
        }
    }

    @Override
    public Renderable render() {
        return Box.of(
                // ── 标题 ─────────────────────────────────────────────────────
                Text.of("UI 组件库画廊").bold(),

                // ── Badge ────────────────────────────────────────────────────
                section("Badge"),
                Box.of(
                        Badge.of("Pass").color(Badge.Color.GREEN),
                        Text.of("  "),
                        Badge.of("Fail").color(Badge.Color.RED),
                        Text.of("  "),
                        Badge.of("Beta").color(Badge.Color.BLUE),
                        Text.of("  "),
                        Badge.of("Warn").color(Badge.Color.YELLOW),
                        Text.of("  "),
                        Badge.of("Info").color(Badge.Color.CYAN)
                ),

                // ── StatusMessage ────────────────────────────────────────────
                section("StatusMessage"),
                StatusMessage.of("操作成功").variant(StatusMessage.Variant.SUCCESS),
                StatusMessage.of("发生错误，请重试").variant(StatusMessage.Variant.ERROR),
                StatusMessage.of("磁盘空间剩余 5%").variant(StatusMessage.Variant.WARNING),
                StatusMessage.of("当前版本 v1.2.3").variant(StatusMessage.Variant.INFO),

                // ── Alert ────────────────────────────────────────────────────
                section("Alert"),
                Alert.of("部署成功！新版本已上线。").variant(Alert.Variant.SUCCESS),
                Alert.of("依赖版本存在安全漏洞，请升级。").variant(Alert.Variant.ERROR),
                Alert.of("配置将在下次重启后生效。").variant(Alert.Variant.WARNING),

                // ── ProgressBar ──────────────────────────────────────────────
                section("ProgressBar"),
                ProgressBar.of(25).label("解压中").width(30),
                ProgressBar.of(60).label("编译中").width(30),
                ProgressBar.of(100).label("完成").width(30),
                section("动态 ProgressBar"),
                ProgressBar.of(getState()).label("循环加载中").width(30),

                // ── UnorderedList ────────────────────────────────────────────
                section("UnorderedList（多层嵌套）"),
                UnorderedList.of()
                        .item("前端")
                        .item("后端", UnorderedList.of()
                                .item("Java")
                                .item("数据库", UnorderedList.of()
                                        .item("MySQL")
                                        .item("Redis"))
                                .item("消息队列"))
                        .item("运维"),

                // ── OrderedList ──────────────────────────────────────────────
                section("OrderedList"),
                OrderedList.of()
                        .item("克隆仓库")
                        .item("安装依赖（mvn install）")
                        .item("配置环境变量")
                        .item("启动服务"),

                // ── Spinner ──────────────────────────────────────────────────
                section("Spinner"),
                spinner,

                // ── 退出提示 ─────────────────────────────────────────────────
                Box.of(Text.of("按 q / Esc 退出").dimmed()).marginTop(1)

        ).flexDirection(FlexDirection.COLUMN);
    }

    // ── 工具方法 ──────────────────────────────────────────────────────────────

    private static Text section(String title) {
        return Text.of("── " + title + " ──").dimmed();
    }

    public static void main(String[] args) {
        Ink.render(new UiGallery()).waitUntilExit();
    }
}
