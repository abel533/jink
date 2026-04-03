# ink vs jink 功能对比

> 详细对比 ink (TypeScript) 和 jink (Java) 的功能实现情况。

## 总览

| 维度 | ink | jink | 覆盖率 |
|:-----|:----|:-----|:-------|
| 组件数量 | 17 个（含 Context） | 11 个 | ~65% |
| Hooks/API | 12 个 Hook | 7 个等效 API | ~58% |
| Flexbox 属性 | ~95% CSS Flexbox | ~70% | ~70% |
| 按键支持 | ~50+ 键（含 Kitty 协议） | ~30 键 | ~60% |
| **总体功能对等** | — | — | **~65%** |

---

## 1. 组件系统

| 功能 | ink | jink | 状态 | 说明 |
|:-----|:----|:-----|:-----|:-----|
| Box | ✅ Box.tsx | ✅ Box.java | ✅ 完整 | Flexbox 容器 |
| Text | ✅ Text.tsx | ✅ Text.java | ✅ 完整 | 样式文本（支持嵌套） |
| Static | ✅ Static.tsx | ✅ Static.java | ✅ 完整 | 增量渲染不可变内容 |
| Transform | ✅ Transform.tsx | ✅ Transform.java | ✅ 完整 | 输出行变换 |
| Newline | ✅ Newline.tsx | ✅ Newline.java | ✅ 完整 | 文本中插入换行 |
| Spacer | ✅ Spacer.tsx | ✅ Spacer.java | ✅ 完整 | flexGrow=1 弹性空白 |
| ErrorBoundary | ✅ ErrorBoundary.tsx | ❌ 缺失 | 🔴 可实现 | 错误边界组件 |
| AppContext | ✅ 提供 exit/stdin/stdout | ⚠️ 部分 | 🟡 | jink 通过 Component 基类访问 |
| StdinContext | ✅ 暴露 stdin 流 | ❌ 缺失 | 🔴 可实现 | jink 直接用 JLine |
| StdoutContext | ✅ 暴露 stdout 流 | ❌ 缺失 | 🔴 可实现 | |
| StderrContext | ✅ 暴露 stderr 流 | ❌ 缺失 | 🔴 可实现 | |
| AccessibilityContext | ✅ 屏幕阅读器支持 | ❌ 缺失 | 🔴 可实现 | 辅助功能/ARIA 角色 |
| CursorContext | ✅ useCursor hook | ⚠️ 隐式 | 🟡 | jink 用 setCursorPosition() |
| FocusContext | ✅ FocusContext.tsx | ✅ FocusManager | ✅ 完整 | Tab/Shift+Tab 导航 |

---

## 2. Hooks vs 方法 API

| ink Hook | jink 等效 | 状态 | 说明 |
|:---------|:----------|:-----|:-----|
| `useApp()` | Component 基类 | ⚠️ 部分 | exit() 可用，缺少 stdin/stdout 暴露 |
| `useInput()` | `Component.onInput()` | ✅ 完整 | 方法 vs Hook，功能等效 |
| `usePaste()` | ❌ 缺失 | 🔴 可实现 | 剪贴板粘贴事件 |
| `useStdin()` | ❌ 缺失 | 🔴 可实现 | |
| `useStdout()` | ❌ 缺失 | 🔴 可实现 | |
| `useStderr()` | ❌ 缺失 | 🔴 可实现 | |
| `useWindowSize()` | `getColumns()/getRows()` | ✅ 完整 | 含 WINCH 信号监听 |
| `useBoxMetrics()` | ❌ 缺失 | 🔴 可实现 | 获取布局后的尺寸/位置 |
| `useFocus()` | `Focusable` 接口 | ✅ 完整 | |
| `useFocusManager()` | `FocusManager` 类 | ✅ 完整 | |
| `useCursor()` | `setCursorPosition()` | ✅ 完整 | |
| `useIsScreenReaderEnabled()` | ❌ 缺失 | 🔴 可实现 | 屏幕阅读器检测 |

> **不可能移植的原因**：ink 基于 React，使用 Hooks 模式（函数式组件 + 闭包状态）。Java 无法直接复制 Hooks 模式，jink 改用 OOP 类组件模式，这是**设计选择**而非功能缺失。核心功能已通过 Component 方法等效实现。

---

## 3. Flexbox 布局

