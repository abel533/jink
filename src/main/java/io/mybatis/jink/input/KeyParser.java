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
        // 箭头键 (CSI 格式: ESC [ x)
        ESC_SEQUENCES.put("[A", "up");
        ESC_SEQUENCES.put("[B", "down");
        ESC_SEQUENCES.put("[C", "right");
        ESC_SEQUENCES.put("[D", "left");

        // 箭头键 (SS3 格式)
        ESC_SEQUENCES.put("OA", "up");
        ESC_SEQUENCES.put("OB", "down");
        ESC_SEQUENCES.put("OC", "right");
        ESC_SEQUENCES.put("OD", "left");

        // 导航键
        ESC_SEQUENCES.put("[1~", "home");
        ESC_SEQUENCES.put("[H", "home");
        ESC_SEQUENCES.put("OH", "home");  // SS3 格式
        ESC_SEQUENCES.put("[7~", "home");
        ESC_SEQUENCES.put("[4~", "end");
        ESC_SEQUENCES.put("[F", "end");
        ESC_SEQUENCES.put("OF", "end");   // SS3 格式
        ESC_SEQUENCES.put("[8~", "end");
        ESC_SEQUENCES.put("[5~", "pageup");
        ESC_SEQUENCES.put("[6~", "pagedown");
        ESC_SEQUENCES.put("[2~", "insert");
        ESC_SEQUENCES.put("[3~", "delete");
        // Ctrl+导航键（修饰符 5 = Ctrl）
        ESC_SEQUENCES.put("[1;5H", "home");
        ESC_SEQUENCES.put("[1;5F", "end");
        ESC_SEQUENCES.put("[5;5~", "pageup");
        ESC_SEQUENCES.put("[6;5~", "pagedown");

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

        // F13-F24 (Shift+F1-F12)
        ESC_SEQUENCES.put("[1;2P", "f13");
        ESC_SEQUENCES.put("[1;2Q", "f14");
        ESC_SEQUENCES.put("[1;2R", "f15");
        ESC_SEQUENCES.put("[1;2S", "f16");
        ESC_SEQUENCES.put("[15;2~", "f17");
        ESC_SEQUENCES.put("[17;2~", "f18");
        ESC_SEQUENCES.put("[18;2~", "f19");
        ESC_SEQUENCES.put("[19;2~", "f20");
        ESC_SEQUENCES.put("[20;2~", "f21");
        ESC_SEQUENCES.put("[21;2~", "f22");
        ESC_SEQUENCES.put("[23;2~", "f23");
        ESC_SEQUENCES.put("[24;2~", "f24");

        // F25-F36 (Ctrl+F1-F12) — 部分终端支持
        ESC_SEQUENCES.put("[1;5P", "f25");
        ESC_SEQUENCES.put("[1;5Q", "f26");
        ESC_SEQUENCES.put("[1;5R", "f27");
        ESC_SEQUENCES.put("[1;5S", "f28");
        ESC_SEQUENCES.put("[15;5~", "f29");
        ESC_SEQUENCES.put("[17;5~", "f30");
        ESC_SEQUENCES.put("[18;5~", "f31");
        ESC_SEQUENCES.put("[19;5~", "f32");
        ESC_SEQUENCES.put("[20;5~", "f33");
        ESC_SEQUENCES.put("[21;5~", "f34");
        ESC_SEQUENCES.put("[23;5~", "f35");
        ESC_SEQUENCES.put("[24;5~", "f36");

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
        // F13-F24 (Shift+F1-F12)
        SHIFT_SEQUENCES.put("[1;2P", true);
        SHIFT_SEQUENCES.put("[1;2Q", true);
        SHIFT_SEQUENCES.put("[1;2R", true);
        SHIFT_SEQUENCES.put("[1;2S", true);
        SHIFT_SEQUENCES.put("[15;2~", true);
        SHIFT_SEQUENCES.put("[17;2~", true);
        SHIFT_SEQUENCES.put("[18;2~", true);
        SHIFT_SEQUENCES.put("[19;2~", true);
        SHIFT_SEQUENCES.put("[20;2~", true);
        SHIFT_SEQUENCES.put("[21;2~", true);
        SHIFT_SEQUENCES.put("[23;2~", true);
        SHIFT_SEQUENCES.put("[24;2~", true);
    }

    /** Ctrl 修饰的序列（修饰符 5 = Ctrl） */
    private static final Map<String, Boolean> CTRL_SEQUENCES = new HashMap<>();

    static {
        // F25-F36 (Ctrl+F1-F12)
        CTRL_SEQUENCES.put("[1;5P", true);
        CTRL_SEQUENCES.put("[1;5Q", true);
        CTRL_SEQUENCES.put("[1;5R", true);
        CTRL_SEQUENCES.put("[1;5S", true);
        CTRL_SEQUENCES.put("[15;5~", true);
        CTRL_SEQUENCES.put("[17;5~", true);
        CTRL_SEQUENCES.put("[18;5~", true);
        CTRL_SEQUENCES.put("[19;5~", true);
        CTRL_SEQUENCES.put("[20;5~", true);
        CTRL_SEQUENCES.put("[21;5~", true);
        CTRL_SEQUENCES.put("[23;5~", true);
        CTRL_SEQUENCES.put("[24;5~", true);
        // Ctrl+导航键
        CTRL_SEQUENCES.put("[1;5H", true);  // Ctrl+Home
        CTRL_SEQUENCES.put("[1;5F", true);  // Ctrl+End
        CTRL_SEQUENCES.put("[5;5~", true);  // Ctrl+PageUp
        CTRL_SEQUENCES.put("[6;5~", true);  // Ctrl+PageDown
    }

    /**
     * 解析转义序列后缀（ESC 之后的部分）
     */
    public static ParseResult parseEscapeSequence(String suffix) {
        // X10 鼠标事件: [M Cb Cx Cy
        if (suffix.startsWith("[M") && suffix.length() >= 5) {
            return parseMouseEvent(suffix);
        }

        // SGR 鼠标事件: [<button;col;rowM 或 [<button;col;rowm
        if (suffix.startsWith("[<") && suffix.length() > 2) {
            return parseMouseEvent(suffix);
        }

        String keyName = ESC_SEQUENCES.get(suffix);
        boolean shift = SHIFT_SEQUENCES.containsKey(suffix);
        boolean ctrl = CTRL_SEQUENCES.containsKey(suffix);

        if (keyName != null) {
            return new ParseResult(keyName, "", ctrl, shift, false);
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
     * 解析 SGR 鼠标事件: [<button;col;rowM
     */
    private static ParseResult parseMouseEvent(String suffix) {
        if (suffix.startsWith("[M") && suffix.length() >= 5) {
            int button = suffix.charAt(2) - ' ';
            if ((button & 0x40) != 0) {
                return (button & 0x01) == 0
                        ? new ParseResult("scrollUp", "", false, false, false)
                        : new ParseResult("scrollDown", "", false, false, false);
            }
            return null;
        }

        // 去掉 [< 前缀和 M/m 结尾
        String body = suffix.substring(2, suffix.length() - 1);
        String[] parts = body.split(";");
        if (parts.length >= 1) {
            try {
                int button = Integer.parseInt(parts[0]);
                // button 64=scrollUp, 65=scrollDown
                if (button == 64) return new ParseResult("scrollUp", "", false, false, false);
                if (button == 65) return new ParseResult("scrollDown", "", false, false, false);
            } catch (NumberFormatException ignored) {
            }
        }
        // 其他鼠标事件忽略
        return null;
    }

    /**
     * 解析单个控制字符
     */
    public static ParseResult parseControlChar(int ch) {
        if (ch == '\r' || ch == '\n') {
            return new ParseResult("return", "", false, false, false);
        } else if (ch == '\t') {
            return new ParseResult("tab", "", false, false, false);
        } else if (ch == 0x1b) {
            return new ParseResult("escape", "", false, false, false);
        } else if (ch == 0x7f) {
            return new ParseResult("backspace", "", false, false, false);
        } else if (ch == 0x08) {
            return new ParseResult("backspace", "", true, false, false);
        } else {
            // Ctrl+letter: 0x01-0x1a 对应 a-z
            if (ch >= 1 && ch <= 26) {
                char letter = (char) ('a' + ch - 1);
                return new ParseResult(String.valueOf(letter), "", true, false, false);
            }
            return new ParseResult(String.valueOf((char) ch), String.valueOf((char) ch),
                    false, false, false);
        }
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

        // ESC O x (SS3 序列): 需要至少 2 个字符（O + 字母）
        if (first == 'O') {
            return suffix.length() >= 2;
        }

        // ESC [ ... (CSI 序列)
        if (first == '[') {
            if (suffix.length() < 2) return false;
            // X10 鼠标序列: [M Cb Cx Cy
            if (suffix.length() >= 2 && suffix.charAt(1) == 'M') {
                return suffix.length() >= 5;
            }
            // SGR 鼠标序列: [<button;col;rowM — 以 M 或 m 结尾
            if (suffix.length() >= 2 && suffix.charAt(1) == '<') {
                char last = suffix.charAt(suffix.length() - 1);
                return last == 'M' || last == 'm';
            }
            char last = suffix.charAt(suffix.length() - 1);
            return Character.isLetter(last) || last == '~';
        }

        // 单个非前缀字符 (Meta+letter)
        return suffix.length() == 1;
    }

    /**
     * 解析结果
     */
    public static final class ParseResult {
        private final String name;
        private final String input;
        private final boolean ctrl;
        private final boolean shift;
        private final boolean meta;

        public ParseResult(String name, String input, boolean ctrl, boolean shift, boolean meta) {
            this.name = name;
            this.input = input;
            this.ctrl = ctrl;
            this.shift = shift;
            this.meta = meta;
        }

        public String name()   { return name; }
        public String input()  { return input; }
        public boolean ctrl()  { return ctrl; }
        public boolean shift() { return shift; }
        public boolean meta()  { return meta; }

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
                    ctrl, shift, meta,
                    "scrollUp".equals(name),
                    "scrollDown".equals(name)
            );
        }

        /**
         * 获取用户可见的输入文本。
         * 对于 Ctrl/Meta 组合键，返回字母本身（匹配 ink 行为，方便组件判断）。
         */
        public String inputText() {
            // 粘贴事件返回完整粘贴文本
            if ("paste".equals(name)) return input;
            switch (name) {
                case "up": case "down": case "left": case "right":
                case "pageup": case "pagedown": case "home": case "end":
                case "return": case "escape": case "tab": case "backspace": case "delete":
                case "scrollUp": case "scrollDown":
                case "f1": case "f2": case "f3": case "f4": case "f5": case "f6":
                case "f7": case "f8": case "f9": case "f10": case "f11": case "f12":
                case "f13": case "f14": case "f15": case "f16": case "f17": case "f18":
                case "f19": case "f20": case "f21": case "f22": case "f23": case "f24":
                case "f25": case "f26": case "f27": case "f28": case "f29": case "f30":
                case "f31": case "f32": case "f33": case "f34": case "f35": case "f36":
                case "insert":
                    return "";
                default:
                    return input.isEmpty() ? name : input;
            }
        }

        /** 是否为粘贴事件 */
        public boolean isPaste() {
            return "paste".equals(name);
        }
    }

    /** 创建粘贴事件的 ParseResult */
    public static ParseResult pasteResult(String pastedText) {
        return new ParseResult("paste", pastedText, false, false, false);
    }
}
