package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.*;
import io.mybatis.jink.input.Key;
import io.mybatis.jink.style.*;
import io.mybatis.jink.util.ConsolePatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * jink 综合功能展示 demo。
 * 通过 4 个标签页展示框架的全部功能特性。
 * <p>
 * 标签页：
 * 1. 布局 - flexDirection, justifyContent, alignItems, flexWrap, gap, 百分比尺寸, Spacer
 * 2. 样式 - 文本效果, 颜色, 边框, textWrap 截断, 背景色
 * 3. 交互 - 计数器(setState), 按键事件显示, patchConsole
 * 4. 高级 - Transform, Static, absolute 定位, overflow:hidden, Newline
 * <p>
 * 操作：1-4 切换标签，+/- 计数器，p 开关 patchConsole，Ctrl+C 退出
 */
public class FeatureShowcase extends Component<FeatureShowcase.State> {

    static final class State {
        private final int tab;
        private final int counter;
        private final String lastKeyName;
        private final String lastInput;
        private final boolean lastCtrl;
        private final boolean lastShift;
        private final boolean lastMeta;
        private final List<String> logs;
        private final boolean consolePatched;
        private final List<String> interceptedLogs;
        State(int tab, int counter, String lastKeyName, String lastInput,
              boolean lastCtrl, boolean lastShift, boolean lastMeta,
              List<String> logs, boolean consolePatched, List<String> interceptedLogs) {
            this.tab = tab;
            this.counter = counter;
            this.lastKeyName = lastKeyName;
            this.lastInput = lastInput;
            this.lastCtrl = lastCtrl;
            this.lastShift = lastShift;
            this.lastMeta = lastMeta;
            this.logs = logs;
            this.consolePatched = consolePatched;
            this.interceptedLogs = interceptedLogs;
        }
        int tab() { return tab; }
        int counter() { return counter; }
        String lastKeyName() { return lastKeyName; }
        String lastInput() { return lastInput; }
        boolean lastCtrl() { return lastCtrl; }
        boolean lastShift() { return lastShift; }
        boolean lastMeta() { return lastMeta; }
        List<String> logs() { return logs; }
        boolean consolePatched() { return consolePatched; }
        List<String> interceptedLogs() { return interceptedLogs; }
    }

    public FeatureShowcase() {
        super(new State(0, 0, "-", "", false, false, false, new ArrayList<>(), false, new ArrayList<>()));
    }

    @Override
    public void onInput(String input, Key key) {
        State s = getState();
        // 解析按键名称
        String keyName = resolveKeyName(input, key);

        // 标签切换
        int newTab = s.tab();
        if ("1".equals(input)) newTab = 0;
        else if ("2".equals(input)) newTab = 1;
        else if ("3".equals(input)) newTab = 2;
        else if ("4".equals(input)) newTab = 3;

        // 计数器操作（Tab 3）
        int newCounter = s.counter();
        if (s.tab() == 2) {
            if (key.upArrow() || "+".equals(input) || "=".equals(input)) newCounter++;
            else if (key.downArrow() || "-".equals(input)) newCounter = Math.max(0, newCounter - 1);
        }

        // patchConsole 开关（Tab 3）
        boolean patched = s.consolePatched();
        if ("p".equals(input) && s.tab() == 2) {
            patched = !patched;
            // 实际调用 ConsolePatcher
            if (patched) {
                ConsolePatcher.patch(text -> {
                    // 把拦截到的输出追加到 interceptedLogs
                    List<String> newLogs = new ArrayList<>(getState().interceptedLogs());
                    newLogs.add(text.trim());
                    while (newLogs.size() > 5) newLogs.remove(0);
                    setState(new State(
                            getState().tab(), getState().counter(), getState().lastKeyName(),
                            getState().lastInput(), getState().lastCtrl(), getState().lastShift(),
                            getState().lastMeta(), getState().logs(), true, newLogs));
                });
                // 演示：主动打一条输出证明拦截已启用
                System.out.println("[Demo] patchConsole 已启用，此行被拦截");
            } else {
                ConsolePatcher.restore();
            }
        }

        // 日志
        List<String> newLogs = new ArrayList<>(s.logs());
        if (newTab != s.tab()) {
            newLogs.add("切换到: " + tabLabel(newTab));
        }
        // 保持日志不超过 5 条
        while (newLogs.size() > 5) newLogs.remove(0);

        setState(new State(newTab, newCounter, keyName, input,
                key.ctrl(), key.shift(), key.meta(), newLogs, patched, new ArrayList<>(s.interceptedLogs())));
    }