| 属性 | ink (Yoga) | jink | 状态 | 说明 |
|:-----|:----------|:-----|:-----|:-----|
| flexDirection | ✅ 4 个值 | ✅ 4 个值 | ✅ 完整 | row/column/row-reverse/column-reverse |
| justifyContent | ✅ 6 个值 | ✅ 6 个值 | ✅ 完整 | |
| alignItems | ✅ 5 个值 | ✅ 4 个值 | ⚠️ 缺 baseline | baseline 对齐缺失 |
| alignSelf | ✅ 完整 | ⚠️ 部分 | 🟡 | |
| **alignContent** | ✅ 7 个值 | ❌ 缺失 | 🔴 可实现 | 多行对齐（需 flexWrap） |
| **flexWrap** | ✅ nowrap/wrap/wrap-reverse | ❌ 缺失 | 🔴 可实现 | 仅单行布局 |
| flexGrow | ✅ | ✅ | ✅ 完整 | |
| flexShrink | ✅ | ✅ | ✅ 完整 | |
| flexBasis | ✅ 数值+百分比+auto | ⚠️ 仅数值 | 🟡 | 缺少百分比 |
| width/height | ✅ 数值+百分比 | ✅ 仅数值 | 🟡 | 缺少百分比 |
| minWidth/minHeight | ✅ 数值+百分比 | ✅ 仅数值 | 🟡 | |
| padding | ✅ 完整 | ✅ 完整 | ✅ 完整 | |
| margin | ✅ 完整 | ✅ 完整 | ✅ 完整 | |
| gap | ✅ gap/rowGap/columnGap | ✅ gap/rowGap/columnGap | ✅ 完整 | |
| **position: absolute** | ✅ relative/absolute | ❌ 缺失 | 🔴 可实现 | 绝对定位 |
| display | ✅ flex/none | ✅ flex/none | ✅ 完整 | |
| overflow | ✅ visible/hidden | ✅ visible/hidden | ✅ 完整 | |
| **aspectRatio** | ✅ | ❌ 缺失 | 🔴 可实现 | 宽高比 |

---

## 4. 样式

| 功能 | ink | jink | 状态 |
|:-----|:----|:-----|:-----|
| 前景色 (16色) | ✅ | ✅ | ✅ 完整 |
| 前景色 (256色) | ✅ | ✅ ansi256() | ✅ 完整 |
| 前景色 (RGB) | ✅ | ✅ rgb()/hex() | ✅ 完整 |
| 背景色 | ✅ | ✅ | ✅ 完整 |
| bold | ✅ | ✅ | ✅ 完整 |
| italic | ✅ | ✅ | ✅ 完整 |
| underline | ✅ | ✅ | ✅ 完整 |
| strikethrough | ✅ | ✅ | ✅ 完整 |
| dimmed | ✅ | ✅ | ✅ 完整 |
| inverse | ✅ | ✅ | ✅ 完整 |
| textWrap (多种截断) | ✅ 7 种模式 | ⚠️ 仅 wrap | 🟡 缺截断模式 |
| 边框样式 | ✅ 15+ 种 | ✅ 9 种 | 🟡 够用 |
| 边框颜色 (每边独立) | ✅ | ⚠️ 单一颜色 | 🟡 |
| 边框 dimColor | ✅ 每边独立 | ❌ 缺失 | 🔴 |

---

## 5. 输入处理

| 按键类型 | ink | jink | 状态 |
|:---------|:----|:-----|:-----|
| 普通字符 (a-z/0-9/符号) | ✅ | ✅ | ✅ 完整 |
| Ctrl+字母 | ✅ | ✅ | ✅ 完整 |
| 方向键 ↑↓←→ | ✅ | ✅ | ✅ 完整 |
| 导航键 (Home/End/PgUp/PgDn) | ✅ | ✅ | ✅ 完整 |
| F1-F12 | ✅ | ✅ | ✅ 完整 |
| F13-F35 | ✅ | ❌ 缺失 | 🔴 可实现 |
| Tab/Shift+Tab | ✅ | ✅ | ✅ 完整 |
| Enter/Escape/Backspace/Delete | ✅ | ✅ | ✅ 完整 |
| Meta/Alt 修饰符 | ✅ | ✅ | ✅ 完整 |
| Shift 修饰符 | ✅ 完整 | ⚠️ 部分 | 🟡 仅 Shift+Arrow/Tab |
| **Kitty 键盘协议** | ✅ 完整支持 | ❌ 缺失 | 🔴 可实现 |
| 媒体键/小键盘 | ✅ | ❌ 缺失 | 🔴 低优先级 |
| 粘贴事件 | ✅ usePaste | ❌ 缺失 | 🔴 可实现 |

---

## 6. 渲染管道

