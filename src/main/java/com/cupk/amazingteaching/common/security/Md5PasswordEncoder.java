package com.cupk.amazingteaching.common.security;

import cn.hutool.crypto.digest.MD5;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * MD5 密码编码器（盐值 + MD5 双重加密）
 * 使用固定盐值 AmazingTeaching@2025 增强安全性
 */
@Component
public class Md5PasswordEncoder implements PasswordEncoder {

    private static final String SALT = "AmazingTeaching@2025";

    @Override
    public String encode(CharSequence rawPassword) {
        // 盐值 + MD5 加密
        return MD5.create().digestHex(SALT + rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return encode(rawPassword).equals(encodedPassword);
    }
}