    private String resolveKeyName(String input, Key key) {
        if (key.upArrow()) return "↑ Up";
        if (key.downArrow()) return "↓ Down";
        if (key.leftArrow()) return "← Left";
        if (key.rightArrow()) return "→ Right";
        if (key.return_()) return "⏎ Enter";
        if (key.escape()) return "⎋ Escape";
        if (key.tab()) return key.shift() ? "⇧⇥ Shift+Tab" : "⇥ Tab";
        if (key.backspace()) return "⌫ Backspace";
        if (key.delete()) return "⌦ Delete";
        if (key.pageUp()) return "⇞ PageUp";
        if (key.pageDown()) return "⇟ PageDown";
        if (key.home()) return "⇱ Home";
        if (key.end()) return "⇲ End";
        if (key.ctrl() && !input.isEmpty()) return "Ctrl+" + input.toUpperCase();
        if (key.meta() && !input.isEmpty()) return "Alt+" + input;
        if (!input.isEmpty()) return "'" + input + "'";
        return "?";
    }

    private String tabLabel(int tab) {
        switch (tab) {
            case 0: return "布局";
            case 1: return "样式";
            case 2: return "交互";
            case 3: return "高级";
            default: return "?";
        }
    }

    @Override
    public Renderable render() {
        int w = getColumns() > 0 ? getColumns() : 80;
        int h = getRows() > 0 ? getRows() : 24;
        return Box.of(
                renderHeader(w),
                renderBody(w, h - 6), // header 3行 + footer 3行
                renderFooter(w)
        ).flexDirection(FlexDirection.COLUMN)
                .width(w).height(h);
    }

    // ======================== 页头 ========================

