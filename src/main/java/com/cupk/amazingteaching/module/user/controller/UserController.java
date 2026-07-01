package com.cupk.amazingteaching.module.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.amazingteaching.common.annotation.LogOperation;
import com.cupk.amazingteaching.common.result.R;
import com.cupk.amazingteaching.module.user.entity.SysUser;
import com.cupk.amazingteaching.module.user.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户管理控制器
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService sysUserService;

    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TEACHER')")
    public R<Page<SysUser>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer userType) {
        return R.ok(sysUserService.pageUsers(page, size, keyword, userType));
    }

    @Operation(summary = "根据ID查询用户")
    @GetMapping("/{id}")
    public R<SysUser> getById(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        user.setPassword(null); // 不返回密码
        return R.ok(user);
    }

    @Operation(summary = "新增用户")
    @PostMapping
    @LogOperation(module = "用户管理", operation = "ADD", description = "新增用户")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public R<SysUser> add(@RequestBody SysUser user, @RequestParam(required = false) Long roleId) {
        SysUser result = sysUserService.addUser(user, roleId);
        result.setPassword(null);
        return R.ok("新增成功", result);
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    @LogOperation(module = "用户管理", operation = "UPDATE", description = "更新用户")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public R<SysUser> update(@PathVariable Long id, @RequestBody SysUser user,
                              @RequestParam(required = false) Long roleId) {
        user.setId(id);
        SysUser result = sysUserService.updateUser(user, roleId);
        result.setPassword(null);
        return R.ok("更新成功", result);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @LogOperation(module = "用户管理", operation = "DELETE", description = "删除用户")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        sysUserService.deleteUser(id);
        return R.ok();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/{id}/reset-password")
    @LogOperation(module = "用户管理", operation = "RESET", description = "重置密码")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public R<Void> resetPassword(@PathVariable Long id) {
        sysUserService.resetPassword(id);
        return R.ok();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/change-password")
    public R<Void> changePassword(@RequestParam Long userId,
                                   @RequestParam String oldPwd,
                                   @RequestParam String newPwd) {
        sysUserService.changePassword(userId, oldPwd, newPwd);
        return R.ok();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/current")
    public R<SysUser> currentUser(@RequestParam Long userId) {
        SysUser user = sysUserService.getById(userId);
        if (user != null) user.setPassword(null);
        return R.ok(user);
    }
}
