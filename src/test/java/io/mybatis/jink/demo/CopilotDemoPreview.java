package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;

/**
 * 验证 CopilotDemo 的静态渲染输出
 */
public class CopilotDemoPreview {
    public static void main(String[] args) {
        CopilotDemo demo = new CopilotDemo();
        demo.setTerminalSize(80, 24);
        String output = Ink.renderToString(demo, 80, 24);
        System.out.println(output);
    }
}
