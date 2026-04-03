# jink

**Java 版终端 UI 框架**，灵感来自 [ink](https://github.com/vadimdemedes/ink)（React for CLI）。

用声明式的方式构建终端界面：组件树 → Flexbox 布局 → ANSI 渲染 → 键盘交互。

![Java 21+](https://img.shields.io/badge/Java-21%2B-blue)
![JLine 3](https://img.shields.io/badge/JLine-3.28.0-green)
![Tests](https://img.shields.io/badge/Tests-146%20passing-brightgreen)
![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)

---

## ✨ 特性

| 功能 | 说明 |
|:-----|:-----|
| **组件模型** | Builder 模式构建 UI 组件树，类 React 有状态组件 |
| **Flexbox 布局** | 纯 Java 实现，支持 direction/justify/align/gap/grow |
| **丰富样式** | 16 色 / 256 色 / RGB 真彩色，粗体/斜体/下划线/反色 |
| **9 种边框** | SINGLE, DOUBLE, ROUND, BOLD, CLASSIC, ARROW 等 |
| **键盘输入** | 基于 JLine 3 raw mode，方向键/功能键/Ctrl 组合键 |
| **CJK 支持** | 中日韩字符正确占 2 列宽度 |
| **焦点管理** | Tab/Shift+Tab 导航，可编程聚焦 |
| **全屏模式** | 备用屏幕缓冲区，退出后终端恢复干净 |
| **帧率控制** | 可配置 maxFps，默认 30 |

---

## 📦 要求

- **Java 21+**
- **Maven 3.6+**
- 真实终端（Windows Terminal / iTerm2 / GNOME Terminal）

---

## 🚀 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.mybatis.jink</groupId>
    <artifactId>jink</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Hello World

```java
import io.mybatis.jink.Ink;
import io.mybatis.jink.component.*;
import io.mybatis.jink.style.*;

public class HelloWorld {
    public static void main(String[] args) {
        Ink.renderOnce(
            Box.of(
                Text.of("Hello, Jink!").color(Color.GREEN).bold()
            ).borderStyle(BorderStyle.ROUND)
             .borderColor(Color.BRIGHT_MAGENTA)
             .paddingX(1),
            40, 5
        );
    }
}
```

```
╭──────────────────────────────────────╮
│ Hello, Jink!                         │
╰──────────────────────────────────────╯
```

---

## 🆚 与 ink 对比

ink（React/TypeScript）的第一个示例是一个自动计数器：

```tsx
// ink (TypeScript)
import React, {useState, useEffect} from 'react';
import {render, Text} from 'ink';

const Counter = () => {
    const [counter, setCounter] = useState(0);

    useEffect(() => {
        const timer = setInterval(() => {
            setCounter(previousCounter => previousCounter + 1);
        }, 100);
        return () => clearInterval(timer);
    }, []);

    return <Text color="green">{counter} tests passed</Text>;
};

render(<Counter />);
```

等效的 jink（Java）实现：

```java
// jink (Java)
import io.mybatis.jink.Ink;
import io.mybatis.jink.component.Component;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.style.Color;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Counter extends Component<Counter.State> {
    record State(int count) {}

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    public Counter() {
        super(new State(0));
    }

    @Override
    public void onMount() {
        scheduler.scheduleAtFixedRate(() ->
            setState(new State(getState().count() + 1)),
            100, 100, TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void onUnmount() {
        scheduler.shutdownNow();
    }

    @Override
    public Renderable render() {
        return Text.of(getState().count() + " tests passed")
                .color(Color.GREEN);
    }

    public static void main(String[] args) {
        Ink.render(new Counter()).waitUntilExit();
    }
}
```

---

## 🎨 示例展示

### 文本样式

```java
Box.of(
    Text.of("粗体").bold(),
    Text.of("红色").color(Color.RED),
    Text.of("RGB橙").color(Color.rgb(255, 165, 0)),
    Text.of("反色").inverse(),
    Text.of(
        Text.of("嵌套: ").color(Color.CYAN),
        Text.of("红色粗体").color(Color.RED).bold()
    )
).flexDirection(FlexDirection.COLUMN);
```

### Flexbox 布局

```java
// 水平等分面板
Box.of(
    Box.of(Text.of("左侧")).flexGrow(1).borderStyle(BorderStyle.SINGLE),
    Box.of(Text.of("右侧")).flexGrow(1).borderStyle(BorderStyle.SINGLE)
).width(60).height(5);

// 垂直布局 + 弹性空白
Box.of(
    Text.of("标题").bold(),
    Spacer.create(),    // 自动填充中间空间
    Text.of("底部").dimmed()
).flexDirection(FlexDirection.COLUMN).height(10);
```

### 有状态交互组件

```java
public class Counter extends Component<Counter.State> {
    record State(int count) {}

    public Counter() { super(new State(0)); }

    @Override
    public Renderable render() {
        return Box.of(
            Text.of("计数: " + getState().count()).color(Color.GREEN).bold(),
            Text.of("↑ 增加  ↓ 减少  q 退出").dimmed()
        ).flexDirection(FlexDirection.COLUMN)
         .borderStyle(BorderStyle.ROUND).paddingX(1);
    }

    @Override
    public void onInput(String input, Key key) {
        if (key.upArrow()) setState(new State(getState().count() + 1));
        else if (key.downArrow()) setState(new State(getState().count() - 1));
    }

    public static void main(String[] args) {
        Ink.render(new Counter()).waitUntilExit();
    }
}
```

### 完整 Copilot CLI 复刻

```java
// CopilotDemo: 完整复刻 GitHub Copilot CLI 界面
// 包含：标题框 + 消息滚动 + 多行输入 + 输入历史 + 快捷键栏
Ink.render(new CopilotDemo()).waitUntilExit();
```

<!-- TODO: 在此处插入 CopilotDemo 的 GIF 动画 -->
<!-- ![CopilotDemo](docs/images/copilot-demo.gif) -->

---

## 📚 文档

| 文档 | 说明 |
|:-----|:-----|
| [快速入门](docs/getting-started.md) | 从零开始，7 个完整示例 |
| [架构与 API 参考](docs/architecture.md) | 包结构、渲染管道、完整 API |
| [示例集锦](docs/examples.md) | 所有 Demo 的操作步骤（适合录制 GIF） |
| [ink vs jink 对比](docs/comparison.md) | 功能覆盖率、缺失项、不可移植项分析 |

---

## 🏃 运行 Demo

```powershell
# 编译
mvn compile test-compile

# 静态渲染 Demo
.\scripts\run-simple.ps1

# 交互式 Demo（消息列表 + 键盘导航）
.\scripts\run-interactive.ps1

# Copilot CLI 风格 Demo（完整功能）
.\scripts\run-demo.ps1

# 静态预览（调试用，可指定尺寸）
.\scripts\run-preview.ps1 [width] [height]
```

### 输入诊断

当需要排查 Windows Terminal + JLine 下的键盘/鼠标输入时，可以运行诊断工具：

```powershell
mvn test-compile
java -cp "target\classes;target\test-classes;..." io.mybatis.jink.demo.InputDiagnostic
```

它会直接启用 `trackMouse()`，并打印收到的方向键、滚轮和其他 ESC 序列，方便确认当前终端实际发送的输入。

---

## 🔧 构建 & 测试

```bash
# 编译
mvn clean compile

# 运行 146 个单元测试
mvn test

# 打包
mvn clean package
```

---

## 🏗️ 项目结构

```
io.mybatis.jink
├── Ink                   # 框架入口（render / renderToString / renderOnce）
├── component/            # 组件系统
│   ├── Component<S>      #   有状态组件基类
│   ├── Box               #   Flexbox 容器
│   ├── Text              #   文本（支持嵌套和样式）
│   ├── Spacer            #   弹性空白
│   ├── Static<T>         #   增量渲染
│   └── FocusManager      #   焦点管理
├── style/                # 样式定义
│   ├── Color             #   16/256/RGB 颜色
│   ├── BorderStyle       #   9 种边框样式
│   └── FlexDirection ... #   Flexbox 枚举
├── layout/               # Flexbox 布局引擎
├── render/               # 渲染管道（VirtualScreen → ANSI）
├── input/                # 键盘输入（Key + KeyParser）
├── dom/                  # 虚拟 DOM（ElementNode/TextNode）
├── ansi/                 # ANSI 转义码工具
└── util/                 # StringWidth（CJK 宽度计算）
```

---

## ⚖️ ink 功能对照

| 功能 | ink | jink |
|:-----|:----|:-----|
| 声明式 UI | JSX | Builder API |
| 状态管理 | useState | Component.setState() |
| 副作用 | useEffect | onMount/onUnmount |
| 输入处理 | useInput | onInput 方法 |
| Flexbox | Yoga (C++) | 纯 Java 实现 |
| 颜色 | chalk | 内置 Color |
| 边框 | boxen | 9 种 BorderStyle |
| 焦点 | useFocus | FocusManager |
| 静态内容 | \<Static\> | Static 组件 |
| CJK 宽度 | string-width | StringWidth |
| 终端控制 | 内置 | JLine 3 |
| 最低版本 | Node.js 18+ | Java 21+ |

---

## 📋 已知限制

- CopilotDemo 的鼠标滚轮依赖 JLine Windows 终端的原生鼠标追踪；其他终端的鼠标事件格式可能需要额外适配
- 目前仅在 Windows Terminal + JLine 3 上测试，Linux/macOS 终端行为可能不同
- 尚未发布到 Maven Central（即将发布）

---

## 📄 License

Apache License, Version 2.0 - 详见 [LICENSE](LICENSE) 文件。
