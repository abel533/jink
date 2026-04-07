package io.mybatis.jink.demo;

import io.mybatis.jink.Ink;
import io.mybatis.jink.component.Box;
import io.mybatis.jink.component.Renderable;
import io.mybatis.jink.component.Text;
import io.mybatis.jink.style.FlexDirection;

/**
 * ink 官方示例 table 的 jink 等效实现。
 *
 * <p>使用 Box 百分比宽度模拟表格布局，展示用户数据（ID / Name / Email 三列）。
 * 对应 ink 原版的固定宽度 Box 列布局。
 *
 * <p>运行:
 * <pre>
 * mvn exec:java -Dexec.mainClass=io.mybatis.jink.demo.TableDemo -Dexec.classpathScope=test
 * </pre>
 */
public class TableDemo {

    static final class User {
        private final int id;
        private final String name;
        private final String email;
        User(int id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
        int id() { return id; }
        String name() { return name; }
        String email() { return email; }
    }

    private static final User[] USERS = {
        new User(0, "alice_dev",    "alice@example.com"),
        new User(1, "bob_builder",  "bob@example.com"),
        new User(2, "carol_qi",     "carol@example.com"),
        new User(3, "david_ops",    "david@example.com"),
        new User(4, "eve_sec",      "eve@example.com"),
        new User(5, "frank_data",   "frank@example.com"),
        new User(6, "grace_ux",     "grace@example.com"),
        new User(7, "henry_ml",     "henry@example.com"),
        new User(8, "iris_cloud",   "iris@example.com"),
        new User(9, "jack_mobile",  "jack@example.com"),
    };

    static Renderable row(String id, String name, String email) {
        return Box.of(
                Box.of(Text.of(id)).width(8),
                Box.of(Text.of(name)).width(40),
                Box.of(Text.of(email)).width(32)
        );
    }

    static Renderable build() {
        Box table = Box.of(row("ID", "Name", "Email"))
                .flexDirection(FlexDirection.COLUMN).width(80);
        for (User u : USERS) {
            table.add(row(String.valueOf(u.id()), u.name(), u.email()));
        }
        return table;
    }

    public static void main(String[] args) {
        Ink.renderOnce(build(), 80, 24);
    }
}