| 功能 | ink | jink | 状态 |
|:-----|:----|:-----|:-----|
| 虚拟屏幕缓冲 | ✅ Output.ts | ✅ VirtualScreen | ✅ 完整 |
| DOM → 屏幕渲染 | ✅ render-node-to-output | ✅ NodeRenderer | ✅ 完整 |
| 文本测量 | ✅ measure-text + string-width | ✅ AnsiStringUtils + StringWidth | ✅ 完整 |
| ANSI 转义码 | ✅ ansi-escapes 库 | ✅ Ansi 工具类 | ✅ 完整 |
| 备用屏幕缓冲区 | ✅ | ✅ | ✅ 完整 |
| 光标显示/隐藏 | ✅ | ✅ | ✅ 完整 |
| **差异化行更新** | ✅ log-update (增量) | ⚠️ 全屏重写 | 🟡 性能影响小 |
| 帧率控制 | ✅ | ✅ maxFps() | ✅ 完整 |
| renderToString | ✅ | ✅ | ✅ 完整 |

---

## 7. 终端交互

| 功能 | ink | jink | 状态 |
|:-----|:----|:-----|:-----|
| Raw mode | ✅ Node.js stdin | ✅ JLine 3 | ✅ 完整 |
| 终端尺寸检测 | ✅ stdout.columns/rows | ✅ JLine Size | ✅ 完整 |
| 尺寸变化监听 | ✅ SIGWINCH | ✅ WINCH signal | ✅ 完整 |
| 信号处理 (Ctrl+C) | ✅ signal-exit | ✅ INT signal + shutdown hook | ✅ 完整 |
| React 并发模式 | ✅ React Scheduler | ❌ 单线程 | ⚫ 不适用 |
| React DevTools | ✅ 可选集成 | ❌ 无 | ⚫ 不适用 |
| patchConsole | ✅ 拦截 console.log | ❌ 无 | 🔴 可实现 |

---

## 8. 不可能/不需要移植的功能

| 功能 | 原因 |
|:-----|:-----|
| React Hooks 模式 | Java 语言特性限制，改用 OOP 类组件模式（**等效替代**） |
| React Reconciler | ink 直接用 React 调和器，jink 自研渲染管道（**等效替代**） |
| React Context API | 无直接等效，用依赖注入/方法调用替代（**等效替代**） |
| React 并发模式 | JVM 线程模型不同，单线程事件循环足够（**无需移植**） |
| React DevTools | 生态特有工具，Java 无对应需求（**无需移植**） |
| JSX 语法 | Java 无 JSX，用 Builder 模式替代（**等效替代**） |

---

## 9. 可实现但尚未实现的功能（按优先级排序）

### 高优先级
| 功能 | 难度 | 说明 |
|:-----|:-----|:-----|
| textWrap 截断模式 | 中 | truncate/truncate-end/truncate-middle |
| flexWrap | 高 | 多行 Flexbox 布局 |
| position: absolute | 中 | 绝对定位支持 |
| 百分比尺寸 | 中 | width: 50% 等 |
| ErrorBoundary | 低 | 错误边界组件 |

### 中优先级
| 功能 | 难度 | 说明 |
|:-----|:-----|:-----|
| alignContent | 中 | 多行对齐（依赖 flexWrap） |
| baseline 对齐 | 中 | alignItems: baseline |
| 每边独立边框色 | 低 | borderTopColor 等 |
| Shift 修饰符完整支持 | 低 | Shift+字母等组合 |
| usePaste 等效 | 中 | 剪贴板粘贴检测 |
| useBoxMetrics 等效 | 低 | 获取组件渲染后的尺寸 |

### 低优先级
| 功能 | 难度 | 说明 |
|:-----|:-----|:-----|
| F13-F35 功能键 | 低 | 极少使用 |
| Kitty 键盘协议 | 高 | 需要完整协议实现 |
| 辅助功能/屏幕阅读器 | 高 | 需要完整 a11y 系统 |
| 差异化行更新 | 中 | 性能优化，当前全屏重写已够用 |
| patchConsole | 低 | 拦截 System.out |
| 媒体键/小键盘 | 低 | 极少在 TUI 中使用 |

---

## 10. jink 独有特性

| 功能 | 说明 |
|:-----|:-----|
| JLine 3 原生集成 | 自动检测 Windows/Unix 终端类型 |
| Builder 模式 API | 比 JSX 更适合 Java 生态的 API 风格 |
| StringWidth CJK 支持 | 内置中日韩字符宽度计算 |
| 绝对光标定位渲染 | 按行绝对定位，避免滚动问题 |
| 幂等清理机制 | synchronized + cleaned 标志位，防止重复清理 |
| 多种运行脚本 | PowerShell/CMD 跨平台启动脚本 |
