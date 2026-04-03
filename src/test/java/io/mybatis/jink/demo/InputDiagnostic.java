package io.mybatis.jink.demo;

import io.mybatis.jink.input.KeyParser;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;
import org.jline.utils.NonBlockingReader;

/**
 * 输入诊断工具：同时观察键盘输入和 JLine Windows 原生鼠标追踪。
 */
public class InputDiagnostic {
    public static void main(String[] args) throws Exception {
        Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .build();

        Attributes saved = terminal.enterRawMode();
        var writer = terminal.writer();
        NonBlockingReader reader = terminal.reader();
        boolean trackingEnabled = false;

        writer.print("\u001B[?1049h");
        writer.print("\u001B[2J\u001B[H");
        writer.print("\u001B[?25l");
        writer.flush();

        try {
            try {
                trackingEnabled = terminal.trackMouse(Terminal.MouseTracking.Normal);
            } catch (Exception ignored) {
                trackingEnabled = false;
            }

            writer.printf("\u001B[1;1HTerminal: %s, type=%s",
                    terminal.getClass().getSimpleName(), terminal.getType());
            writer.printf("\u001B[2;1HtrackMouse=%s, hasMouseSupport=%s",
                    trackingEnabled, terminal.hasMouseSupport());
            writer.printf("\u001B[3;1Hget_mouse=%s",
                    capabilityText(terminal, InfoCmp.Capability.get_mouse));
            writer.printf("\u001B[4;1Hkey_mouse=%s",
                    capabilityText(terminal, InfoCmp.Capability.key_mouse));
            writer.printf("\u001B[5;1HUse mouse wheel / arrows / keys, then Ctrl+C.");
            writer.flush();

            long lastTime = System.currentTimeMillis();
            int row = 7;

            while (true) {
                int ch = reader.read(100);
                if (ch < 0) {
                    continue;
                }

                long now = System.currentTimeMillis();
                long delta = now - lastTime;
                lastTime = now;

                if (ch == 0x1b) {
                    String suffix = readEscapeSuffix(reader);
                    KeyParser.ParseResult result = KeyParser.parseEscapeSequence(suffix);
                    String parsed = result == null ? "null" : result.name();
                    if (delta > 200) {
                        row++;
                    }
                    writer.printf("\u001B[%d;1H\u001B[2K[+%3dms] ESC %s => %s   ",
                            row++, delta, printableSuffix(suffix), parsed);
                    writer.flush();
                    continue;
                }

                String display;
                if (ch == '\r') display = "CR";
                else if (ch == '\n') display = "LF";
                else if (ch == '\t') display = "TAB";
                else if (ch == 0x7F) display = "DEL";
                else if (ch < 0x20) display = "^" + (char) (ch + 64);
                else if (ch < 0x7F) display = "'" + (char) ch + "'";
                else display = "U+" + Integer.toHexString(ch);

                if (delta > 200) {
                    row++;
                }
                writer.printf("\u001B[%d;1H\u001B[2K[+%3dms] 0x%04X %s   ",
                        row++, delta, ch, display);
                writer.flush();

                if (ch == 3) {
                    break;
                }
            }
        } finally {
            if (trackingEnabled) {
                try {
                    terminal.trackMouse(Terminal.MouseTracking.Off);
                } catch (Exception ignored) {
                }
            }
            writer.print("\u001B[?25h");
            writer.print("\u001B[?1049l");
            writer.flush();
            terminal.setAttributes(saved);
            terminal.close();
        }
    }

    private static String capabilityText(Terminal terminal, InfoCmp.Capability capability) {
        try {
            String value = terminal.getStringCapability(capability);
            return value == null ? "null" : printableSuffix(value);
        } catch (Exception ignored) {
            return "error";
        }
    }

    private static String readEscapeSuffix(NonBlockingReader reader) throws Exception {
        int next = reader.read(150);
        if (next < 0) {
            return "";
        }
        StringBuilder seq = new StringBuilder();
        seq.append((char) next);

        if (next != '[' && next != 'O') {
            return seq.toString();
        }

        while (!KeyParser.isCompleteSequence(seq.toString())) {
            int more = reader.read(300);
            if (more < 0) {
                break;
            }
            seq.append((char) more);
            if (seq.length() > 20) {
                break;
            }
        }
        return seq.toString();
    }

    private static String printableSuffix(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch < 0x20 || ch == 0x7f) {
                sb.append(String.format("\\x%02X", (int) ch));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
