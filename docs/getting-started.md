# Jink 快速入门

> 从零开始构建你的第一个终端 UI 应用。

## 环境要求

- **Java 8+**
- **Maven 3.6+**
- 真实终端（Windows Terminal / iTerm2 / GNOME Terminal 等）

## 添加依赖

```xml
<dependency>
    <groupId>io.mybatis.jink</groupId>
    <artifactId>jink</artifactId>
    <version>0.5.0</version>
</dependency>
```

## 示例 1：Hello World（静态渲染）

最简单的 jink 程序，渲染一行带样式的文本：

```java
import io.mybatis.jink.Ink;
import io.mybatis.jink.component.*;
import io.mybatis.jink.style.*;

public class HelloWorld {
    public static void main(String[] args) {
        Renderable ui = Box.of(
            Text.of("Hello, Jink!").color(Color.GREEN).bold()
        ).borderStyle(BorderStyle.ROUND)
         .borderColor(Color.BRIGHT_MAGENTA)
         .paddingX(1);

        // 输出到控制台
        Ink.renderOnce(ui, 40, 5);
    }
}
```

输出效果：
```
╭──────────────────────────────────────╮
│ Hello, Jink!                         │
╰──────────────────────────────────────╯
```

---

## 示例 2：富文本样式

展示颜色、粗体、斜体、下划线等文本样式：

```java
Renderable ui = Box.of(
    Text.of("普通文本"),
    Text.of("粗体文本").bold(),
    Text.of("斜体文本").italic(),
    Text.of("下划线").underline(),
    Text.of("红色文本").color(Color.RED),
    Text.of("绿色背景").backgroundColor(Color.GREEN),
    Text.of("RGB 橙色").color(Color.rgb(255, 165, 0)),
    Text.of("反色效果").inverse(),
    Text.of("暗淡文本").dimmed(),
    Text.of(
        Text.of("嵌套: ").color(Color.CYAN),
        Text.of("粗体红").color(Color.RED).bold(),
        Text.of(" 和 "),
        Text.of("下划线蓝").color(Color.BLUE).underline()
    )
).flexDirection(FlexDirection.COLUMN).width(40);

Ink.renderOnce(ui, 40, 12);
```

---

## 示例 3：Flexbox 布局

展示水平和垂直布局：

```java
// 水平布局（默认）
Renderable horizontal = Box.of(
    Box.of(Text.of("左")).flexGrow(1).borderStyle(BorderStyle.SINGLE),
    Box.of(Text.of("中")).flexGrow(2).borderStyle(BorderStyle.SINGLE),
    Box.of(Text.of("右")).flexGrow(1).borderStyle(BorderStyle.SINGLE)
).width(60).height(3);

// 垂直布局
Renderable vertical = Box.of(
    Text.of("标题").color(Color.BRIGHT_CYAN).bold(),
    Text.of("─".repeat(40)).color(Color.BRIGHT_BLACK),
    Text.of("正文内容..."),
    Spacer.create(),
    Text.of("底部").dimmed()
).flexDirection(FlexDirection.COLUMN).width(40).height(8);
```

---

## 示例 4：边框样式

jink 内置 9 种边框样式：

```java
BorderStyle[] styles = {
    BorderStyle.SINGLE, BorderStyle.DOUBLE, BorderStyle.ROUND,
    BorderStyle.BOLD, BorderStyle.CLASSIC, BorderStyle.ARROW
};

List<Renderable> boxes = new ArrayList<>();
for (BorderStyle style : styles) {
    boxes.add(
        Box.of(Text.of(style.name()))
            .borderStyle(style)
            .paddingX(1)
            .width(18)
            .height(3)
    );
}

Renderable ui = Box.of(boxes.toArray(new Renderable[0]))
    .flexDirection(FlexDirection.COLUMN)
    .gap(1);
```

---

## 示例 5：交互式应用

创建一个支持键盘输入的交互式组件：

```java
public class Counter extends Component<Counter.State> {
    record State(int count) {}

    public Counter() {
        super(new State(0));
    }

    @Override
    public Renderable render() {
        return Box.of(
            Text.of("计数器: " + getState().count())
                .color(Color.BRIGHT_GREEN).bold(),
            Text.of(""),
            Text.of("按 ↑ 增加, ↓ 减少, q 退出").dimmed()
        ).flexDirection(FlexDirection.COLUMN)
         .borderStyle(BorderStyle.ROUND)
         .borderColor(Color.BRIGHT_CYAN)
         .paddingX(2).paddingY(1);
    }

    @Override
    public void onInput(String input, Key key) {
        if (key.upArrow()) {
            setState(new State(getState().count() + 1));
        } else if (key.downArrow()) {
            setState(new State(Math.max(0, getState().count() - 1)));
        } else if ("q".equals(input)) {
            // 需要通过 Ink.Instance.exit() 退出
        }
    }

    public static void main(String[] args) {
        Ink.Instance app = Ink.render(new Counter());
        app.waitUntilExit();
    }
}
```

