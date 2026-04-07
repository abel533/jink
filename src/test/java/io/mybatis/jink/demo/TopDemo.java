package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Component;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.input.Key;
import io.mybatis.jink.style.*;

import java.io.*;
import java.lang.management.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

/**
 * 类似 top / htop 的 Java 系统监控 TUI Demo。
 *
 * <p>功能：
 * <ul>
 *   <li>实时显示系统 CPU、内存、JVM 堆、线程数进度条（带颜色告警）</li>
 *   <li>进程列表：PID、名称、CPU%、内存占用，可上下滚动选择</li>
 *   <li>支持按 CPU%、MEM、PID、进程名排序</li>
 *   <li>支持 {@code /} 键实时搜索/过滤进程（名称或 PID）</li>
 *   <li>支持 PageUp/PageDown/Home/End 快速跳转</li>
 *   <li>动态适应终端尺寸</li>
 *   <li>内存信息跨平台获取：Linux(/proc)、macOS(ps)、Windows(tasklist)</li>
 * </ul>
 *
 * <p>运行方式：
 * <pre>
 * mvn exec:java -Dexec.mainClass=io.mybatis.jink.demo.TopDemo -Dexec.classpathScope=test
 * </pre>
 *
 * <p>快捷键：
 * <pre>
 *  q       退出
 *  ↑ / ↓   移动选择
 *  PgUp/Dn 翻页
 *  Home/End 首/末
 *  p       按 CPU% 排序
 *  m       按内存排序
 *  n       按 PID 排序
 *  N       按进程名排序
 *  /       进入搜索模式（实时过滤进程名/PID）
 *  ESC     清除搜索 / 退出搜索输入
 *  r       立即刷新
 * </pre>
 */
@SuppressWarnings("restriction")
public class TopDemo extends Component<TopDemo.AppState> {

    // ===== 枚举 =====

    enum SortField { CPU, MEM, PID, NAME }

    // ===== 数据模型 =====

    /** 系统整体指标快照 */
    static final class SystemInfo {
        private final double cpuPercent;
        private final long totalMemBytes;
        private final long usedMemBytes;
        private final long jvmHeapUsed;
        private final long jvmHeapMax;
        private final int threadCount;
        private final long uptimeSeconds;
        private final int cpuCores;

        SystemInfo(double cpuPercent, long totalMemBytes, long usedMemBytes,
                   long jvmHeapUsed, long jvmHeapMax, int threadCount,
                   long uptimeSeconds, int cpuCores) {
            this.cpuPercent = cpuPercent;
            this.totalMemBytes = totalMemBytes;
            this.usedMemBytes = usedMemBytes;
            this.jvmHeapUsed = jvmHeapUsed;
            this.jvmHeapMax = jvmHeapMax;
            this.threadCount = threadCount;
            this.uptimeSeconds = uptimeSeconds;
            this.cpuCores = cpuCores;
        }

        double getCpuPercent()  { return cpuPercent; }
        long getTotalMemBytes() { return totalMemBytes; }
        long getUsedMemBytes()  { return usedMemBytes; }
        long getJvmHeapUsed()   { return jvmHeapUsed; }
        long getJvmHeapMax()    { return jvmHeapMax; }
        int getThreadCount()    { return threadCount; }
        long getUptimeSeconds() { return uptimeSeconds; }
        int getCpuCores()       { return cpuCores; }
    }

    /** 单个进程快照 */
    static final class ProcessInfo {
        private final long pid;
        private final String name;
        private final double cpuPercent;
        private final long memoryBytes;  // -1 = N/A

        ProcessInfo(long pid, String name, double cpuPercent, long memoryBytes) {
            this.pid = pid;
            this.name = name;
            this.cpuPercent = cpuPercent;
            this.memoryBytes = memoryBytes;
        }

        long getPid()          { return pid; }
        String getName()       { return name; }
        double getCpuPercent() { return cpuPercent; }
        long getMemoryBytes()  { return memoryBytes; }
    }

