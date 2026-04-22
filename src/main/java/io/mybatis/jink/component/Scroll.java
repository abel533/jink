package io.mybatis.jink.component;

import io.mybatis.jink.dom.ElementNode;
import io.mybatis.jink.dom.Node;
import io.mybatis.jink.input.Key;
import io.mybatis.jink.style.FlexDirection;
import io.mybatis.jink.style.Style;

/**
 * Scroll 组件 - 为内容添加垂直滚动能力。
 * 支持 ↑/↓ 方向键控制滚动，按 q 退出滚动模式。
 * 滚动条用 ▲▼ 符号在右侧指示当前位置。
 * <p>
 * 使用示例:
 * <pre>
 * Scroll.of(content)
 *     .viewportHeight(5)  // 视口高度为 5 行
 *     .scrollMode(true)   // 启用滚动模式（接收键盘事件）
 * </pre>
 */
public class Scroll extends Component<Scroll.State> {

    public static final class State {
        public final int scrollOffset;
        public final int viewportHeight;
        public final int contentHeight;
        public final boolean scrollMode;

        public State(int scrollOffset, int viewportHeight, int contentHeight, boolean scrollMode) {
            this.scrollOffset = scrollOffset;
            this.viewportHeight = viewportHeight;
            this.contentHeight = contentHeight;
            this.scrollMode = scrollMode;
        }
    }

    private final Renderable content;
    private Integer explicitViewportHeight;

    private Scroll(Renderable content) {
        super(new State(0, 0, 0, false));
        this.content = content;
    }

    public static Scroll of(Renderable content) {
        return new Scroll(content);
    }

    /**
     * 设置视口高度（容器可见区域的行数）。
     * 如果不设置，将使用内容的实际高度（不会滚动）。
     */
    public Scroll viewportHeight(int height) {
        this.explicitViewportHeight = height;
        return this;
    }

    /**
     * 设置是否启用滚动模式。
     * 滚动模式下会接收 ↑↓ 方向键和 q 键事件。
     */
    public Scroll scrollMode(boolean enabled) {
        State s = getState();
        if (s.scrollMode != enabled) {
            setState(new State(s.scrollOffset, s.viewportHeight, s.contentHeight, enabled));
        }
        return this;
    }

    /**
     * 获取当前滚动偏移。
     */
    public int getScrollOffset() {
        return getState().scrollOffset;
    }

    /**
     * 获取内容总高度（布局后可用）。
     */
    public int getContentHeight() {
        return getState().contentHeight;
    }

    /**
     * 检查是否可以向上滚动。
     */
    public boolean canScrollUp() {
        State s = getState();
        return s.scrollOffset > 0;
    }

    /**
     * 检查是否可以向下滚动。
     */
    public boolean canScrollDown() {
        State s = getState();
        return s.contentHeight > 0 && s.scrollOffset + s.viewportHeight < s.contentHeight;
    }

    /**
     * 向上滚动一行。
     */
    public void scrollUp() {
        State s = getState();
        if (s.scrollOffset > 0) {
            setState(new State(s.scrollOffset - 1, s.viewportHeight, s.contentHeight, s.scrollMode));
        }
    }

    /**
     * 向下滚动一行。
     */
    public void scrollDown() {
        State s = getState();
        if (s.contentHeight > 0 && s.scrollOffset + s.viewportHeight < s.contentHeight) {
            setState(new State(s.scrollOffset + 1, s.viewportHeight, s.contentHeight, s.scrollMode));
        }
    }

    /**
     * 滚动到顶部。
     */
    public void scrollToTop() {
        State s = getState();
        if (s.scrollOffset != 0) {
            setState(new State(0, s.viewportHeight, s.contentHeight, s.scrollMode));
        }
    }

    /**
     * 滚动到底部。
     */
    public void scrollToBottom() {
        State s = getState();
        if (s.contentHeight > 0) {
            int maxOffset = Math.max(0, s.contentHeight - s.viewportHeight);
            if (s.scrollOffset != maxOffset) {
                setState(new State(maxOffset, s.viewportHeight, s.contentHeight, s.scrollMode));
            }
        }
    }