---

## 示例 6：多行输入和消息列表

展示复杂交互场景：

```java
public class Chat extends Component<Chat.State> {
    record State(String input, List<String> messages) {}

    public Chat() {
        super(new State("", List.of()));
    }

    @Override
    public Renderable render() {
        State s = getState();
        int w = getColumns();

        // 消息列表
        List<Renderable> msgItems = new ArrayList<>();
        for (String msg : s.messages) {
            msgItems.add(Text.of("● " + msg).color(Color.WHITE));
        }

        // 输入框提示
        Text prompt = s.input.isEmpty()
            ? Text.of("❯ 输入消息...").dimmed()
            : Text.of("❯ " + s.input).color(Color.GREEN);

        return Box.of(
            Box.of(Text.of("Jink Chat").bold())
                .borderStyle(BorderStyle.ROUND).paddingX(1),
            Box.of(msgItems.toArray(new Renderable[0]))
                .flexDirection(FlexDirection.COLUMN).paddingX(1).paddingY(1),
            Spacer.create(),
            Text.of("─".repeat(w)),
            Box.of(prompt).paddingX(1),
            Text.of("─".repeat(w)),
            Box.of(
                Text.of("Enter").bold().inverse(),
                Text.of(" 发送  ").dimmed(),
                Text.of("Ctrl+C").bold().inverse(),
                Text.of(" 退出").dimmed()
            ).paddingX(1)
        ).flexDirection(FlexDirection.COLUMN).width(w).height(getRows());
    }

    @Override
    public void onInput(String input, Key key) {
        State s = getState();
        if (key.return_() && !s.input.isEmpty()) {
            List<String> msgs = new ArrayList<>(s.messages);
            msgs.add(s.input);
            setState(new State("", msgs));
        } else if (key.backspace() && !s.input.isEmpty()) {
            setState(new State(s.input.substring(0, s.input.length() - 1), s.messages));
        } else if (!input.isEmpty() && !key.ctrl() && !key.meta()) {
            setState(new State(s.input + input, s.messages));
        }
    }

    public static void main(String[] args) {
        Ink.Instance app = Ink.render(new Chat());
        app.waitUntilExit();
    }
}
```

---

## 示例 7：非交互预览（用于测试）

不进入 raw mode，直接输出渲染结果：

```java
// 渲染为 ANSI 字符串
String output = Ink.renderToString(myComponent, 80, 24);
System.out.println(output);

// 渲染到标准输出
Ink.renderOnce(myComponent, 80, 24);

// 渲染到文件
Ink.renderOnce(myComponent, 80, 24, new PrintStream("output.txt"));
```

---

## 运行 Demo

项目内置了多个 Demo，使用启动脚本一键运行：

### 交互式菜单（推荐）

自动扫描并列出所有 Demo 类，选择序号运行：

```powershell
# PowerShell
.\scripts\run.ps1

# 指定 JDK 路径（当系统 Java < 21 时）
.\scripts\run.ps1 C:\path\to\jdk21
```

```bash
# Bash
./scripts/run.sh

# 指定 JDK 路径
./scripts/run.sh /path/to/jdk21
```

```cmd
:: CMD
scripts\run.cmd
```

### 直接运行指定 Demo

```powershell
# PowerShell：run-demo.ps1 [类名] [JDK路径（可选）]
.\scripts\run-demo.ps1 io.mybatis.jink.demo.Counter
.\scripts\run-demo.ps1 io.mybatis.jink.demo.CopilotDemo
.\scripts\run-demo.ps1 io.mybatis.jink.demo.InputDiagnostic
```

```cmd
:: CMD
scripts\run-demo.cmd io.mybatis.jink.demo.Counter
```

> **JDK 优先级**：命令行参数 > `JINK_JAVA_HOME` 环境变量 > 系统 Java（须 ≥ 21）

如果需要排查 Windows Terminal + JLine 下的滚轮或方向键行为，运行 `InputDiagnostic`，它会直接打印收到的原始 ESC 序列和解析结果。

---

## 下一步

- 📖 [架构与 API 参考](architecture.md) — 深入了解内部实现
- 🎨 [示例集锦](examples.md) — 更多示例和演示步骤
