# Jink 架构与 API 参考

> 本文档详细介绍 jink 的内部架构、核心 API 和扩展方式。

## 目录

- [架构总览](#架构总览)
- [包结构](#包结构)
- [渲染管道](#渲染管道)
- [组件系统](#组件系统)
- [布局引擎](#布局引擎)
- [样式系统](#样式系统)
- [输入处理](#输入处理)
- [终端管理](#终端管理)

---

## 架构总览

jink 的设计灵感来自 JavaScript 的 [ink](https://github.com/vadimdemedes/ink) 库，采用类 React 的组件化架构。核心思想是：**用声明式的方式描述终端 UI，框架自动处理布局和渲染。**

### ink → jink 模块映射

| ink (TypeScript) | jink (Java) | 说明 |
|:-----------------|:------------|:-----|
| `dom.ts` (DOMElement/TextNode) | `dom` 包 (Node/ElementNode/TextNode) | 虚拟 DOM 树 |
| `styles.ts` | `style` 包 (Style/FlexDirection 等) | 样式定义 |
| `yoga-layout` | `layout` 包 (FlexLayout) | 纯 Java Flexbox |
| `reconciler.ts` | 状态驱动重渲染 | DOM 更新 |
| `output.ts` | `render` 包 (VirtualScreen) | 虚拟输出缓冲 |
| `render-node-to-output.ts` | `render` 包 (NodeRenderer) | 节点→输出 |
| `log-update.ts` | `render` 包 (TerminalWriter) | 终端输出 |
| `ink.tsx` | `Ink` 主类 | 框架入口 |
| `Box.tsx` / `Text.tsx` | `component` 包 (Box/Text) | 内置组件 |
| `use-input.ts` | `input` 包 (InputHandler) | 键盘输入 |
| `parse-keypress.ts` | `input` 包 (KeyParser) | 按键解析 |

### 数据流

```
组件定义 → DOM 树构建 → Flexbox 布局 → 虚拟屏幕渲染 → ANSI 输出 → 终端显示
                                                                    ↑
                                              键盘输入 → 按键解析 → 组件 setState → 重渲染
```

---

## 包结构

```
io.mybatis.jink
├── Ink.java                    # 框架入口，渲染生命周期管理
├── component/                  # 组件系统
│   ├── Renderable.java         # 可渲染接口
│   ├── Component<S>.java       # 有状态组件基类
│   ├── Box.java                # Flexbox 容器组件
│   ├── Text.java               # 文本显示组件
│   ├── Spacer.java             # 弹性空白组件
│   ├── Static<T>.java          # 增量渲染组件
│   ├── Newline.java            # 换行组件
│   ├── Transform.java          # 内容变换组件
│   ├── Focusable.java          # 可聚焦接口
│   └── FocusManager.java       # 焦点管理器
├── dom/                        # 虚拟 DOM
│   ├── Node.java               # 节点基类
│   ├── ElementNode.java        # 元素节点（带布局）
│   ├── TextNode.java           # 文本节点
│   └── NodeType.java           # 节点类型枚举
├── style/                      # 样式定义
│   ├── Style.java              # 不可变样式记录
│   ├── Color.java              # 颜色（16/256/RGB）
│   ├── BorderStyle.java        # 边框样式（9 种）
│   ├── FlexDirection.java      # 排列方向
│   ├── JustifyContent.java     # 主轴对齐
│   ├── AlignItems.java         # 交叉轴对齐
│   ├── Display.java            # 显示模式
│   ├── TextWrap.java           # 文本换行
│   ├── Overflow.java           # 溢出处理
│   └── Position.java           # 定位方式
├── layout/                     # 布局引擎
│   ├── FlexLayout.java         # Flexbox 算法
│   └── LayoutResult.java       # 布局结果
├── render/                     # 渲染管道
│   ├── NodeRenderer.java       # DOM → VirtualScreen
│   ├── VirtualScreen.java      # 二维字符缓冲
│   └── TerminalWriter.java     # ANSI 终端输出
├── input/                      # 输入处理
│   ├── Key.java                # 按键事件记录
│   └── KeyParser.java          # 转义序列解析
├── ansi/                       # ANSI 工具
│   ├── Ansi.java               # 转义码生成
│   └── AnsiStringUtils.java    # ANSI 字符串工具
└── util/
    └── StringWidth.java        # CJK 字符宽度计算
```

---

## 渲染管道

渲染分为 5 个阶段：

### 1. 组件树构建

用户通过 Builder API 创建组件树：

```java
Box.of(
    Text.of("Hello").color(Color.GREEN),
    Text.of("World").bold()
).flexDirection(FlexDirection.COLUMN).padding(1)
```

### 2. DOM 树生成

`Renderable.toNode()` 将组件转为 `ElementNode` 树：
- `Box` → `ElementNode`（type=BOX，包含子节点和样式）
- `Text` → `ElementNode`（type=TEXT，包含 `TextNode` 叶子）

### 3. Flexbox 布局

`FlexLayout.calculateLayout(root, width)` 对 DOM 树执行布局计算：
- 递归计算每个节点的位置 (x, y) 和尺寸 (width, height)
- 处理 flexGrow/flexShrink 分配
- 应用 padding、margin、gap
- 结果存储在 `LayoutResult` 中

### 4. 虚拟屏幕渲染

`NodeRenderer.render(root)` 将布局后的 DOM 转为 `VirtualScreen`：
- DFS 遍历节点树
- 在虚拟网格上绘制文本、边框、背景色
- 应用裁剪（overflow: hidden）
- 处理 ANSI 颜色/样式转义码

### 5. 终端输出

`VirtualScreen.render()` 生成最终 ANSI 字符串：
- 合并相邻同色区域
- 生成最优 ANSI 转义序列
- `TerminalWriter` 或 `Ink.Instance` 写入终端

---

## 组件系统

### Renderable 接口

所有可渲染对象的基础接口：

```java
public interface Renderable {
    Node toNode();
}
```

### Component\<S\> 基类

有状态组件的基类，类似 React 类组件：

```java
public abstract class Component<S> implements Renderable {
    // 构造：设置初始状态
    protected Component(S initialState);

    // 必须实现：返回 UI 树
    public abstract Renderable render();

    // 状态管理
    protected S getState();
    protected void setState(S newState);  // 触发重渲染

    // 生命周期
    public void onMount();     // 组件挂载后调用
    public void onUnmount();   // 组件卸载前调用

    // 输入处理
    public void onInput(String input, Key key);

    // 终端信息
    protected int getColumns();  // 终端宽度
    protected int getRows();     // 终端高度

    // 光标控制
    protected void setCursorPosition(int row, int col);
}
```

### Box 组件

Flexbox 布局容器，通过链式 Builder API 配置：

```java
// 创建
Box.of(child1, child2, ...)
box.add(child)

// 布局
.flexDirection(FlexDirection.COLUMN)
.justifyContent(JustifyContent.CENTER)
.alignItems(AlignItems.STRETCH)

// 尺寸
.width(80).height(24)
.minWidth(40).minHeight(10)

// Flex 属性
.flexGrow(1).flexShrink(0)

// 间距
.padding(1).paddingX(2).paddingTop(1)
.margin(1).marginX(2)
.gap(1).rowGap(1).columnGap(2)

// 边框
.borderStyle(BorderStyle.ROUND)
.borderColor(Color.BRIGHT_MAGENTA)

// 其他
.display(Display.FLEX)
.overflow(Overflow.HIDDEN)
```

### Text 组件

文本显示组件，支持嵌套和样式：

```java
// 简单文本
Text.of("Hello")

// 样式
.color(Color.GREEN).bold().italic().underline()
.dimmed().inverse().strikethrough()
.backgroundColor(Color.BLUE)
.wrap(TextWrap.TRUNCATE_END)

// 嵌套（富文本）
Text.of(
    Text.of("Error: ").color(Color.RED).bold(),
    Text.of("File not found").color(Color.WHITE)
)
```

### Spacer 组件

弹性空白，自动填充剩余空间（flexGrow=1）：

```java
Box.of(
    Text.of("Left"),
    Spacer.create(),    // 占满中间空间
    Text.of("Right")
)
// 结果：Left                          Right
```

---

## 布局引擎

纯 Java 实现的简化版 CSS Flexbox，支持：

| 属性 | 可选值 |
|:-----|:------|
| `flexDirection` | `ROW`, `COLUMN`, `ROW_REVERSE`, `COLUMN_REVERSE` |
| `justifyContent` | `FLEX_START`, `CENTER`, `FLEX_END`, `SPACE_BETWEEN`, `SPACE_AROUND`, `SPACE_EVENLY` |
| `alignItems` | `FLEX_START`, `CENTER`, `FLEX_END`, `STRETCH`, `BASELINE` |
| `flexGrow` | `0, 1, 2, ...`（增长比例） |
| `flexShrink` | `0, 1, 2, ...`（收缩比例） |
| `gap` | 像素值（子元素间距） |
| `padding` | 上右下左（内边距） |
| `margin` | 上右下左（外边距） |
| `width/height` | 固定尺寸 |
| `minWidth/minHeight` | 最小尺寸 |

---

## 样式系统

### Color

支持三种颜色模式：

```java
// 16 色基本色
Color.RED, Color.GREEN, Color.BRIGHT_CYAN, ...

// 256 色
Color.ansi256(208)    // 橙色

// RGB 真彩色
Color.rgb(255, 165, 0)
Color.hex("#FFA500")
```

### BorderStyle

9 种边框样式：

| 样式 | 示例 |
|:-----|:-----|
| `SINGLE` | `┌─┐│└─┘` |
| `DOUBLE` | `╔═╗║╚═╝` |
| `ROUND` | `╭─╮│╰─╯` |
| `BOLD` | `┏━┓┃┗━┛` |
| `CLASSIC` | `+--+\|+--+` |
| `ARROW` | `↘↓↙←↗↑↖→` |
| `SINGLE_DOUBLE` | 混合样式 |
| `DOUBLE_SINGLE` | 混合样式 |

---

## 输入处理

### Key 事件

```java
public record Key(
    boolean upArrow, downArrow, leftArrow, rightArrow,
    boolean pageUp, pageDown, home, end,
    boolean return_, escape, tab, backspace, delete,
    boolean ctrl, shift, meta,
    boolean scrollUp, scrollDown
)
```

### 在组件中处理输入

```java
@Override
public void onInput(String input, Key key) {
    if (key.return_()) {
        // Enter 键
    } else if (key.ctrl() && "p".equals(input)) {
        // Ctrl+P
    } else if (key.upArrow()) {
        // 方向键上
    } else if (!input.isEmpty()) {
        // 普通文本输入
    }
}
```

### 支持的按键

- 方向键：↑ ↓ ← →
- 导航键：PageUp, PageDown, Home, End
- 功能键：F1-F12
- 修饰符：Ctrl, Shift, Meta/Alt
- 编辑键：Enter, Tab, Backspace, Delete, Escape
- 组合键：Ctrl+字母, Alt+字母, Shift+箭头

---

## 终端管理

### Ink 入口 API

```java
// 非交互渲染（测试/预览）
String output = Ink.renderToString(renderable, 80, 24);
Ink.renderOnce(renderable, 80, 24);

// 交互式渲染（全屏应用）
Ink.Instance app = Ink.render(new MyComponent());
app.waitUntilExit();
```

### Ink.Instance 生命周期

```java
Ink.Instance app = Ink.render(component);

// 配置
app.maxFps(30);

// 事件循环（阻塞，处理输入+渲染）
app.waitUntilExit();

// 退出
app.exit();
```

### 终端特性

- **备用屏幕缓冲区**：UI 在独立屏幕中渲染，退出后恢复原始终端内容
- **Raw Mode**：通过 JLine 3 实现，支持单字符输入
- **终端尺寸检测**：自动获取，监听 WINCH 信号实时调整
- **光标控制**：组件可设置光标位置
- **优雅退出**：Ctrl+C、JVM 关闭钩子、INT 信号处理
- **帧率控制**：默认 30 FPS，可调节

### StringWidth 工具

CJK（中日韩）字符在终端中占 2 列宽度，`StringWidth` 正确计算：

```java
StringWidth.width("Hello")     // → 5
StringWidth.width("你好")      // → 4（每个中文字符占 2 列）
StringWidth.width("Hi你好")    // → 6
```
