# Jink 示例集锦

> 全面展示 jink 各项功能的代码示例，附带演示操作步骤，适合录制 GIF 动画。

---

## 内置 Demo 一览

| Demo | 说明 | 交互性 | 运行命令 |
|:-----|:-----|:------|:---------|
| `SimpleDemo` | 静态渲染，展示布局和样式 | 无 | `.\run-simple.ps1` |
| `InteractiveDemo` | 消息列表，键盘导航 | ✅ | `.\run-interactive.ps1` |
| `CopilotDemo` | 完整 Copilot CLI 复刻 | ✅ | `.\run-demo.ps1` |
| `CopilotDemoPreview` | CopilotDemo 静态预览 | 无 | `.\run-preview.ps1` |

---

## Demo 1：SimpleDemo — 静态渲染

### 展示功能
- ✅ 圆角边框 (BorderStyle.ROUND)
- ✅ 边框着色 (borderColor)
- ✅ 文本颜色（多种 ANSI 颜色）
- ✅ 文本样式（bold, italic, dimmed, inverse）
- ✅ Flexbox 垂直布局 (COLUMN)
- ✅ Flexbox 水平布局（嵌套面板）
- ✅ padding/gap 间距
- ✅ flexGrow 弹性分配
- ✅ RGB 真彩色
- ✅ `renderToString` / `renderOnce` API

### 运行步骤

```powershell
cd ink4j
.\run-simple.ps1
```

### GIF 录制操作
1. 打开终端，进入 `ink4j` 目录
2. 运行 `.\run-simple.ps1`
3. 画面会依次显示：
   - Copilot CLI 风格界面（圆角边框 + 消息列表 + 输入框 + 快捷键栏）
   - 嵌套 Flexbox 布局（左右两栏面板）
   - 颜色与样式验证（多种文本效果）
4. 程序自动退出

### 核心代码片段

```java
// 欢迎框（圆角洋红色边框）
Box.of(
    Text.of("Welcome to GitHub Copilot CLI v1.0")
        .color(Color.MAGENTA).bold(),
    Text.of("Powered by Jink - Java ink framework").dimmed()
).flexDirection(FlexDirection.COLUMN)
 .borderStyle(BorderStyle.ROUND)
 .borderColor(Color.MAGENTA)
 .paddingX(1);

// 嵌套面板布局
Box.of(
    Box.of(Text.of("左侧面板").color(Color.CYAN))
        .flexGrow(1).borderStyle(BorderStyle.SINGLE).height(5),
    Box.of(Text.of("右侧面板").color(Color.YELLOW))
        .flexGrow(1).borderStyle(BorderStyle.SINGLE).height(5)
).width(40).height(5);

// 颜色样式
Box.of(
    Text.of("普通文本"),
    Text.of("粗体").bold(),
    Text.of("斜体").italic(),
    Text.of("红色").color(Color.RED),
    Text.of("RGB颜色").color(Color.rgb(255, 165, 0))
).flexDirection(FlexDirection.COLUMN);
```

---

## Demo 2：InteractiveDemo — 键盘交互

### 展示功能
- ✅ 有状态组件 (Component\<State\>)
- ✅ 实时键盘输入 (onInput)
- ✅ setState 驱动重渲染
- ✅ 消息列表动态更新
- ✅ 方向键导航选择
- ✅ 快捷键提示栏（inverse 反色样式）
- ✅ 备用屏幕缓冲区（退出后终端干净）

### 运行步骤

```powershell
cd ink4j
.\run-interactive.ps1
```

### GIF 录制操作
1. 运行后显示欢迎消息列表
2. **输入文字**：键入 "Hello Jink"，观察输入框实时更新
3. **发送消息**：按 `Enter`，消息出现在列表中
4. **再发几条**：输入 "这是第二条" → Enter → "Third message" → Enter
5. **方向键选择**：按 `↑` `↓` 选择消息，观察高亮变化（青色粗体）
6. **删除消息**：选中某条后按 `Backspace` 删除
7. **退出**：按 `Ctrl+C`，终端恢复干净

### 核心代码片段

