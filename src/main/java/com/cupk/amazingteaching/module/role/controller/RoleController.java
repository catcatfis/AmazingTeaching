package com.cupk.amazingteaching.module.role.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cupk.amazingteaching.common.annotation.LogOperation;
import com.cupk.amazingteaching.common.result.R;
import com.cupk.amazingteaching.module.role.entity.SysRole;
import com.cupk.amazingteaching.module.role.entity.SysRolePermission;
import com.cupk.amazingteaching.module.role.entity.SysUserRole;
import com.cupk.amazingteaching.module.role.mapper.SysRoleMapper;
import com.cupk.amazingteaching.module.role.mapper.SysRolePermissionMapper;
import com.cupk.amazingteaching.module.role.mapper.SysUserRoleMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 */
@Tag(name = "角色管理")
@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class RoleController {

    private final SysRoleMapper sysRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    @Operation(summary = "查询所有角色")
    @GetMapping
    public R<List<SysRole>> list() {
        return R.ok(sysRoleMapper.selectList(null));
    }

    @Operation(summary = "新增角色")
    @PostMapping
    @LogOperation(module = "角色管理", operation = "ADD", description = "新增角色")
    @Transactional
    public R<SysRole> add(@RequestBody SysRole role) {
        sysRoleMapper.insert(role);
        return R.ok("新增成功", role);
    }

    @Operation(summary = "更新角色")
    @PutMapping("/{id}")
    @LogOperation(module = "角色管理", operation = "UPDATE", description = "更新角色")
    public R<SysRole> update(@PathVariable Long id, @RequestBody SysRole role) {
        role.setId(id);
        sysRoleMapper.updateById(role);
        return R.ok("更新成功", role);
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @LogOperation(module = "角色管理", operation = "DELETE", description = "删除角色")
    @Transactional
    public R<Void> delete(@PathVariable Long id) {
        sysRoleMapper.deleteById(id);
        sysRolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, id));
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, id));
        return R.ok();
    }

    @Operation(summary = "分配权限")
    @PostMapping("/{roleId}/permissions")
    @LogOperation(module = "角色管理", operation = "ASSIGN", description = "分配权限")
    @Transactional
    public R<Void> assignPermissions(@PathVariable Long roleId, @RequestBody List<Long> permIds) {
        // 删除旧权限
        sysRolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId));
        // 添加新权限
        for (Long permId : permIds) {
            SysRolePermission rp = new SysRolePermission();
            rp.setRoleId(roleId);
            rp.setPermId(permId);
            sysRolePermissionMapper.insert(rp);
        }
        return R.ok();
    }

    @Operation(summary = "查询角色权限")
    @GetMapping("/{roleId}/permissions")
    public R<List<Long>> getPermissions(@PathVariable Long roleId) {
        List<Long> permIds = sysRolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getRoleId, roleId))
                .stream().map(SysRolePermission::getPermId).toList();
        return R.ok(permIds);
    }

    @Operation(summary = "查询角色下的用户")
    @GetMapping("/{roleId}/users")
    public R<List<SysUserRole>> getUsers(@PathVariable Long roleId) {
        return R.ok(sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getRoleId, roleId)));
    }
}
