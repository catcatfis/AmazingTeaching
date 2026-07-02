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
        // 安全检查：不允许注册管理员账户
        if (user.getUserType() != null && user.getUserType() == 1) {
            return R.error(400, "不允许注册管理员账户");
        }
        // 根据用户类型分配对应角色
        Long roleId = 3L; // 默认学生角色
        if (user.getUserType() != null && user.getUserType() == 2) {
            roleId = 2L; // 教师角色
        }
        SysUser result = sysUserService.addUser(user, roleId);
        return R.ok("注册成功", result);
    }
}
