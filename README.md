# jink

Java 版终端 UI 框架，灵感来自 [ink](https://github.com/vadimdemedes/ink)。

## 特性

- **组件模型**：Builder 模式构建 UI 组件树
- **Flexbox 布局**：纯 Java 实现的简化版 Flexbox
- **虚拟 DOM**：差异化更新，高效渲染
- **ANSI 支持**：完整的颜色、样式、光标控制
- **键盘输入**：基于 JLine 3 的 raw mode 输入处理

## 要求

- Java 21+
- Maven 3.6+

## 快速开始

```java
import io.mybatis.jink.*;
import io.mybatis.jink.component.*;
import io.mybatis.jink.style.*;

public class HelloWorld {
    public static void main(String[] args) {
        Jink.render(
            Box.of(
                Text.of("Hello, jink!").color(Color.GREEN).bold()
            ).borderStyle(BorderStyle.SINGLE)
             .padding(1)
        );
    }
}
```

## 构建

```bash
mvn clean package
```