```java
public class InteractiveDemo extends Component<InteractiveDemo.State> {
    record State(String inputText, int cursorPos,
                 List<String> messages, int selectedIndex) {}

    @Override
    public Renderable render() {
        // 高亮选中项
        if (selected) {
            Text.of("▶ " + msg).color(Color.CYAN).bold();
        } else {
            Text.of("  " + msg).color(Color.WHITE);
        }
    }

    @Override
    public void onInput(String input, Key key) {
        if (key.return_()) {
            // 添加消息
        } else if (key.upArrow()) {
            // 选择上一条
        }
    }
}
```

---

## Demo 3：CopilotDemo — 完整复刻

### 展示功能
- ✅ 全屏 Copilot CLI 界面
- ✅ 圆角标题框（洋红色边框 + 版本信息 + Tips）
- ✅ 消息列表（虚拟滚动）
- ✅ 弹性空白区 (Spacer)
- ✅ 状态栏（左: 路径, 右: 模型信息）
- ✅ 输入框（提示符 ❯ + placeholder）
- ✅ 水平分隔线
- ✅ 底部快捷键栏
- ✅ 多行输入 (Shift+Enter)
- ✅ 输入历史 (Ctrl+P/N)
- ✅ 消息滚动 (↑↓/鼠标滚轮/PageUp/PageDown)
- ✅ CJK 字符光标位置正确
- ✅ Ctrl+C 优雅退出
- ✅ 终端尺寸自适应

### 运行步骤

```powershell
cd ink4j
.\run-demo.ps1
```

### GIF 录制操作（完整流程，约 30 秒）

**第一段：基本输入** (约 10 秒)
1. 运行后展示完整界面（标题框 + 消息 + 输入框 + 快捷键）
2. 输入 "Hello Jink!" → 按 `Enter` 发送
3. 输入 "你好，这是中文测试" → 按 `Enter`
4. 输入 "Third message" → 按 `Enter`

**第二段：滚动和历史** (约 10 秒)
5. 鼠标滚轮向上滚动查看消息
6. 按 `↑↓` 滚动
7. 按 `PageUp` / `PageDown` 快速滚动
8. 按 `Ctrl+P` 浏览输入历史（显示上一条命令）
9. 按 `Ctrl+N` 切换到下一条
10. 按 `Ctrl+P` 多次，浏览所有历史

**第三段：多行输入** (约 5 秒)
11. 输入 "第一行"
12. 按 `Shift+Enter`（实际为 `Alt+Enter`）换行
13. 继续输入 "第二行"
14. 按 `Enter` 发送多行消息

**第四段：退出** (约 3 秒)
15. 按 `Ctrl+C` 退出
16. 观察终端恢复到正常状态

### 快捷键总结

| 按键 | 功能 |
|:-----|:-----|
| `Enter` | 发送消息 |
| `Alt+Enter` | 多行输入换行 |
| `Backspace` | 删除字符 |
| `↑` / `↓` | 滚动消息（每次 3 行） |
| `PageUp` / `PageDown` | 快速滚动（每次 10 行） |
| `Ctrl+P` | 上一条输入历史 |
| `Ctrl+N` | 下一条输入历史 |
| `Ctrl+C` | 退出 |

### 核心代码片段

```java
public class CopilotDemo extends Component<CopilotDemo.State> {
    record State(String inputText, List<String> messages, int scrollOffset) {}

    @Override
    public Renderable render() {
        return Box.of(
            headerBox(w),           // 圆角标题框
            statusMessages(s, max), // 消息列表（虚拟滚动）
            Spacer.create(),        // 弹性空白
            statusBar(w, h),        // 状态栏
            separator(w),           // 分隔线
            inputArea(s, w),        // 输入区（❯ 提示符）
            separator(w),           // 分隔线
            shortcutBar(w)          // 快捷键栏
        ).flexDirection(FlexDirection.COLUMN).width(w).height(h);
    }
}
```

---

## Demo 4：CopilotDemoPreview — 静态预览

### 展示功能
- ✅ 无需 raw mode 的渲染测试
- ✅ 自定义终端尺寸
- ✅ 调试输出（行号 + 内容预览）

### 运行步骤

