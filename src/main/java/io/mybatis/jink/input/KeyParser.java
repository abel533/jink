package io.mybatis.jink.input;

import java.util.HashMap;
import java.util.Map;

/**
 * 终端按键解析器，对应 ink 的 parse-keypress.ts。
 * 将原始终端输入序列解析为 Key 事件。
 * <p>
 * 支持：
 * - 普通字符输入 (a-z, 0-9, 符号)
 * - Ctrl+字母组合 (0x01-0x1a)
 * - 标准 ANSI 转义序列（箭头键、功能键、导航键）
 * - Meta/Alt 修饰符（ESC + 字母）
 */
public class KeyParser {

    /** ESC 后缀 → 键名映射 */
    private static final Map<String, String> ESC_SEQUENCES = new HashMap<>();

    static {
        // 箭头键
        ESC_SEQUENCES.put("[A", "up");
        ESC_SEQUENCES.put("[B", "down");
        ESC_SEQUENCES.put("[C", "right");
        ESC_SEQUENCES.put("[D", "left");

        // 导航键
        ESC_SEQUENCES.put("[1~", "home");
        ESC_SEQUENCES.put("[H", "home");
        ESC_SEQUENCES.put("[7~", "home");
        ESC_SEQUENCES.put("[4~", "end");
        ESC_SEQUENCES.put("[F", "end");
        ESC_SEQUENCES.put("[8~", "end");
        ESC_SEQUENCES.put("[5~", "pageup");
        ESC_SEQUENCES.put("[6~", "pagedown");
        ESC_SEQUENCES.put("[2~", "insert");
        ESC_SEQUENCES.put("[3~", "delete");

        // 功能键 (xterm/VT)
        ESC_SEQUENCES.put("OP", "f1");
        ESC_SEQUENCES.put("OQ", "f2");
        ESC_SEQUENCES.put("OR", "f3");
        ESC_SEQUENCES.put("OS", "f4");
        ESC_SEQUENCES.put("[11~", "f1");
        ESC_SEQUENCES.put("[12~", "f2");
        ESC_SEQUENCES.put("[13~", "f3");
        ESC_SEQUENCES.put("[14~", "f4");
        ESC_SEQUENCES.put("[15~", "f5");
        ESC_SEQUENCES.put("[17~", "f6");
        ESC_SEQUENCES.put("[18~", "f7");
        ESC_SEQUENCES.put("[19~", "f8");
        ESC_SEQUENCES.put("[20~", "f9");
        ESC_SEQUENCES.put("[21~", "f10");
        ESC_SEQUENCES.put("[23~", "f11");
        ESC_SEQUENCES.put("[24~", "f12");

        // Shift+箭头
        ESC_SEQUENCES.put("[1;2A", "up");
        ESC_SEQUENCES.put("[1;2B", "down");
        ESC_SEQUENCES.put("[1;2C", "right");
        ESC_SEQUENCES.put("[1;2D", "left");

        // Shift+Tab (backtab)
        ESC_SEQUENCES.put("[Z", "tab");
    }

    /** Shift 修饰的序列 */
    private static final Map<String, Boolean> SHIFT_SEQUENCES = new HashMap<>();

    static {
        SHIFT_SEQUENCES.put("[1;2A", true);
        SHIFT_SEQUENCES.put("[1;2B", true);
        SHIFT_SEQUENCES.put("[1;2C", true);
        SHIFT_SEQUENCES.put("[1;2D", true);
        SHIFT_SEQUENCES.put("[Z", true);
    }

    /**
     * 解析转义序列后缀（ESC 之后的部分）
     */
    public static ParseResult parseEscapeSequence(String suffix) {
        String keyName = ESC_SEQUENCES.get(suffix);
        boolean shift = SHIFT_SEQUENCES.containsKey(suffix);

        if (keyName != null) {
            return new ParseResult(keyName, "", false, shift, false);
        }

        // ESC + 单个字母 = Meta 修饰
        if (suffix.length() == 1 && Character.isLetterOrDigit(suffix.charAt(0))) {
            return new ParseResult(
                    String.valueOf(Character.toLowerCase(suffix.charAt(0))),
                    suffix, false, false, true);
        }

        // ESC + 控制字符 = Meta+控制键
        if (suffix.length() == 1) {
            char c = suffix.charAt(0);
            if (c == '\r' || c == '\n') {
                return new ParseResult("return", "", false, false, true);
            }
        }

        // 未知序列，返回 escape
        return new ParseResult("escape", "", false, false, false);
    }

    /**
     * 解析单个控制字符
     */
    public static ParseResult parseControlChar(int ch) {
        return switch (ch) {
            case '\r', '\n' -> new ParseResult("return", "", false, false, false);
            case '\t' -> new ParseResult("tab", "", false, false, false);
            case 0x1b -> new ParseResult("escape", "", false, false, false);
            case 0x7f -> new ParseResult("backspace", "", false, false, false);
            case 0x08 -> new ParseResult("backspace", "", true, false, false);
            default -> {
                // Ctrl+letter: 0x01-0x1a 对应 a-z
                if (ch >= 1 && ch <= 26) {
                    char letter = (char) ('a' + ch - 1);
                    yield new ParseResult(String.valueOf(letter), "", true, false, false);
                }
                yield new ParseResult(String.valueOf((char) ch), String.valueOf((char) ch),
                        false, false, false);
            }
        };
    }

    /**
     * 解析普通可打印字符
     */
    public static ParseResult parseChar(int codePoint) {
        String text = new String(Character.toChars(codePoint));
        return new ParseResult(text, text, false, false, false);
    }

    /**
     * 检查是否为完整的转义序列
     */
    public static boolean isCompleteSequence(String suffix) {
        if (suffix.isEmpty()) return false;

        char first = suffix.charAt(0);

        // ESC O x (SS3 序列)
        if (first == 'O' && suffix.length() >= 2) return true;

        // ESC [ ... (CSI 序列)
        if (first == '[') {
            if (suffix.length() < 2) return false;
            char last = suffix.charAt(suffix.length() - 1);
            // CSI 序列以字母或 ~ 结尾
            return Character.isLetter(last) || last == '~';
        }

        // 单个字符 (Meta+letter)
        return suffix.length() == 1;
    }

    /**
     * 解析结果
     */
    public record ParseResult(String name, String input, boolean ctrl, boolean shift, boolean meta) {

        /**
         * 转换为 Key 事件
         */
        public Key toKey() {
            return new Key(
                    "up".equals(name),
                    "down".equals(name),
                    "left".equals(name),
                    "right".equals(name),
                    "pageup".equals(name),
                    "pagedown".equals(name),
                    "home".equals(name),
                    "end".equals(name),
                    "return".equals(name),
                    "escape".equals(name),
                    "tab".equals(name),
                    "backspace".equals(name),
                    "delete".equals(name),
                    ctrl, shift, meta
            );
        }

        /**
         * 获取用户可见的输入文本（非特殊键时返回原始字符）
         */
        public String inputText() {
            if (ctrl || meta) return "";
            return switch (name) {
                case "up", "down", "left", "right",
                        "pageup", "pagedown", "home", "end",
                        "return", "escape", "tab", "backspace", "delete",
                        "f1", "f2", "f3", "f4", "f5", "f6",
                        "f7", "f8", "f9", "f10", "f11", "f12",
                        "insert" -> "";
                default -> input.isEmpty() ? name : input;
            };
        }
    }
}
