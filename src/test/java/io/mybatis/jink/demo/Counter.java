package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.Component;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.style.Color;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ink 官方文档第一个示例的 jink 等效实现。
 *
 * <p>ink (TypeScript) 原版:
 * <pre>
 * const Counter = () => {
 *     const [counter, setCounter] = useState(0);
 *     useEffect(() => {
 *         const timer = setInterval(() => {
 *             setCounter(prev => prev + 1);
 *         }, 100);
 *         return () => clearInterval(timer);
 *     }, []);
 *     return &lt;Text color="green"&gt;{counter} tests passed&lt;/Text&gt;;
 * };
 * render(&lt;Counter /&gt;);
 * </pre>
 *
 * <p>运行方式:
 * <pre>
 * mvn exec:java -Dexec.mainClass=io.mybatis.jink.demo.Counter \
 *   --enable-native-access=ALL-UNNAMED
 * </pre>
 */
public class Counter extends Component<Counter.State> {

    record State(int count) {}

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "counter-timer");
                t.setDaemon(true);
                return t;
            });

    public Counter() {
        super(new State(0));
    }

    @Override
    public void onMount() {
        scheduler.scheduleAtFixedRate(() -> {
            int next = getState().count() + 1;
            setState(new State(next));
            if (next >= 100) {
                scheduler.shutdown();
            }
        }, 100, 100, TimeUnit.MILLISECONDS);
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
