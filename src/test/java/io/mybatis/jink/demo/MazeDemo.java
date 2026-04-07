package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Component;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.input.Key;
import io.mybatis.jink.style.Color;
import io.mybatis.jink.style.FlexDirection;

import java.util.*;

/**
 * 控制台迷宫游戏 Demo（Java 8 兼容）。
 *
 * <p>功能：
 * <ul>
 *   <li>DFS 随机生成完美迷宫（无环路）</li>
 *   <li>方向键 / WASD 移动玩家 {@code @}</li>
 *   <li>移动轨迹：前进路径（绿色 ·），回退路径（灰色 ·）</li>
 *   <li>到达出口 (E) 后显示成功画面，显示路径步数</li>
 *   <li>快捷键：r 重新生成迷宫，q 退出</li>
 * </ul>
 *
 * <p>迷宫大小自动适配终端窗口尺寸。
 *
 * <p>运行方式：
 * <pre>
 * mvn exec:java -Dexec.mainClass=io.mybatis.jink.demo.MazeDemo -Dexec.classpathScope=test
 * </pre>
 */
public class MazeDemo extends Component<MazeDemo.State> {

    // ===== 状态 =====

    static final class State {
        /** 平铺网格：grid[y][x]=true 表示墙，false 表示可通行 */
        final boolean[][] grid;
        /** 逻辑房间行列数 */
        final int logicRows, logicCols;
        /** 玩家当前逻辑房间坐标 */
        final int playerR, playerC;
        /** 历史路径（不含当前位置），栈结构，末尾=最近一步 */
        final List<int[]> pathHistory;
        /** 已被回退的房间坐标集合（encode(r,c)） */
        final Set<Long> backtrackedSet;
        /** 是否通关 */
        final boolean won;

        State(boolean[][] grid, int logicRows, int logicCols,
              int playerR, int playerC,
              List<int[]> pathHistory, Set<Long> backtrackedSet, boolean won) {
            this.grid = grid;
            this.logicRows = logicRows;
            this.logicCols = logicCols;
            this.playerR = playerR;
            this.playerC = playerC;
            this.pathHistory = pathHistory;
            this.backtrackedSet = backtrackedSet;
            this.won = won;
        }
    }

    // ===== 字段 =====

    private final Random rand = new Random();
    /** 上一次渲染时匹配的目标行列数，用于检测终端 resize */
    private int lastTargetRows = -1;
    private int lastTargetCols = -1;

    // ===== 构造 =====

    public MazeDemo() {
        // 先用小尺寸占位，首次 render() 时根据真实终端尺寸自动重新生成
        super(buildState(4, 9, new Random()));
    }

    // ===== 迷宫生成 =====