    private Renderable renderHeader(int w) {
        State s = getState();
        String[] labels = {"1:布局", "2:样式", "3:交互", "4:高级"};
        List<Renderable> items = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (i == s.tab()) {
                items.add(Text.of(" " + labels[i] + " ").inverse().bold());
            } else {
                items.add(Text.of(" " + labels[i] + " ").dimmed());
            }
        }
        items.add(Spacer.create());
        items.add(Text.of("jink 功能展示").color(Color.CYAN).bold());
        return Box.of(items.toArray(new Renderable[0]))
                .borderStyle(BorderStyle.ROUND)
                .borderColor(Color.CYAN);
    }

    // ======================== 页脚 ========================

    private Renderable renderFooter(int w) {
        return Box.of(
                Text.of(" 1-4").color(Color.BRIGHT_YELLOW).bold(),
                Text.of(" 切换标签  "),
                Text.of("↑↓+/-").color(Color.BRIGHT_YELLOW).bold(),
                Text.of(" 计数器  "),
                Text.of("p").color(Color.BRIGHT_YELLOW).bold(),
                Text.of(" patchConsole  "),
                Text.of("Ctrl+C").color(Color.BRIGHT_YELLOW).bold(),
                Text.of(" 退出")
        ).borderStyle(BorderStyle.SINGLE)
                .borderColor(Color.GRAY);
    }

    // ======================== 页体 ========================

    private Renderable renderBody(int w, int h) {
        Renderable content;
        switch (getState().tab()) {
            case 0: content = renderLayoutTab(w); break;
            case 1: content = renderStyleTab(w); break;
            case 2: content = renderInteractiveTab(w); break;
            case 3: content = renderAdvancedTab(w); break;
            default: content = Box.of(); break;
        }
        return Box.of(content).flexGrow(1);
    }

    // ================================================================
    //  Tab 1: 布局
    // ================================================================

    private Renderable renderLayoutTab(int w) {
        int half = (w - 2) / 2;
        return Box.of(
                // 第一行：flexDirection
                renderFlexDirectionDemo(half),
                // 第二行：justifyContent + alignItems
                renderJustifyDemo(half),
                // 第三行：flexWrap + gap + 百分比 + Spacer
                renderFlexWrapDemo(w - 2),
                renderGapAndPercentDemo(w - 2)
        ).flexDirection(FlexDirection.COLUMN);
    }

    private Renderable renderFlexDirectionDemo(int half) {
        // ROW vs COLUMN 对比
        Renderable rowDemo = Box.of(
                tag("A", Color.RED), tag("B", Color.GREEN), tag("C", Color.BLUE)
        ).borderStyle(BorderStyle.SINGLE).borderColor(Color.GRAY)
                .width(half).height(5);

        Renderable colDemo = Box.of(
                tag("A", Color.RED), tag("B", Color.GREEN), tag("C", Color.BLUE)
        ).flexDirection(FlexDirection.COLUMN)
                .borderStyle(BorderStyle.SINGLE).borderColor(Color.GRAY)
                .width(half).height(5);

        return Box.of(
                Box.of(Text.of("  ROW ").color(Color.BRIGHT_CYAN).bold(), rowDemo)
                        .flexDirection(FlexDirection.COLUMN),
                Box.of(Text.of("  COLUMN ").color(Color.BRIGHT_CYAN).bold(), colDemo)
                        .flexDirection(FlexDirection.COLUMN)
        ).gap(1);
    }

    private Renderable renderJustifyDemo(int half) {
        int barW = half - 4;
        return Box.of(
                Text.of("  justifyContent ").color(Color.BRIGHT_CYAN).bold(),
                Box.of(
                        justifyRow("START", JustifyContent.FLEX_START, barW),
                        justifyRow("CENTER", JustifyContent.CENTER, barW),
                        justifyRow("END", JustifyContent.FLEX_END, barW),
                        justifyRow("BETWEEN", JustifyContent.SPACE_BETWEEN, barW)
                ).flexDirection(FlexDirection.COLUMN)
                        .borderStyle(BorderStyle.SINGLE).borderColor(Color.GRAY)
                        .width(half),
                Text.of("  alignItems ").color(Color.BRIGHT_CYAN).bold(),
                Box.of(
                        Box.of(Text.of("START").dimmed(),
                                Box.of(tag("A", Color.RED), tag("B", Color.GREEN))
                                        .alignItems(AlignItems.FLEX_START).gap(1).height(4)
                                        .borderStyle(BorderStyle.SINGLE).borderColor(Color.GRAY)
                        ).flexDirection(FlexDirection.COLUMN).flexGrow(1),
                        Box.of(Text.of("CENTER").dimmed(),
                                Box.of(tag("A", Color.RED), tag("B", Color.GREEN))
                                        .alignItems(AlignItems.CENTER).gap(1).height(4)
                                        .borderStyle(BorderStyle.SINGLE).borderColor(Color.GRAY)
                        ).flexDirection(FlexDirection.COLUMN).flexGrow(1),
                        Box.of(Text.of("END").dimmed(),
                                Box.of(tag("A", Color.RED), tag("B", Color.GREEN))
                                        .alignItems(AlignItems.FLEX_END).gap(1).height(4)
                                        .borderStyle(BorderStyle.SINGLE).borderColor(Color.GRAY)
                        ).flexDirection(FlexDirection.COLUMN).flexGrow(1)
                ).width(half)
        ).flexDirection(FlexDirection.COLUMN);
    }

    private Renderable justifyRow(String label, JustifyContent jc, int w) {
        return Box.of(
                Text.of(" " + label + ": ").dimmed(),
                Box.of(tag("A", Color.YELLOW), tag("B", Color.MAGENTA), tag("C", Color.CYAN))
                        .justifyContent(jc).flexGrow(1)
        ).flexGrow(1);
    }

    private Renderable renderFlexWrapDemo(int w) {
        return Box.of(
                Text.of(" flexWrap: ").color(Color.BRIGHT_CYAN).bold(),
                Box.of(
                        tag("W1", Color.RED), tag("W2", Color.GREEN),
                        tag("W3", Color.BLUE), tag("W4", Color.YELLOW),
                        tag("W5", Color.MAGENTA), tag("W6", Color.CYAN)
                ).flexWrap(Style.FlexWrap.WRAP).width(20)
                        .borderStyle(BorderStyle.SINGLE).borderColor(Color.GRAY),
                Text.of("  Spacer: ").color(Color.BRIGHT_CYAN).bold(),
                Box.of(
                        Text.of("左").color(Color.GREEN),
                        Spacer.create(),
                        Text.of("右").color(Color.RED)
                ).flexGrow(1)
                        .borderStyle(BorderStyle.SINGLE).borderColor(Color.GRAY)
        );
    }

    private Renderable renderGapAndPercentDemo(int w) {
        return Box.of(
                Text.of(" gap=2: ").color(Color.BRIGHT_CYAN).bold(),
                Box.of(tag("G1", Color.RED), tag("G2", Color.GREEN), tag("G3", Color.BLUE))
                        .gap(2).borderStyle(BorderStyle.SINGLE).borderColor(Color.GRAY),
                Text.of("  50%宽: ").color(Color.BRIGHT_CYAN).bold(),
                Box.of(
                        Box.of(Text.of("50%").color(Color.BRIGHT_GREEN))
                                .widthPercent(50)
                                .borderStyle(BorderStyle.SINGLE).borderColor(Color.GREEN),
                        Box.of(Text.of("50%").color(Color.BRIGHT_BLUE))
                                .widthPercent(50)
                                .borderStyle(BorderStyle.SINGLE).borderColor(Color.BLUE)
                ).flexGrow(1)
        );
    }

    // ================================================================
    //  Tab 2: 样式
    // ================================================================

    private Renderable renderStyleTab(int w) {
        return Box.of(
                renderTextEffects(),
                renderColorSpectrum(),
                renderBorderGallery(w),
                renderPerSideBorderDemo(),
                renderTextWrapDemo(w)
        ).flexDirection(FlexDirection.COLUMN);
    }

    private Renderable renderTextEffects() {
        return Box.of(
                Text.of(" 文本效果: ").color(Color.BRIGHT_CYAN).bold(),
                Text.of("粗体").bold(), Text.of(" "),
                Text.of("斜体").italic(), Text.of(" "),
                Text.of("下划线").underline(), Text.of(" "),
                Text.of("删除线").strikethrough(), Text.of(" "),
                Text.of("暗淡").dimmed(), Text.of(" "),
                Text.of("反转").inverse()
        );
    }

    private Renderable renderColorSpectrum() {
        return Box.of(
                // 基本色
                Box.of(
                        Text.of(" 颜色: ").color(Color.BRIGHT_CYAN).bold(),
                        Text.of("■").color(Color.RED), Text.of("■").color(Color.GREEN),
                        Text.of("■").color(Color.YELLOW), Text.of("■").color(Color.BLUE),
                        Text.of("■").color(Color.MAGENTA), Text.of("■").color(Color.CYAN),
                        Text.of("■").color(Color.WHITE),
                        Text.of("■").color(Color.BRIGHT_RED), Text.of("■").color(Color.BRIGHT_GREEN),
                        Text.of("■").color(Color.BRIGHT_YELLOW), Text.of("■").color(Color.BRIGHT_BLUE),
                        Text.of("■").color(Color.BRIGHT_MAGENTA), Text.of("■").color(Color.BRIGHT_CYAN)
                ),
                // 256色 + RGB
                Box.of(
                        Text.of(" 256色: ").color(Color.BRIGHT_CYAN).bold(),
                        Text.of("██").color(Color.ansi256(196)),
                        Text.of("██").color(Color.ansi256(208)),
                        Text.of("██").color(Color.ansi256(220)),
                        Text.of("██").color(Color.ansi256(46)),
                        Text.of("██").color(Color.ansi256(33)),
                        Text.of("██").color(Color.ansi256(129)),
                        Text.of("  RGB: ").color(Color.BRIGHT_CYAN).bold(),
                        Text.of("██").color(Color.rgb(255, 100, 50)),
                        Text.of("██").color(Color.rgb(50, 200, 100)),
                        Text.of("██").color(Color.hex("#7B68EE"))
                ),
                // 背景色
                Box.of(
                        Text.of(" 背景色: ").color(Color.BRIGHT_CYAN).bold(),
                        Text.of(" 红底 ").backgroundColor(Color.RED).color(Color.WHITE),
                        Text.of(" 绿底 ").backgroundColor(Color.GREEN).color(Color.BLACK),
                        Text.of(" 蓝底 ").backgroundColor(Color.BLUE).color(Color.WHITE),
                        Text.of(" RGB底 ").backgroundColor(Color.rgb(128, 0, 128)).color(Color.WHITE)
                )
        ).flexDirection(FlexDirection.COLUMN);
    }

    private Renderable renderBorderGallery(int w) {
        int boxW = (w - 4) / 4;
        // 两行展示 8 种边框
        return Box.of(
                Text.of(" 边框样式: ").color(Color.BRIGHT_CYAN).bold(),
                Box.of(
                        borderSample("SINGLE", BorderStyle.SINGLE, boxW),
                        borderSample("DOUBLE", BorderStyle.DOUBLE, boxW),
                        borderSample("ROUND", BorderStyle.ROUND, boxW),
                        borderSample("BOLD", BorderStyle.BOLD, boxW)
                ),
                Box.of(
                        borderSample("S+D", BorderStyle.SINGLE_DOUBLE, boxW),
                        borderSample("D+S", BorderStyle.DOUBLE_SINGLE, boxW),
                        borderSample("CLASSIC", BorderStyle.CLASSIC, boxW),
                        borderSample("ARROW", BorderStyle.ARROW, boxW)
                )
        ).flexDirection(FlexDirection.COLUMN);
    }

    private Renderable borderSample(String label, BorderStyle bs, int w) {
        return Box.of(Text.of(label).dimmed()).borderStyle(bs).width(w).height(3);
    }

    private Renderable renderPerSideBorderDemo() {
        return Box.of(
                Text.of(" 每边独立边框色: ").color(Color.BRIGHT_CYAN).bold(),
                Box.of(Text.of("四色边框"))
                        .borderStyle(BorderStyle.ROUND)
                        .borderTopColor(Color.RED)
                        .borderRightColor(Color.GREEN)
                        .borderBottomColor(Color.BLUE)
                        .borderLeftColor(Color.YELLOW)
                        .paddingX(1)
        );
    }

    private Renderable renderTextWrapDemo(int w) {
        // label 和文本分行显示，文本 Box 显式设宽保证截断正确
        String longText = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=[]{}|";
        int boxW = w - 4;
        return Box.of(
                Text.of(" textWrap 截断: ").color(Color.BRIGHT_CYAN).bold(),
                Box.of(
                        Text.of(" END→  ").dimmed(),
                        Box.of(Text.of(longText).wrap(TextWrap.TRUNCATE_END)
                                .color(Color.BRIGHT_YELLOW)).width(boxW - 8)
                ),
                Box.of(
                        Text.of(" =MID= ").dimmed(),
                        Box.of(Text.of(longText).wrap(TextWrap.TRUNCATE_MIDDLE)
                                .color(Color.BRIGHT_GREEN)).width(boxW - 8)
                ),
                Box.of(
                        Text.of("  ←SRT ").dimmed(),
                        Box.of(Text.of(longText).wrap(TextWrap.TRUNCATE_START)
                                .color(Color.BRIGHT_BLUE)).width(boxW - 8)
                )
        ).flexDirection(FlexDirection.COLUMN);
    }

    // ================================================================
    //  Tab 3: 交互
    // ================================================================

    private Renderable renderInteractiveTab(int w) {
        State s = getState();
        int half = (w - 2) / 2;
        return Box.of(
                // 上半部分：计数器 + 按键显示
                Box.of(
                        renderCounter(half, s),
                        renderKeyDisplay(half, s)
                ),
                // 下半部分：日志 + patchConsole 状态
                renderLogArea(w, s)
        ).flexDirection(FlexDirection.COLUMN);
    }

    private Renderable renderCounter(int w, State s) {
        // 构建计数条
        int barLen = Math.min(s.counter(), w - 10);
        StringBuilder barSb = new StringBuilder();
        for (int i = 0; i < barLen; i++) barSb.append("█");
        String bar = barSb.toString();

        return Box.of(
                Text.of("  计数器 (↑↓ 或 +/-)").color(Color.BRIGHT_CYAN).bold(),
                Box.of(
                        Text.of(" 值: ").dimmed(),
                        Text.of(String.valueOf(s.counter())).color(Color.BRIGHT_GREEN).bold(),
                        Text.of("  " + bar).color(Color.GREEN)
                ),
                Box.of(
                        Text.of(" ").dimmed(),
                        progressBlocks(s.counter(), w - 6)
                )
        ).flexDirection(FlexDirection.COLUMN)
                .borderStyle(BorderStyle.ROUND)
                .borderColor(Color.GREEN)
                .width(w);
    }

    private Renderable progressBlocks(int value, int maxW) {
        int filled = Math.min(value, maxW);
        List<Renderable> blocks = new ArrayList<>();
        for (int i = 0; i < filled; i++) {
            // 渐变色彩
            int r = 0, g = (int) (255.0 * i / Math.max(maxW, 1)), b = 255 - g;
            blocks.add(Text.of("█").color(Color.rgb(r, g, b)));
        }
        if (blocks.isEmpty()) {
            blocks.add(Text.of("(按 ↑ 或 + 增加)").dimmed());
        }
        return Box.of(blocks.toArray(new Renderable[0]));
    }

    private Renderable renderKeyDisplay(int w, State s) {
        return Box.of(
                Text.of("  按键事件").color(Color.BRIGHT_CYAN).bold(),
                Box.of(
                        Text.of(" 键名: ").dimmed(),
                        Text.of(s.lastKeyName()).color(Color.BRIGHT_YELLOW).bold()
                ),
                Box.of(
                        Text.of(" 输入: ").dimmed(),
                        Text.of(s.lastInput().isEmpty() ? "(无)" : "'" + s.lastInput() + "'")
                                .color(Color.BRIGHT_WHITE)
                ),
                Box.of(
                        Text.of(" 修饰: ").dimmed(),
                        Text.of(s.lastCtrl() ? " Ctrl " : "      ")
                                .color(s.lastCtrl() ? Color.BLACK : Color.GRAY)
                                .backgroundColor(s.lastCtrl() ? Color.YELLOW : null),
                        Text.of(s.lastShift() ? " Shift " : "       ")
                                .color(s.lastShift() ? Color.BLACK : Color.GRAY)
                                .backgroundColor(s.lastShift() ? Color.GREEN : null),
                        Text.of(s.lastMeta() ? " Meta " : "      ")
                                .color(s.lastMeta() ? Color.BLACK : Color.GRAY)
                                .backgroundColor(s.lastMeta() ? Color.BLUE : null)
                )
        ).flexDirection(FlexDirection.COLUMN)
                .borderStyle(BorderStyle.ROUND)
                .borderColor(Color.YELLOW)
                .width(w);
    }

    private Renderable renderLogArea(int w, State s) {
        List<Renderable> logItems = new ArrayList<>();
        logItems.add(Box.of(
                Text.of("  操作日志").color(Color.BRIGHT_CYAN).bold(),
                Spacer.create(),
                Text.of("patchConsole: ").dimmed(),
                Text.of(s.consolePatched() ? "ON " : "OFF")
                        .color(s.consolePatched() ? Color.GREEN : Color.RED)
                        .bold(),
                Text.of("  (按 p 切换)").dimmed()
        ));

        if (s.logs().isEmpty()) {
            logItems.add(Text.of("  (操作后这里会显示日志)").dimmed());
        } else {
            for (String log : s.logs()) {
                logItems.add(Box.of(
                        Text.of("  > ").color(Color.GRAY),
                        Text.of(log).color(Color.BRIGHT_WHITE)
                ));
            }
        }

        if (s.consolePatched()) {
            logItems.add(Text.of("  拦截的 console 输出:").color(Color.BRIGHT_CYAN).bold());
            if (s.interceptedLogs().isEmpty()) {
                logItems.add(Text.of("  (暂无拦截输出)").dimmed());
            } else {
                for (String log : s.interceptedLogs()) {
                    logItems.add(Text.of("  > " + log).color(Color.BRIGHT_GREEN));
                }
            }
        }

        return Box.of(logItems.toArray(new Renderable[0]))
                .flexDirection(FlexDirection.COLUMN)
                .borderStyle(BorderStyle.SINGLE)
                .borderColor(Color.GRAY)
                .flexGrow(1);
    }

    // ================================================================
    //  Tab 4: 高级特性
    // ================================================================

    private Renderable renderAdvancedTab(int w) {
        int half = (w - 2) / 2;
        return Box.of(
                Box.of(
                        renderTransformDemo(half),
                        renderOverflowDemo(half)
                ),
                Box.of(
                        renderAbsoluteDemo(half),
                        renderNewlineDemo(half)
                ),
                renderStaticDemo(w)
        ).flexDirection(FlexDirection.COLUMN);
    }

    private Renderable renderTransformDemo(int w) {
        return Box.of(
                Text.of(" Transform 变换").color(Color.BRIGHT_CYAN).bold(),
                Transform.of(
                        Text.of("hello jink framework!")
                ).transform((line, idx) -> "  >> " + line.toUpperCase())
        ).flexDirection(FlexDirection.COLUMN)
                .borderStyle(BorderStyle.ROUND)
                .borderColor(Color.MAGENTA)
                .width(w);
    }

    private Renderable renderOverflowDemo(int w) {
        return Box.of(
                Text.of(" overflow: hidden").color(Color.BRIGHT_CYAN).bold(),
                Box.of(
                        Text.of("这段文字超出了容器宽度，被裁剪掉不可见部分")
                                .color(Color.BRIGHT_YELLOW)
                ).overflow(Overflow.HIDDEN).width(20).height(1)
        ).flexDirection(FlexDirection.COLUMN)
                .borderStyle(BorderStyle.ROUND)
                .borderColor(Color.RED)
                .width(w);
    }

    private Renderable renderAbsoluteDemo(int w) {
        return Box.of(
                Text.of(" position: absolute").color(Color.BRIGHT_CYAN).bold(),
                Box.of(
                        Text.of(" (相对内容 flex)").color(Color.GRAY),
                        Box.of(Text.of("[绝对定位]").color(Color.BRIGHT_RED).bold())
                                .position(Position.ABSOLUTE)
                                .posTop(0).posLeft(w - 14)
                ).height(2).width(w - 4)
        ).flexDirection(FlexDirection.COLUMN)
                .borderStyle(BorderStyle.ROUND)
                .borderColor(Color.BLUE)
                .width(w);
    }

    private Renderable renderNewlineDemo(int w) {
        return Box.of(
                Text.of(" Newline 换行").color(Color.BRIGHT_CYAN).bold(),
                Text.of(
                        "第一行", Newline.create(),
                        Text.of("第二行").color(Color.GREEN), Newline.create(),
                        Text.of("第三行").color(Color.BLUE)
                )
        ).flexDirection(FlexDirection.COLUMN)
                .borderStyle(BorderStyle.ROUND)
                .borderColor(Color.CYAN)
                .width(w);
    }

    private Renderable renderStaticDemo(int w) {
        State s = getState();
        List<String> staticItems = new ArrayList<>();
        staticItems.add("框架名称: jink");
        staticItems.add("当前标签: " + tabLabel(s.tab()));
        staticItems.add("计数器值: " + s.counter());

        return Box.of(
                Text.of(" Static 组件 (不可变增量内容)").color(Color.BRIGHT_CYAN).bold(),
                Static.of(staticItems).render((item, idx) ->
                        Box.of(
                                Text.of("  " + (idx + 1) + ". ").color(Color.GRAY),
                                Text.of(item).color(Color.BRIGHT_WHITE)
                        )
                )
        ).flexDirection(FlexDirection.COLUMN)
                .borderStyle(BorderStyle.SINGLE)
                .borderColor(Color.GRAY)
                .flexGrow(1);
    }

    // ======================== 工具方法 ========================

    /**
     * 创建一个小标签块
     */
    private static Renderable tag(String label, Color color) {
        return Text.of(" " + label + " ").color(Color.BLACK).backgroundColor(color);
    }

    // ======================== 启动入口 ========================

    public static void main(String[] args) {
        if (args.length > 0 && "preview".equals(args[0])) {
            int w = args.length > 1 ? Integer.parseInt(args[1]) : 80;
            int h = args.length > 2 ? Integer.parseInt(args[2]) : 24;
            preview(w, h);
            return;
        }
        Ink.Instance app = Ink.render(new FeatureShowcase());
        app.waitUntilExit();
    }

    /**
     * 静态预览模式（不启动终端交互）
     */
    public static void preview(int width, int height) {
        System.out.println("=== Tab 1: 布局 ===");
        FeatureShowcase demo1 = new FeatureShowcase();
        demo1.setTerminalSize(width, height);
        System.out.println(Ink.renderToString(demo1, width, height));

        System.out.println("\n=== Tab 2: 样式 ===");
        FeatureShowcase demo2 = new FeatureShowcase();
        demo2.setTerminalSize(width, height);
        demo2.setState(new State(1, 0, "-", "", false, false, false, new ArrayList<>(), false, new ArrayList<>()));
        System.out.println(Ink.renderToString(demo2, width, height));

        System.out.println("\n=== Tab 3: 交互 ===");
        FeatureShowcase demo3 = new FeatureShowcase();
        demo3.setTerminalSize(width, height);
        demo3.setState(new State(2, 5, "↑ Up", "+", false, false, false,
                Arrays.asList("切换到: 交互", "按下 +"), false, new ArrayList<>()));
        System.out.println(Ink.renderToString(demo3, width, height));

        System.out.println("\n=== Tab 4: 高级 ===");
        FeatureShowcase demo4 = new FeatureShowcase();
        demo4.setTerminalSize(width, height);
        demo4.setState(new State(3, 0, "-", "", false, false, false, new ArrayList<>(), false, new ArrayList<>()));
        System.out.println(Ink.renderToString(demo4, width, height));
    }
}
