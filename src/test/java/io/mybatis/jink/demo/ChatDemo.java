package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Component;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.input.Key;
import io.mybatis.jink.style.FlexDirection;

import java.util.ArrayList;
import java.util.List;

/**
 * ink 官方示例 chat 的 jink 等效实现。
 *
 * <p>简单聊天输入框：键入文字后按 Enter 发送消息到列表，
 * Backspace 删除末字符，Ctrl+C 退出。
 * 对应 ink 的 useInput + useState 组合。
 *
 * <p>运行:
 * <pre>
 * mvn exec:java -Dexec.mainClass=io.mybatis.jink.demo.ChatDemo -Dexec.classpathScope=test
 * </pre>
 */
public class ChatDemo extends Component<ChatDemo.State> {

    record State(String input, List<String> messages) {}

    public ChatDemo() {
        super(new State("", new ArrayList<>()));
    }

    @Override
    public Renderable render() {
        State s = getState();
        List<Renderable> msgNodes = new ArrayList<>();
        for (String msg : s.messages()) {
            msgNodes.add(Text.of(msg));
        }
        return Box.of(
                Box.of(msgNodes.toArray(new Renderable[0])).flexDirection(FlexDirection.COLUMN),
                Box.of(Text.of("Enter your message: " + s.input())).marginTop(1)
        ).flexDirection(FlexDirection.COLUMN).padding(1);
    }

    @Override
    public void onInput(String input, Key key) {
        State s = getState();
        if (key.return_()) {
            if (!s.input().isEmpty()) {
                List<String> msgs = new ArrayList<>(s.messages());
                msgs.add("User: " + s.input());
                setState(new State("", msgs));
            }
        } else if (key.backspace() || key.delete()) {
            if (!s.input().isEmpty()) {
                setState(new State(s.input().substring(0, s.input().length() - 1), s.messages()));
            }
        } else if (!input.isEmpty()) {
            setState(new State(s.input() + input, s.messages()));
        }
    }

    public static void main(String[] args) {
        Ink.render(new ChatDemo()).waitUntilExit();
    }
}
