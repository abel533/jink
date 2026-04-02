package io.mybatis.jink.demo;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.Attributes;
import org.jline.utils.NonBlockingReader;

/**
 * 输入诊断工具：显示 JLine 实际收到的每个字符的十六进制值和时间间隔。
 * 用于调试方向键、鼠标滚轮等特殊键在 Windows 上的实际输入序列。
 *
 * 运行后请按方向键、滚动鼠标滚轮，观察输出的十六进制序列。
 * 按 Ctrl+C 退出。
 */
public class InputDiagnostic {
    public static void main(String[] args) throws Exception {
        Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .build();

        System.out.println("Terminal type: " + terminal.getType());
        System.out.println("Terminal class: " + terminal.getClass().getName());
        System.out.println("Size: " + terminal.getSize());
        System.out.println();
        System.out.println("Press keys to see their hex values. Ctrl+C to exit.");
        System.out.println("Try: arrow keys, mouse wheel, Shift+Enter, Alt+Enter");
        System.out.println("---");

        Attributes saved = terminal.enterRawMode();
        NonBlockingReader reader = terminal.reader();

        long lastTime = System.currentTimeMillis();

        try {
            while (true) {
                int ch = reader.read(100);
                if (ch < 0) continue;

                long now = System.currentTimeMillis();
                long delta = now - lastTime;
                lastTime = now;

                String display;
                if (ch == 0x1B) display = "ESC";
                else if (ch == '\r') display = "CR";
                else if (ch == '\n') display = "LF";
                else if (ch == '\t') display = "TAB";
                else if (ch == 0x7F) display = "DEL";
                else if (ch < 0x20) display = "^" + (char) (ch + 64);
                else if (ch < 0x7F) display = "'" + (char) ch + "'";
                else display = "U+" + Integer.toHexString(ch);

                // 超过 200ms 间隔视为新事件，加空行分隔
                if (delta > 200) {
                    System.out.print("\r\n");
                }

                System.out.printf("[+%3dms] 0x%04X  %s\r\n", delta, ch, display);

                // Ctrl+C
                if (ch == 3) {
                    break;
                }
            }
        } finally {
            terminal.setAttributes(saved);
            terminal.close();
        }
    }
}
