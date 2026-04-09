package io.mybatis.jink.ui;

/**
 * 密码输入组件。
 *
 * <p>与 {@link TextInput} 完全相同，只是将输入内容显示为 {@code *}（不显示真实字符）。
 *
 * <p>使用示例：
 * <pre>
 * PasswordInput pwd = new PasswordInput.Builder()
 *     .placeholder("输入密码...")
 *     .onSubmit(password -> authenticate(password))
 *     .build();
 * </pre>
 */
public class PasswordInput extends TextInput {

    protected PasswordInput(Builder builder) {
        super(builder);
    }

    /** 创建 PasswordInput Builder */
    public static Builder builder() {
        return new Builder();
    }

    /** PasswordInput 构建器 */
    public static class Builder extends TextInput.Builder {

        public Builder() {
            // 默认使用 * 作为掩码字符
            this.maskChar = '*';
        }

        @Override
        public Builder placeholder(String placeholder) {
            super.placeholder(placeholder);
            return this;
        }

        @Override
        public Builder maxLength(int cols) {
            super.maxLength(cols);
            return this;
        }

        @Override
        public Builder value(String value) {
            super.value(value);
            return this;
        }

        @Override
        public Builder onChange(TextInput.Callback onChange) {
            super.onChange(onChange);
            return this;
        }

        @Override
        public Builder onSubmit(TextInput.Callback onSubmit) {
            super.onSubmit(onSubmit);
            return this;
        }

        @Override
        public Builder showBorder(boolean show) {
            super.showBorder(show);
            return this;
        }

        /** 自定义掩码字符（默认 '*'） */
        public Builder maskWith(char c) {
            this.maskChar = c;
            return this;
        }

        @Override
        public PasswordInput build() {
            return new PasswordInput(this);
        }
    }
}