    /**
     * 使用随机化 Prim 算法生成迷宫。
     *
     * <p>与 DFS 相比，Prim 产生更多短分支和死胡同，增加难度。
     * 平铺网格尺寸为 (rows*2+1) × (cols*2+1)：
     * <ul>
     *   <li>奇行+奇列 = 房间格（可通行）</li>
     *   <li>奇行+偶列 或 偶行+奇列 = 走廊格（初始为墙，算法打通）</li>
     *   <li>偶行+偶列 = 角格（永远是墙）</li>
     * </ul>
     */
    private static State buildState(int rows, int cols, Random rand) {
        int gRows = rows * 2 + 1, gCols = cols * 2 + 1;
        boolean[][] grid = new boolean[gRows][gCols];
        // 初始全为墙
        for (boolean[] row : grid) Arrays.fill(row, true);
        // 开放所有房间格
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                grid[r * 2 + 1][c * 2 + 1] = false;

        // 随机化 Prim 算法
        boolean[][] inMaze = new boolean[rows][cols];
        List<int[]> frontier = new ArrayList<int[]>();
        inMaze[0][0] = true;
        addFrontier(0, 0, rows, cols, inMaze, frontier);

        int[][] DIRS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        while (!frontier.isEmpty()) {
            int idx = rand.nextInt(frontier.size());
            int[] f = frontier.remove(idx);
            if (inMaze[f[0]][f[1]]) continue; // 已被其他路径加入，跳过

            // 找出所有已在迷宫中的相邻房间
            List<int[]> neighbors = new ArrayList<int[]>();
            for (int[] d : DIRS) {
                int nr = f[0] + d[0], nc = f[1] + d[1];
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && inMaze[nr][nc]) {
                    neighbors.add(new int[]{nr, nc});
                }
            }
            if (!neighbors.isEmpty()) {
                // 随机连接一个已入迷宫的相邻房间，打通走廊
                int[] nb = neighbors.get(rand.nextInt(neighbors.size()));
                grid[f[0] * 2 + 1 + (nb[0] - f[0])][f[1] * 2 + 1 + (nb[1] - f[1])] = false;
                inMaze[f[0]][f[1]] = true;
                addFrontier(f[0], f[1], rows, cols, inMaze, frontier);
            }
        }
        return new State(grid, rows, cols, 0, 0,
                new ArrayList<int[]>(), new HashSet<Long>(), false);
    }

    /** 将房间 (r,c) 的未入迷宫相邻格加入 frontier 列表 */
    private static void addFrontier(int r, int c, int rows, int cols,
                                     boolean[][] inMaze, List<int[]> frontier) {
        int[][] DIRS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] d : DIRS) {
            int nr = r + d[0], nc = c + d[1];
            if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && !inMaze[nr][nc]) {
                frontier.add(new int[]{nr, nc});
            }
        }
    }

    // ===== 生命周期 =====

    @Override
    public void onMount() {}

    @Override
    public void onUnmount() {}

    // ===== 渲染 =====

    @Override
    public Renderable render() {
        int w = getColumns();
        int h = getRows();
        int width = Math.max(50, w > 0 ? w : 80);

        // 自动适配终端尺寸：
        //   总高 = 标题(1) + 迷宫(logicRows*2+1) + 分隔线(1) + 操作栏(1) = logicRows*2+4
        //   → logicRows = (h-5)/2  （留 1 行缓冲，防止 jink 换行）
        //   总宽 = paddingLeft(1) + 迷宫(logicCols*2+1) = logicCols*2+2
        //   → logicCols = (w-2)/2
        if (w > 0 && h > 0) {
            int targetRows = Math.max(3, Math.min(25, (h - 5) / 2));
            int targetCols = Math.max(4, Math.min(45, (w - 2) / 2));
            if (targetRows != lastTargetRows || targetCols != lastTargetCols) {
                lastTargetRows = targetRows;
                lastTargetCols = targetCols;
                State cur = getState();
                if (cur.logicRows != targetRows || cur.logicCols != targetCols) {
                    // setState 在 render() 中调用是安全的：
                    // 当前 render 继续使用 cur，setState 触发下一帧重渲染
                    setState(buildState(targetRows, targetCols, rand));
                }
            }
        }

        State s = getState();
        return Box.of(
                renderHeader(s, width),
                renderMaze(s),
                renderFooter(s, width)
        ).flexDirection(FlexDirection.COLUMN);
    }

    private Renderable renderHeader(State s, int width) {
        String title = " \u25a0 jink Maze";  // ■ jink Maze
        String status = s.won
                ? "  \u2605 SOLVED! \u2605  "  // ★ SOLVED! ★
                : String.format("  (%d,%d) steps:%d  ", s.playerR, s.playerC, s.pathHistory.size());
        int space = Math.max(1, width - title.length() - status.length());
        return Box.of(
                Text.of(title).color(Color.BRIGHT_CYAN).bold(),
                Text.of(rep(" ", space)),
                Text.of(status).color(s.won ? Color.BRIGHT_YELLOW : Color.BRIGHT_BLACK)
        ).backgroundColor(Color.ansi256(17));
    }

    /**
     * 渲染迷宫网格。
     *
     * <p>字符含义：
     * <ul>
     *   <li>{@code \u2588} (█) 墙体</li>
     *   <li>{@code @} 玩家（绿色）</li>
     *   <li>{@code S} 起点（青色）</li>
     *   <li>{@code E} 出口（黄色）</li>
     *   <li>{@code \u00B7} (·) 轨迹：前进=绿，回退=灰</li>
     * </ul>
     */
    private Renderable renderMaze(State s) {
        // 构建路径集合（用于轨迹着色）
        Set<Long> trailSet = new HashSet<Long>();
        for (int[] pos : s.pathHistory) trailSet.add(encode(pos[0], pos[1]));
        // 包含当前位置，用于走廊连续性判断
        Set<Long> trailWithCurrent = new HashSet<Long>(trailSet);
        trailWithCurrent.add(encode(s.playerR, s.playerC));

        int gRows = s.logicRows * 2 + 1, gCols = s.logicCols * 2 + 1;
        Box mazeBox = Box.of().flexDirection(FlexDirection.COLUMN).paddingLeft(1);

        for (int gy = 0; gy < gRows; gy++) {
            Box rowBox = Box.of();
            StringBuilder runBuf = new StringBuilder();
            Color runColor = null;  // null = 无颜色
            boolean runBold = false;

            for (int gx = 0; gx < gCols; gx++) {
                String ch;
                Color color;
                boolean bold = false;

                if (s.grid[gy][gx]) {
                    ch = "\u2588";  // █ 墙体
                    color = Color.BRIGHT_BLACK;
                } else if (gy % 2 == 1 && gx % 2 == 1) {
                    // 房间格
                    int r = gy / 2, c = gx / 2;
                    long code = encode(r, c);
                    if (r == s.playerR && c == s.playerC) {
                        ch = "@"; color = Color.BRIGHT_GREEN; bold = true;
                    } else if (r == s.logicRows - 1 && c == s.logicCols - 1) {
                        ch = "E"; color = Color.BRIGHT_YELLOW; bold = true;
                    } else if (r == 0 && c == 0) {
                        ch = "S"; color = Color.BRIGHT_CYAN;
                    } else if (s.backtrackedSet.contains(code)) {
                        ch = "\u00B7"; color = Color.BRIGHT_BLACK;  // · 灰色回退轨迹
                    } else if (trailSet.contains(code)) {
                        ch = "\u00B7"; color = Color.GREEN;          // · 绿色前进轨迹
                    } else {
                        ch = " "; color = null;
                    }
                } else {
                    // 走廊格（已打通的墙）
                    int type = corridorTrailType(gy, gx, trailWithCurrent, s.backtrackedSet);
                    if (type == 1)      { ch = " "; color = Color.GREEN; }
                    else if (type == 2) { ch = " "; color = Color.BRIGHT_BLACK; }
                    else                { ch = " "; color = null; }
                }

                // 合并连续同色字符，减少 Text 节点数量
                boolean sameStyle = (runColor == color) && (runBold == bold);
                if (sameStyle) {
                    runBuf.append(ch);
                } else {
                    if (runBuf.length() > 0) {
                        Text t = Text.of(runBuf.toString());
                        if (runColor != null) t = t.color(runColor);
                        if (runBold) t = t.bold();
                        rowBox.add(t);
                    }
                    runBuf = new StringBuilder(ch);
                    runColor = color;
                    runBold = bold;
                }
            }
            // 刷新本行最后一段
            if (runBuf.length() > 0) {
                Text t = Text.of(runBuf.toString());
                if (runColor != null) t = t.color(runColor);
                if (runBold) t = t.bold();
                rowBox.add(t);
            }
            mazeBox.add(rowBox);
        }
        return mazeBox;
    }

    /**
     * 判断走廊格的轨迹类型。
     *
     * <p>完美迷宫中，若走廊两端的房间均在前进路径上，则玩家必然经过该走廊。
     *
     * @return 0=无轨迹，1=前进（绿），2=回退（灰）
     */
    private int corridorTrailType(int gy, int gx, Set<Long> trailWithCurrent, Set<Long> backtracked) {
        int r1, c1, r2, c2;
        if (gy % 2 == 1) {
            // 水平走廊（gy 奇，gx 偶）：连接 (gy/2, gx/2-1) 和 (gy/2, gx/2)
            r1 = r2 = gy / 2;
            c1 = gx / 2 - 1;
            c2 = gx / 2;
        } else {
            // 垂直走廊（gy 偶，gx 奇）：连接 (gy/2-1, gx/2) 和 (gy/2, gx/2)
            c1 = c2 = gx / 2;
            r1 = gy / 2 - 1;
            r2 = gy / 2;
        }
        if (r1 < 0 || c1 < 0) return 0;
        long code1 = encode(r1, c1), code2 = encode(r2, c2);
        boolean t1 = trailWithCurrent.contains(code1), t2 = trailWithCurrent.contains(code2);
        boolean b1 = backtracked.contains(code1), b2 = backtracked.contains(code2);
        if (t1 && t2) return 1;        // 两端均为前进路径 → 前进走廊
        if (b1 || b2) return 2;        // 任一端被回退 → 回退走廊
        return 0;
    }

    private Renderable renderFooter(State s, int width) {
        Box footer = Box.of().flexDirection(FlexDirection.COLUMN);
        footer.add(Text.of(rep("\u2500", width)).color(Color.BRIGHT_BLACK));  // ─
        if (s.won) {
            footer.add(Box.of(
                    Text.of("  \u2605 \u606d\u559c\u901a\u5173\uff01\u5171\u8d70 " + s.pathHistory.size()
                            + " \u6b65  \u2605  ").color(Color.BRIGHT_YELLOW).bold(),
                    Text.of("   "),
                    shortcut("r", "\u91cd\u73a9"),
                    Text.of("   "),
                    shortcut("q", "\u9000\u51fa")
            ).paddingX(1));
        } else {
            footer.add(Box.of(
                    shortcut("\u2191\u2193\u2190\u2192", "\u79fb\u52a8"),
                    Text.of("  "),
                    shortcut("WASD", "\u79fb\u52a8"),
                    Text.of("   "),
                    shortcut("r", "\u65b0\u8ff7\u5bab"),
                    Text.of("   "),
                    shortcut("q", "\u9000\u51fa")
            ).paddingX(1));
        }
        return footer;
    }

    private Renderable shortcut(String key, String desc) {
        return Text.of(
                Text.of(" " + key + " ").bold().inverse(),
                Text.of(" " + desc).dimmed()
        );
    }

    // ===== 键盘输入 =====

    @Override
    public void onInput(String input, Key key) {
        if ("q".equals(input)) {
            exit();
            return;
        }

        State s = getState();

        // r 键重新生成同尺寸迷宫
        if ("r".equals(input)) {
            setState(buildState(s.logicRows, s.logicCols, rand));
            return;
        }

        if (s.won) return; // 通关后只响应 r/q

        // 解析移动方向（方向键 + WASD）
        int dr = 0, dc = 0;
        if (key.upArrow()    || "w".equals(input) || "W".equals(input)) dr = -1;
        if (key.downArrow()  || "s".equals(input) || "S".equals(input)) dr =  1;
        if (key.leftArrow()  || "a".equals(input) || "A".equals(input)) dc = -1;
        if (key.rightArrow() || "d".equals(input) || "D".equals(input)) dc =  1;
        if (dr == 0 && dc == 0) return;

        int nr = s.playerR + dr, nc = s.playerC + dc;
        // 边界检查
        if (nr < 0 || nr >= s.logicRows || nc < 0 || nc >= s.logicCols) return;
        // 墙壁检查（走廊格）
        if (s.grid[s.playerR * 2 + 1 + dr][s.playerC * 2 + 1 + dc]) return;

        // 更新路径历史
        List<int[]> newHistory = new ArrayList<int[]>(s.pathHistory);
        Set<Long> newBacktracked = new HashSet<Long>(s.backtrackedSet);
        long nextCode = encode(nr, nc);

        if (!newHistory.isEmpty()) {
            int[] prevPos = newHistory.get(newHistory.size() - 1);
            if (prevPos[0] == nr && prevPos[1] == nc) {
                // 回退：返回上一步的位置，当前位置标记为已回退
                newHistory.remove(newHistory.size() - 1);
                newBacktracked.add(encode(s.playerR, s.playerC));
            } else {
                // 前进：进入新格（若目标格曾被回退，则清除回退标记）
                newBacktracked.remove(nextCode);
                newHistory.add(new int[]{s.playerR, s.playerC});
            }
        } else {
            // 从起点出发的第一步
            newBacktracked.remove(nextCode);
            newHistory.add(new int[]{s.playerR, s.playerC});
        }

        boolean won = (nr == s.logicRows - 1 && nc == s.logicCols - 1);
        setState(new State(s.grid, s.logicRows, s.logicCols,
                nr, nc, newHistory, newBacktracked, won));
    }

    // ===== 辅助方法 =====

    private static long encode(int r, int c) {
        return (long) r * 100000L + c;
    }

    private static String rep(String s, int n) {
        if (n <= 0) return "";
        StringBuilder sb = new StringBuilder(s.length() * n);
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }

    // ===== 入口 =====

    public static void main(String[] args) {
        Ink.render(new MazeDemo()).waitUntilExit();
    }
}
