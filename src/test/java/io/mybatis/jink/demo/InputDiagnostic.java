package io.mybatis.jink.demo;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.Attributes;
import org.jline.utils.InfoCmp;
import org.jline.utils.NonBlockingReader;

/**
 * 输入诊断工具：测试 JLine 鼠标事件和键盘输入。
 */
public class InputDiagnostic {
    public static void main(String[] args) throws Exception {
        Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .build();

        Attributes saved = terminal.enterRawMode();
        var writer = terminal.writer();

        // 进入备用屏幕缓冲区
        writer.print("\u001B[?1049h");
        writer.print("\u001B[2J\u001B[H");
        writer.print("\u001B[?25l");
        writer.flush();

        // 检查并启用 JLine 鼠标支持
        boolean mouseOk = terminal.hasMouseSupport();
        writer.printf("\u001B[1;1HMouse support: %s, Terminal: %s", mouseOk, terminal.getClass().getSimpleName());
        if (mouseOk) {
            terminal.trackMouse(Terminal.MouseTracking.Normal);
            writer.print("\u001B[2;1HMouse tracking ENABLED. Scroll wheel, click, arrows, then Ctrl+C.");
        } else {
            writer.print("\u001B[2;1HMouse NOT supported. Try arrows, then Ctrl+C.");
        }

        // 获取 key_mouse 能力字符串
        String keyMouse = null;
        try {
            keyMouse = terminal.getStringCapability(InfoCmp.Capability.key_mouse);
        } catch (Exception ignored) {
        }
        writer.printf("\u001B[3;1Hkey_mouse: %s",
                keyMouse != null ? "len=" + keyMouse.length() + " first=0x" + Integer.toHexString(keyMouse.charAt(0)) : "null");
        writer.flush();

        NonBlockingReader reader = terminal.reader();
        long lastTime = System.currentTimeMillis();
        int row = 5;

        try {
            while (true) {
                int ch = reader.read(100);
                if (ch < 0) continue;

                long now = System.currentTimeMillis();
                long delta = now - lastTime;
                lastTime = now;

                // 检测是否为 key_mouse 触发字符
                if (keyMouse != null && keyMouse.length() > 0 && ch == keyMouse.charAt(0)) {
                    try {
                        var mouseEvent = terminal.readMouseEvent(() -> {
                            try { return reader.read(50); } catch (Exception e) { return -1; }
                        });
                        if (delta > 200) row++;
                        writer.printf("\u001B[%d;1H\u001B[2K[+%3dms] MOUSE: %s   ", row, delta, mouseEvent);
                        writer.flush();
                        row++;
                        continue;
                    } catch (Exception e) {
                        if (delta > 200) row++;
                        writer.printf("\u001B[%d;1H\u001B[2K[+%3dms] MOUSE_ERR: %s   ", row, delta, e.getMessage());
                        writer.flush();
                        row++;
                        continue;
                    }
                }

                String display;
                if (ch == 0x1B) display = "ESC";
                else if (ch == '\r') display = "CR";
                else if (ch == '\n') display = "LF";
                else if (ch == '\t') display = "TAB";
                else if (ch == 0x7F) display = "DEL";
                else if (ch < 0x20) display = "^" + (char) (ch + 64);
                else if (ch < 0x7F) display = "'" + (char) ch + "'";
                else display = "U+" + Integer.toHexString(ch);

                if (delta > 200) row++;
                writer.printf("\u001B[%d;1H\u001B[2K[+%3dms] 0x%04X  %s   ", row, delta, ch, display);
                writer.flush();
                row++;

                if (ch == 3) break;
            }
        } finally {
            if (mouseOk) terminal.trackMouse(Terminal.MouseTracking.Off);
            writer.print("\u001B[?25h");
            writer.print("\u001B[?1049l");
            writer.flush();
            terminal.setAttributes(saved);
            terminal.close();
        }
    }
}