    /** 完整 UI 状态（不可变，通过 setState 触发重渲染） */
    static final class AppState {
        private final SystemInfo sysInfo;
        private final List<ProcessInfo> processes;
        private final int selectedIndex;
        private final SortField sortField;
        private final String searchQuery;   // 空串 = 不过滤
        private final boolean searchMode;   // true = 正在输入搜索词

        AppState(SystemInfo sysInfo, List<ProcessInfo> processes, int selectedIndex,
                 SortField sortField, String searchQuery, boolean searchMode) {
            this.sysInfo = sysInfo;
            this.processes = processes;
            this.selectedIndex = selectedIndex;
            this.sortField = sortField;
            this.searchQuery = searchQuery;
            this.searchMode = searchMode;
        }

        SystemInfo getSysInfo()          { return sysInfo; }
        List<ProcessInfo> getProcesses() { return processes; }
        int getSelectedIndex()           { return selectedIndex; }
        SortField getSortField()         { return sortField; }
        String getSearchQuery()          { return searchQuery; }
        boolean isSearchMode()           { return searchMode; }
    }

    // ===== 常量 =====

    private static final boolean IS_WINDOWS =
            System.getProperty("os.name", "").toLowerCase().contains("windows");
    private static final boolean IS_LINUX =
            System.getProperty("os.name", "").toLowerCase().contains("linux");

    // ===== 字段 =====

    private final ScheduledExecutorService scheduler;

    private final OperatingSystemMXBean stdOsMBean;
    // com.sun.management 扩展接口（因运行时/权限可能为 null）
    private final com.sun.management.OperatingSystemMXBean extOsMBean;
    private final MemoryMXBean memMBean;
    private final ThreadMXBean threadMBean;
    private final RuntimeMXBean runtimeMBean;

    /** 进程 CPU 双采样：pid -> [cpuSec, wallClockNanos] */
    private final Map<Long, long[]> cpuSamples = new ConcurrentHashMap<Long, long[]>();
    /** 进程物理内存缓存（字节）：由低频后台任务更新 */
    private final Map<Long, Long> memCache = new ConcurrentHashMap<Long, Long>();

    // ===== 构造 =====

