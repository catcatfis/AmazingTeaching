package com.cupk.amazingteaching.module.user.controller;

import com.cupk.amazingteaching.common.result.R;
import com.cupk.amazingteaching.module.user.entity.LoginRequest;
import com.cupk.amazingteaching.module.user.entity.LoginResponse;
import com.cupk.amazingteaching.module.user.entity.SysUser;
import com.cupk.amazingteaching.module.user.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器（公开接口）
 */
@Tag(name = "认证管理")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse result = sysUserService.login(request.getUsername(), request.getPassword());
        return R.ok("登录成功", result);
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<SysUser> register(@RequestBody SysUser user) {
        SysUser result = sysUserService.addUser(user, 3L); // 默认学生角色
        return R.ok("注册成功", result);
    }
}
