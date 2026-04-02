package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;

/**
 * 验证 CopilotDemo 的静态渲染输出
 */
public class CopilotDemoPreview {
    public static void main(String[] args) {
        int w = args.length > 0 ? Integer.parseInt(args[0]) : 80;
        int h = args.length > 1 ? Integer.parseInt(args[1]) : 24;
        CopilotDemo demo = new CopilotDemo();
        demo.setTerminalSize(w, h);
        String output = Ink.renderToString(demo, w, h);
        // 输出行数统计
        String[] lines = output.split("\n", -1);
        System.err.println("[DEBUG] Terminal: " + w + "x" + h + ", Output lines: " + lines.length);
        for (int i = 0; i < lines.length; i++) {
            System.err.println("[" + String.format("%2d", i) + "] " + lines[i].substring(0, Math.min(lines[i].length(), 60)));
        }
        System.out.println(output);
    }
}