    public TopDemo() {
        super(new AppState(
                new SystemInfo(0, 1, 0, 0, 1, 0, 0, Runtime.getRuntime().availableProcessors()),
                Collections.<ProcessInfo>emptyList(),
                0,
                SortField.CPU,
                "",
                false
        ));
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        this.stdOsMBean = os;
        this.extOsMBean = (os instanceof com.sun.management.OperatingSystemMXBean)
                ? (com.sun.management.OperatingSystemMXBean) os : null;
        this.memMBean     = ManagementFactory.getMemoryMXBean();
        this.threadMBean  = ManagementFactory.getThreadMXBean();
        this.runtimeMBean = ManagementFactory.getRuntimeMXBean();
        this.scheduler    = Executors.newScheduledThreadPool(2, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "jink-top-refresh");
                t.setDaemon(true);
                return t;
            }
        });
    }

    // ===== 生命周期 =====

    @Override
    public void onMount() {
        // 主刷新：每秒（CPU、进程列表）
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override public void run() { refresh(); }
        }, 0, 1, TimeUnit.SECONDS);
        // 内存刷新：每5秒（跨平台命令较慢）
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override public void run() { refreshMemory(); }
        }, 0, 5, TimeUnit.SECONDS);
    }

    @Override
    public void onUnmount() {
        scheduler.shutdownNow();
    }

    // ===== 主刷新（每秒）=====

    private void refresh() {
        try {
            // ── 系统 CPU ──────────────────────────────────────────
            double cpuPct = 0;
            if (extOsMBean != null) {
                double load = extOsMBean.getSystemCpuLoad();
                cpuPct = load < 0 ? 0 : load * 100;
            } else {
                // 回退：系统负载平均值 / CPU 核数（Windows 返回 -1）
                double avg = stdOsMBean.getSystemLoadAverage();
                if (avg >= 0) cpuPct = Math.min(100, avg / stdOsMBean.getAvailableProcessors() * 100);
            }

            // ── 系统物理内存 ──────────────────────────────────────
            long totalMem = 0, freeMem = 0;
            if (extOsMBean != null) {
                totalMem = extOsMBean.getTotalPhysicalMemorySize();
                freeMem  = extOsMBean.getFreePhysicalMemorySize();
            }

            // ── JVM 堆内存 ────────────────────────────────────────
            MemoryUsage heap = memMBean.getHeapMemoryUsage();
            long heapMax = heap.getMax() > 0 ? heap.getMax() : heap.getCommitted();

            SystemInfo sysInfo = new SystemInfo(
                    cpuPct, totalMem, totalMem - freeMem,
                    heap.getUsed(), heapMax,
                    threadMBean.getThreadCount(),
                    runtimeMBean.getUptime() / 1000,
                    stdOsMBean.getAvailableProcessors()
            );

            // ── 进程列表 ──────────────────────────────────────────
            List<ProcessInfo> procs = collectProcesses();
            AppState cur = getState();
            procs = sortProcesses(procs, cur.getSortField());
            int newSelIdx = Math.min(cur.getSelectedIndex(), Math.max(0, procs.size() - 1));

            setState(new AppState(sysInfo, procs, newSelIdx,
                    cur.getSortField(), cur.getSearchQuery(), cur.isSearchMode()));
        } catch (Exception ignored) {}
    }

    private List<ProcessInfo> collectProcesses() {
        List<ProcessInfo> result = new ArrayList<ProcessInfo>();
        try {
            if (IS_WINDOWS) {
                collectProcessesWindows(result);
            } else {
                collectProcessesUnix(result);
            }
        } catch (Exception ignored) {}

        // 清理已退出进程的历史采样
        Set<Long> alive = new HashSet<Long>();
        for (ProcessInfo p : result) alive.add(p.getPid());
        cpuSamples.keySet().retainAll(alive);

        return result;
    }

    /** Linux/macOS：ps -A -o pid=,comm=,%cpu=,rss= */
    private void collectProcessesUnix(List<ProcessInfo> result) throws Exception {
        Process proc = Runtime.getRuntime().exec(new String[]{"ps", "-A", "-o", "pid=,comm=,%cpu=,rss="});
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\s+", 4);
                if (parts.length < 4) continue;
                try {
                    long pid = Long.parseLong(parts[0]);
                    String name = parts[1];
                    if (name.endsWith(".exe")) name = name.substring(0, name.length() - 4);
                    double cpu = Double.parseDouble(parts[2]);
                    long mem = Long.parseLong(parts[3]) * 1024L; // KB → bytes
                    memCache.put(pid, mem);
                    result.add(new ProcessInfo(pid, name, cpu, mem));
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    /** Windows：tasklist /V /FO CSV /NH 双采样 CPU */
    private void collectProcessesWindows(List<ProcessInfo> result) throws Exception {
        long nowNanos = System.nanoTime();
        Process proc = Runtime.getRuntime().exec("tasklist /V /FO CSV /NH");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                // "java.exe","12345","Console","1","256,128 K","Running","user","0:00:01","Title"
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (parts.length < 8) continue;
                try {
                    String nameRaw = parts[0].replace("\"", "").trim();
                    String name = nameRaw.endsWith(".exe") ? nameRaw.substring(0, nameRaw.length() - 4) : nameRaw;
                    long pid = Long.parseLong(parts[1].replace("\"", "").trim());
                    String cpuTimeStr = parts[7].replace("\"", "").trim();
                    long cpuSec = parseCpuTime(cpuTimeStr);
                    String memStr = parts[4].replaceAll("[\"\\sK,]", "").trim();
                    long memBytes = memStr.isEmpty() ? -1L : Long.parseLong(memStr) * 1024L;

                    double cpuPct = 0;
                    if (cpuSec >= 0) {
                        long[] prev = cpuSamples.get(pid);
                        if (prev != null && cpuSec >= prev[0]) {
                            long deltaCpu = (cpuSec - prev[0]) * 1_000_000_000L;
                            long deltaWall = nowNanos - prev[1];
                            if (deltaWall > 0) cpuPct = (double) deltaCpu / deltaWall * 100.0;
                        }
                        cpuSamples.put(pid, new long[]{cpuSec, nowNanos});
                    }

                    result.add(new ProcessInfo(pid, name, cpuPct, memBytes));
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    /** 解析 "H:MM:SS" 格式 CPU Time → 秒数 */
    private static long parseCpuTime(String s) {
        try {
            String[] parts = s.split(":");
            if (parts.length == 3) {
                return Long.parseLong(parts[0]) * 3600 + Long.parseLong(parts[1]) * 60 + Long.parseLong(parts[2]);
            }
        } catch (NumberFormatException ignored) {}
        return -1;
    }

    // ===== 内存刷新（每5秒，跨平台）=====

    private void refreshMemory() {
        try {
            if (IS_LINUX) {
                refreshMemoryLinux();
            } else if (IS_WINDOWS) {
                refreshMemoryWindows();
            } else {
                refreshMemoryMac();
            }
        } catch (Exception ignored) {}
    }

    /** Linux：读取 /proc/&lt;pid&gt;/status 的 VmRSS 字段（物理内存） */
    private void refreshMemoryLinux() {
        File procDir = new File("/proc");
        if (!procDir.exists()) return;
        File[] pidDirs = procDir.listFiles(new FileFilter() {
            @Override public boolean accept(File f) { return f.isDirectory(); }
        });
        if (pidDirs == null) return;

        for (File pidDir : pidDirs) {
            try {
                final long pid = Long.parseLong(pidDir.getName());
                Path statusPath = pidDir.toPath().resolve("status");
                if (!Files.exists(statusPath)) continue;

                Files.lines(statusPath)
                        .filter(l -> l.startsWith("VmRSS:"))
                        .findFirst()
                        .ifPresent(l -> {
                            try {
                                String[] parts = l.split("\\s+");
                                if (parts.length >= 2) {
                                    memCache.put(pid, Long.parseLong(parts[1]) * 1024L);
                                }
                            } catch (NumberFormatException ignored2) {}
                        });
            } catch (Exception ignored) {}
        }
    }

    /** Windows：tasklist /FO CSV /NH 批量获取工作集大小 */
    private void refreshMemoryWindows() throws Exception {
        Process proc = Runtime.getRuntime().exec("tasklist /FO CSV /NH");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(proc.getInputStream()))) {
            // 输出格式: "image.exe","pid","Session","#","Mem Usage"
            // 示例:    "java.exe","12345","Console","1","256,128 K"
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                // 按引号外的逗号切分
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (parts.length >= 5) {
                    try {
                        long pid = Long.parseLong(parts[1].replaceAll("\"", "").trim());
                        // "256,128 K" -> 去除引号/空格/逗号/K 后取数字
                        String memStr = parts[4].replaceAll("[\"\\sK,]", "").trim();
                        if (!memStr.isEmpty()) {
                            memCache.put(pid, Long.parseLong(memStr) * 1024L);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    /** macOS / 其他 Unix：ps -A -o pid,rss 批量获取（RSS 单位 KB） */
    private void refreshMemoryMac() throws Exception {
        Process proc = Runtime.getRuntime().exec(new String[]{"ps", "-A", "-o", "pid,rss"});
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(proc.getInputStream()))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) { first = false; continue; } // 跳过表头
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 2) {
                    try {
                        memCache.put(Long.parseLong(parts[0]),
                                Long.parseLong(parts[1]) * 1024L);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    // ===== 排序 =====

    private List<ProcessInfo> sortProcesses(List<ProcessInfo> procs, SortField field) {
        Comparator<ProcessInfo> cmp;
        if (field == SortField.MEM) {
            cmp = new Comparator<ProcessInfo>() {
                @Override public int compare(ProcessInfo a, ProcessInfo b) {
                    return Long.compare(b.getMemoryBytes(), a.getMemoryBytes());
                }
            };
        } else if (field == SortField.PID) {
            cmp = new Comparator<ProcessInfo>() {
                @Override public int compare(ProcessInfo a, ProcessInfo b) {
                    return Long.compare(a.getPid(), b.getPid());
                }
            };
        } else if (field == SortField.NAME) {
            cmp = new Comparator<ProcessInfo>() {
                @Override public int compare(ProcessInfo a, ProcessInfo b) {
                    return a.getName().compareToIgnoreCase(b.getName());
                }
            };
        } else { // CPU (default)
            cmp = new Comparator<ProcessInfo>() {
                @Override public int compare(ProcessInfo a, ProcessInfo b) {
                    return Double.compare(b.getCpuPercent(), a.getCpuPercent());
                }
            };
        }
        return procs.stream().sorted(cmp).collect(Collectors.toList());
    }

    // ===== 搜索过滤 =====

    private List<ProcessInfo> filteredProcesses(AppState s) {
        final String q = s.getSearchQuery().toLowerCase();
        if (q.isEmpty()) return s.getProcesses();
        return s.getProcesses().stream()
                .filter(p -> p.getName().toLowerCase().contains(q)
                        || String.valueOf(p.getPid()).contains(q))
                .collect(Collectors.toList());
    }

    // ===== 工具方法 =====

    private static String rep(String s, int n) {
        if (n <= 0) return "";
        StringBuilder sb = new StringBuilder(s.length() * n);
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }

    // ===== 渲染 =====

    @Override
    public Renderable render() {
        AppState s = getState();
        int width  = Math.max(80, getColumns());
        int height = Math.max(20, getRows());

        // 固定行：标题(1)+分隔(1)+4进度条(4)+分隔(1)+表头(1)+摘要行(1)+底部分隔(1)+底部(1)=11
        int fixedRows  = 11;
        int listHeight = Math.max(1, height - fixedRows);

        return Box.of(
                renderHeader(s, width),
                renderSeparator(width),
                renderStats(s, width),
                renderSeparator(width),
                renderProcessTable(s, width, listHeight),
                renderFooter(s, width)
        ).flexDirection(FlexDirection.COLUMN);
    }

    /** 顶部标题栏（含运行时间与当前时刻） */
    private Renderable renderHeader(AppState s, int width) {
        // 注意：不使用 emoji，避免终端字符宽度计算偏差（emoji 在部分终端占 2 列）
        String title = " >> jink-top - Java System Monitor";
        long up = s.getSysInfo().getUptimeSeconds();
        String upStr = String.format("  up %02d:%02d:%02d  ", up / 3600, (up % 3600) / 60, up % 60);
        String time  = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " ";
        int space = Math.max(1, width - title.length() - upStr.length() - time.length());

        return Box.of(
                Text.of(title).color(Color.BRIGHT_CYAN).bold(),
                Text.of(rep(" ", space)),
                Text.of(upStr).color(Color.BRIGHT_BLACK).dimmed(),
                Text.of(time).color(Color.BRIGHT_WHITE).dimmed()
        ).backgroundColor(Color.ansi256(17));
    }

    /**
     * 资源进度条区域。
     *
     * <p>barWidth 计算：总宽 - paddingLeft(1) - label(4) - "["(1) - "] "(2) - 最长右侧信息(14) - 余量(8)
     *                 = width - 30
     * <p>最长信息示例：MEM "100.0G/100.0G" = 13 chars；保守地多留 8 char 余量，确保不换行
     */
    private Renderable renderStats(AppState s, int width) {
        SystemInfo sys = s.getSysInfo();
        // 保守计算：多留余量，确保进度条右侧信息不会触发换行
        int barWidth = Math.max(10, width - 30);

        double memPct = sys.getTotalMemBytes() > 0
                ? (double) sys.getUsedMemBytes() / sys.getTotalMemBytes() * 100 : 0;
        double jvmPct = sys.getJvmHeapMax() > 0
                ? (double) sys.getJvmHeapUsed() / sys.getJvmHeapMax() * 100 : 0;
        double thdPct = Math.min(100, sys.getThreadCount() / 2.0);  // 200 threads ≈ 100%

        // 紧凑格式：最长不超过 14 chars
        String cpuInfo = String.format("%.1f%%(%dc)", sys.getCpuPercent(), sys.getCpuCores());
        String memInfo = sys.getTotalMemBytes() > 0
                ? String.format("%.1fG/%.1fG", sys.getUsedMemBytes() / 1e9, sys.getTotalMemBytes() / 1e9)
                : "N/A";
        String jvmInfo = String.format("%.0fM/%.0fM", sys.getJvmHeapUsed() / 1e6, sys.getJvmHeapMax() / 1e6);
        String thdInfo = sys.getThreadCount() + " threads";

        return Box.of(
                progressBar("CPU ", sys.getCpuPercent(), barWidth, cpuInfo),
                progressBar("MEM ", memPct,              barWidth, memInfo),
                progressBar("JVM ", jvmPct,              barWidth, jvmInfo),
                progressBar("THD ", thdPct,              barWidth, thdInfo)
        ).flexDirection(FlexDirection.COLUMN).paddingLeft(1);
    }

    /**
     * 单条进度条。
     * 颜色编码：绿(&lt;50%) → 黄(&lt;80%) → 红(≥80%)
     */
    private Renderable progressBar(String label, double percent, int barWidth, String info) {
        percent = Math.max(0, Math.min(100, percent));
        int filled = (int) (percent / 100 * barWidth);
        int empty  = Math.max(0, barWidth - filled);

        Color barColor = percent < 50 ? Color.GREEN
                       : percent < 80 ? Color.YELLOW
                       : Color.RED;

        return Box.of(
                Text.of(label).color(Color.BRIGHT_WHITE).bold(),
                Text.of("[").color(Color.BRIGHT_BLACK),
                Text.of(rep("█", filled)).color(barColor),
                Text.of(rep("░", empty)).color(Color.BRIGHT_BLACK),
                Text.of("] ").color(Color.BRIGHT_BLACK),
                Text.of(info).color(barColor)
        );
    }

    private Renderable renderSeparator(int width) {
        return Text.of(rep("─", width)).color(Color.BRIGHT_BLACK);
    }

    /** 进程列表（含搜索过滤 + 虚拟滚动） */
    private Renderable renderProcessTable(AppState s, int width, int listHeight) {
        List<ProcessInfo> procs = filteredProcesses(s);
        int total  = procs.size();
        int selIdx = Math.min(s.getSelectedIndex(), Math.max(0, total - 1));

        // 计算滚动偏移（保证选中行可见）
        int scroll = 0;
        if (selIdx >= listHeight) scroll = selIdx - listHeight + 1;
        int end = Math.min(scroll + listHeight, total);

        // 列宽：PID(7) + CPU(9) + MEM(9) + NAME(剩余)
        int pidW  = 7;
        int cpuW  = 9;
        int memW  = 9;
        int nameW = Math.max(10, width - pidW - cpuW - memW - 5);

        // 排序标记 ▲
        String pidH  = s.getSortField() == SortField.PID  ? "PID ▲"  : "PID";
        String nameH = s.getSortField() == SortField.NAME ? "NAME ▲" : "NAME";
        String cpuH  = s.getSortField() == SortField.CPU  ? "CPU% ▲" : "CPU%";
        String memH  = s.getSortField() == SortField.MEM  ? "MEM ▲"  : "MEM";

        Box table = Box.of(
                Box.of(
                        Text.of(String.format("%-" + pidW  + "s ", pidH)).bold(),
                        Text.of(String.format("%-" + nameW + "s ", nameH)).bold(),
                        Text.of(String.format("%-" + cpuW  + "s ", cpuH)).bold(),
                        Text.of(String.format("%-" + memW  + "s",  memH)).bold()
                ).backgroundColor(Color.ansi256(238))
        ).flexDirection(FlexDirection.COLUMN);

        for (int i = scroll; i < end; i++) {
            ProcessInfo p = procs.get(i);
            boolean sel   = (i == selIdx);

            String pidStr  = String.format("%-" + pidW + "d ", p.getPid());
            String rawName = p.getName();
            String nameStr = rawName.length() > nameW
                    ? rawName.substring(0, nameW - 1) + "…"
                    : String.format("%-" + nameW + "s ", rawName);
            String cpuStr  = String.format("%7.1f%% ", Math.min(9999.9, p.getCpuPercent()));
            String memStr  = p.getMemoryBytes() > 0 ? formatMem(p.getMemoryBytes()) : "   N/A   ";

            Color cpuColor = p.getCpuPercent() > 50 ? Color.RED
                           : p.getCpuPercent() > 10 ? Color.YELLOW
                           : Color.GREEN;

            Box row = Box.of(
                    Text.of(pidStr).color(sel  ? Color.BLACK : Color.BRIGHT_WHITE),
                    Text.of(nameStr).color(sel ? Color.BLACK : Color.WHITE),
                    Text.of(cpuStr).color(sel  ? Color.BLACK : cpuColor),
                    Text.of(memStr).color(sel  ? Color.BLACK : Color.CYAN)
            );
            if (sel) row.backgroundColor(Color.BRIGHT_CYAN);
            table.add(row);
        }

        // 状态摘要行
        String totalLabel = s.getSearchQuery().isEmpty() ? total + " processes" : total + " matched";
        String summary = String.format(" %s  selected: %d/%d  lines: %d-%d ",
                totalLabel, selIdx + 1, total, scroll + 1, end);
        table.add(Text.of(summary).color(Color.BRIGHT_BLACK).dimmed());

        return table;
    }

    /** 格式化内存大小 → 固定9字符宽度 */
    private String formatMem(long bytes) {
        if (bytes >= 1_073_741_824L) {
            return String.format("%7.1fG ", bytes / 1_073_741_824.0);
        } else if (bytes >= 1_048_576L) {
            return String.format("%7.1fM ", bytes / 1_048_576.0);
        } else if (bytes >= 1024L) {
            return String.format("%7.1fK ", bytes / 1024.0);
        }
        return String.format("%7dB ", bytes);
    }

    /** 底部栏：普通模式显示快捷键，搜索模式显示输入框 */
    private Renderable renderFooter(AppState s, int width) {
        if (s.isSearchMode()) {
            return Box.of(
                    renderSeparator(width),
                    Box.of(
                            Text.of("/").color(Color.BRIGHT_YELLOW).bold(),
                            Text.of(" Search: ").color(Color.BRIGHT_WHITE),
                            Text.of(s.getSearchQuery()).color(Color.BRIGHT_YELLOW),
                            Text.of("█").color(Color.BRIGHT_YELLOW),  // 模拟光标
                            Text.of("  ESC=取消  Enter=确认").color(Color.BRIGHT_BLACK).dimmed()
                    ).paddingX(1)
            ).flexDirection(FlexDirection.COLUMN);
        }

        String sortLabelVal;
        if (s.getSortField() == SortField.MEM) sortLabelVal = "MEM";
        else if (s.getSortField() == SortField.PID) sortLabelVal = "PID";
        else if (s.getSortField() == SortField.NAME) sortLabelVal = "Name";
        else sortLabelVal = "CPU%";
        String sortLabel = "Sort: " + sortLabelVal;
        String filterHint = s.getSearchQuery().isEmpty() ? "" : "  [/" + s.getSearchQuery() + "]";

        return Box.of(
                renderSeparator(width),
                Box.of(
                        shortcut("q", "Quit"),
                        Text.of("  "),
                        shortcut("↑↓", "Move"),
                        Text.of("  "),
                        shortcut("p", "CPU"),
                        Text.of(" "),
                        shortcut("m", "MEM"),
                        Text.of(" "),
                        shortcut("n", "PID"),
                        Text.of(" "),
                        shortcut("N", "Name"),
                        Text.of("  "),
                        shortcut("/", "Search"),
                        Box.of().flexGrow(1),
                        Text.of(sortLabel + filterHint + " ").color(Color.BRIGHT_CYAN).bold()
                ).paddingX(1)
        ).flexDirection(FlexDirection.COLUMN);
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
        AppState s = getState();

        if (s.isSearchMode()) {
            handleSearchInput(s, input, key);
            return;
        }

        List<ProcessInfo> visible = filteredProcesses(s);
        int total  = visible.size();
        int selIdx = s.getSelectedIndex();

        if ("q".equals(input)) {
            System.exit(0);
        } else if ("/".equals(input)) {
            setState(new AppState(s.getSysInfo(), s.getProcesses(), 0,
                    s.getSortField(), s.getSearchQuery(), true));
        } else if (key.escape()) {
            // 清除过滤
            setState(new AppState(s.getSysInfo(), s.getProcesses(), 0, s.getSortField(), "", false));
        } else if (key.upArrow() || key.scrollUp()) {
            updateSel(s, Math.max(0, selIdx - 1));
        } else if (key.downArrow() || key.scrollDown()) {
            updateSel(s, Math.min(Math.max(0, total - 1), selIdx + 1));
        } else if (key.pageUp()) {
            updateSel(s, Math.max(0, selIdx - 10));
        } else if (key.pageDown()) {
            updateSel(s, Math.min(Math.max(0, total - 1), selIdx + 10));
        } else if (key.home()) {
            updateSel(s, 0);
        } else if (key.end()) {
            updateSel(s, Math.max(0, total - 1));
        } else if ("p".equals(input)) {
            resort(s, SortField.CPU);
        } else if ("m".equals(input)) {
            resort(s, SortField.MEM);
        } else if ("n".equals(input)) {
            resort(s, SortField.PID);
        } else if ("N".equals(input)) {
            resort(s, SortField.NAME);
        } else if ("r".equals(input)) {
            scheduler.execute(new Runnable() {
                @Override public void run() { refresh(); }
            });
        }
    }

    private void handleSearchInput(AppState s, String input, Key key) {
        if (key.escape()) {
            // 清空并退出
            setState(new AppState(s.getSysInfo(), s.getProcesses(), 0, s.getSortField(), "", false));
        } else if (key.return_()) {
            // 确认搜索，退出输入模式但保留过滤词
            setState(new AppState(s.getSysInfo(), s.getProcesses(), 0,
                    s.getSortField(), s.getSearchQuery(), false));
        } else if (key.backspace() || key.delete()) {
            String q = s.getSearchQuery();
            String newQ = q.isEmpty() ? "" : q.substring(0, q.length() - 1);
            setState(new AppState(s.getSysInfo(), s.getProcesses(), 0, s.getSortField(), newQ, true));
        } else if (!input.isEmpty() && !key.ctrl() && !key.meta()) {
            setState(new AppState(s.getSysInfo(), s.getProcesses(), 0,
                    s.getSortField(), s.getSearchQuery() + input, true));
        }
    }

    private void updateSel(AppState s, int newIdx) {
        setState(new AppState(s.getSysInfo(), s.getProcesses(), newIdx,
                s.getSortField(), s.getSearchQuery(), s.isSearchMode()));
    }

    private void resort(AppState s, SortField field) {
        List<ProcessInfo> sorted = sortProcesses(s.getProcesses(), field);
        setState(new AppState(s.getSysInfo(), sorted, 0, field, s.getSearchQuery(), s.isSearchMode()));
    }

    // ===== 入口 =====

    public static void main(String[] args) {
        Ink.render(new TopDemo()).waitUntilExit();
    }
}