    @Override
    public Renderable render() {
        State s = getState();
        int viewportH = explicitViewportHeight != null ? explicitViewportHeight : s.viewportHeight;

        if (viewportH <= 0) {
            return content;
        }

        Box contentBox = Box.of(content)
                .flexDirection(FlexDirection.COLUMN);

        Box container = Box.of(contentBox)
                .flexDirection(FlexDirection.COLUMN)
                .height(viewportH);

        return new ScrollRenderable(container, s.scrollOffset, viewportH, s.contentHeight, this);
    }

    @Override
    public void onInput(String input, Key key) {
        State s = getState();
        if (!s.scrollMode) {
            return;
        }

        if ("q".equalsIgnoreCase(input)) {
            setState(new State(s.scrollOffset, s.viewportHeight, s.contentHeight, false));
            return;
        }

        if (key.upArrow()) {
            scrollUp();
            return;
        }

        if (key.downArrow()) {
            scrollDown();
            return;
        }

        if (key.pageUp()) {
            State current = getState();
            int newOffset = Math.max(0, current.scrollOffset - current.viewportHeight);
            if (newOffset != current.scrollOffset) {
                setState(new State(newOffset, current.viewportHeight, current.contentHeight, current.scrollMode));
            }
            return;
        }

        if (key.pageDown()) {
            State current = getState();
            if (current.contentHeight > 0) {
                int maxOffset = Math.max(0, current.contentHeight - current.viewportHeight);
                int newOffset = Math.min(maxOffset, current.scrollOffset + current.viewportHeight);
                if (newOffset != current.scrollOffset) {
                    setState(new State(newOffset, current.viewportHeight, current.contentHeight, current.scrollMode));
                }
            }
            return;
        }

        if (key.home()) {
            scrollToTop();
            return;
        }

        if (key.end()) {
            scrollToBottom();
            return;
        }
    }

    private static final class ScrollRenderable implements Renderable {
        private final Renderable content;
        private final int scrollOffset;
        private final int viewportHeight;
        private final int contentHeight;
        private final Scroll scrollComponent;

        ScrollRenderable(Renderable content, int scrollOffset, int viewportHeight, int contentHeight,
                         Scroll scrollComponent) {
            this.content = content;
            this.scrollOffset = scrollOffset;
            this.viewportHeight = viewportHeight;
            this.contentHeight = contentHeight;
            this.scrollComponent = scrollComponent;
        }

        @Override
        public ElementNode toNode() {
            ElementNode node = content.toNode();

            node.setAttribute("internal_scrollOffset", scrollOffset);
            node.setAttribute("internal_viewportHeight", viewportHeight);

            if (contentHeight > 0) {
                node.setAttribute("internal_contentHeight", contentHeight);
            }

            node.addLayoutListener(() -> {
                int measuredHeight = findContentHeight(node);
                Scroll.State current = scrollComponent.getState();
                if (measuredHeight > 0 && measuredHeight != current.contentHeight) {
                    int newViewportHeight = current.viewportHeight > 0 ? current.viewportHeight : viewportHeight;
                    int maxOffset = Math.max(0, measuredHeight - newViewportHeight);
                    int newOffset = Math.min(current.scrollOffset, maxOffset);
                    scrollComponent.setState(new Scroll.State(newOffset, newViewportHeight, measuredHeight, current.scrollMode));
                }
            });

            return node;
        }

        private int findContentHeight(ElementNode node) {
            int nodeHeight = node.getComputedHeight();
            Style style = node.getStyle();
            int borderV = style.verticalBorderWidth();
            int paddingV = style.verticalPadding();
            int contentHeight = nodeHeight - borderV - paddingV;

            if (contentHeight > viewportHeight) {
                return contentHeight;
            }

            for (Node child : node.getChildNodes()) {
                if (child instanceof ElementNode) {
                    int childHeight = findContentHeight((ElementNode) child);
                    if (childHeight > contentHeight) {
                        contentHeight = childHeight;
                    }
                }
            }

            return contentHeight;
        }
    }
}
