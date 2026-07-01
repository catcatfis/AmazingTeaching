package com.cupk.amazingteaching.module.user.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应
 */
@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String username;
    private String realName;
    private Long userId;
    private Integer userType;
}