```powershell
cd ink4j
.\run-preview.ps1

# 自定义尺寸
.\run-preview.ps1 120 30
```

### GIF 录制操作
1. 运行 `.\run-preview.ps1`
2. 显示 CopilotDemo 的静态渲染结果（含 ANSI 颜色）
3. 观察调试信息（stderr 输出）

---

## 功能对照表

| 功能 | ink (TypeScript) | jink (Java) |
|:-----|:----------------|:------------|
| 声明式 UI | JSX | Builder API |
| 状态管理 | useState Hook | Component.setState() |
| 副作用 | useEffect Hook | onMount/onUnmount |
| 输入处理 | useInput Hook | onInput 方法 |
| Flexbox 布局 | Yoga (C++) | 纯 Java 实现 |
| 颜色 | chalk | 内置 Color 类 |
| 边框 | boxen 风格 | 9 种 BorderStyle |
| 焦点管理 | useFocus | FocusManager |
| 静态内容 | Static 组件 | Static 组件 |
| 文本换行 | wrap-ansi | 内置 TextWrap |
| CJK 宽度 | string-width | StringWidth 工具 |
| 终端控制 | 自动 | JLine 3 |
| 最低版本 | Node.js 18+ | Java 21+ |

---

## 创建运行脚本

为方便录制演示，可创建以下 PowerShell 脚本：

### run-simple.ps1

```powershell
$env:JAVA_HOME = "D:\Dev\jdk-21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
mvn -q test-compile
$cp = (mvn -q dependency:build-classpath -Dmdep.outputFile=CON 2>$null)
java -cp "target/classes;target/test-classes;$cp" io.mybatis.jink.demo.SimpleDemo
```

### run-interactive.ps1

```powershell
$env:JAVA_HOME = "D:\Dev\jdk-21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
mvn -q test-compile
$cp = (mvn -q dependency:build-classpath -Dmdep.outputFile=CON 2>$null)
java --enable-native-access=ALL-UNNAMED -cp "target/classes;target/test-classes;$cp" io.mybatis.jink.demo.InteractiveDemo
```

---

## 补充示例

### 颜色系统完整演示

```java
// 16 色基本色
Color.BLACK, Color.RED, Color.GREEN, Color.YELLOW,
Color.BLUE, Color.MAGENTA, Color.CYAN, Color.WHITE

// 16 色亮色
Color.BRIGHT_BLACK, Color.BRIGHT_RED, Color.BRIGHT_GREEN,
Color.BRIGHT_YELLOW, Color.BRIGHT_BLUE, Color.BRIGHT_MAGENTA,
Color.BRIGHT_CYAN, Color.BRIGHT_WHITE

// 256 色
Color.ansi256(196)  // 亮红
Color.ansi256(208)  // 橙色
Color.ansi256(226)  // 亮黄

// RGB 真彩色
Color.rgb(255, 0, 0)     // 红
Color.rgb(0, 255, 0)     // 绿
Color.hex("#FF6347")      // 番茄红
Color.hex("4169E1")       // 皇家蓝
```

### 边框样式完整演示

```java
// 所有可用边框
BorderStyle.SINGLE          // ┌─┐│└─┘
BorderStyle.DOUBLE          // ╔═╗║╚═╝
BorderStyle.ROUND           // ╭─╮│╰─╯
BorderStyle.BOLD            // ┏━┓┃┗━┛
BorderStyle.SINGLE_DOUBLE   // ╓─╖║╙─╜
BorderStyle.DOUBLE_SINGLE   // ╒═╕│╘═╛
BorderStyle.CLASSIC         // +--+|+--+
BorderStyle.ARROW           // ↘↓↙←↗↑↖→
```

### Flexbox 对齐方式演示

```java
// justifyContent 示例（水平分布）
Box.of(a, b, c).justifyContent(JustifyContent.SPACE_BETWEEN)
// [a         b         c]

Box.of(a, b, c).justifyContent(JustifyContent.CENTER)
// [      a  b  c      ]

// alignItems 示例（垂直对齐）
Box.of(tall, short_).alignItems(AlignItems.CENTER)
// tall 居中对齐 short_
```
