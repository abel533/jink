package io.mybatis.jink.component;

import io.mybatis.jink.dom.ElementNode;
import io.mybatis.jink.style.Color;

import java.util.function.Function;

/**
 * ErrorBoundary 组件 - 错误边界。
 * 捕获子组件在渲染（toNode）期间抛出的异常，渲染 fallback 内容代替崩溃的子树。
 * 对应 ink 的 ErrorBoundary 组件。
 * <p>
 * 使用示例:
 * <pre>
 * // 使用默认错误提示
 * ErrorBoundary.of(new MyComponent())
 *
 * // 使用静态 fallback
 * ErrorBoundary.of(
 *     new MyComponent(),
 *     Text.of("组件加载失败").color(Color.Basic.RED)
 * )
 *
 * // 使用动态 fallback（可访问异常信息）
 * ErrorBoundary.of(
 *     new MyComponent(),
 *     error -&gt; Text.of("错误: " + error.getMessage()).color(Color.Basic.RED)
 * )
 * </pre>
 */
public class ErrorBoundary implements Renderable {

    private final Renderable children;
    private final Function<Throwable, Renderable> fallback;

    private ErrorBoundary(Renderable children, Function<Throwable, Renderable> fallback) {
        this.children = children;
        this.fallback = fallback;
    }

    /**
     * 创建 ErrorBoundary，使用默认的错误提示（红色文本显示错误类名）
     */
    public static ErrorBoundary of(Renderable children) {
        return new ErrorBoundary(children, error ->
                Text.of(error.getClass().getSimpleName() + ": " + error.getMessage())
                        .color(Color.Basic.RED)
        );
    }

    /**
     * 创建 ErrorBoundary，使用静态 fallback 组件
     */
    public static ErrorBoundary of(Renderable children, Renderable fallback) {
        return new ErrorBoundary(children, error -> fallback);
    }

    /**
     * 创建 ErrorBoundary，使用动态 fallback 函数（可访问异常信息）
     */
    public static ErrorBoundary of(Renderable children, Function<Throwable, Renderable> fallback) {
        return new ErrorBoundary(children, fallback);
    }

    @Override
    public ElementNode toNode() {
        try {
            return children.toNode();
        } catch (Throwable error) {
            return fallback.apply(error).toNode();
        }
    }
}
