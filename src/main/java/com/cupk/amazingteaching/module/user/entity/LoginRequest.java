package com.cupk.amazingteaching.module.user.entity;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 用户登录请求
 */
@Data
@Accessors(chain = true)
public class LoginRequest {
    private String username;
    private String password;
}
