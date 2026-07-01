package com.cupk.amazingteaching.module.permission.controller;

import com.cupk.amazingteaching.common.result.R;
import com.cupk.amazingteaching.module.permission.entity.SysPermission;
import com.cupk.amazingteaching.module.permission.mapper.SysPermissionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 权限管理控制器
 */
@Tag(name = "权限管理")
@RestController
@RequestMapping("/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final SysPermissionMapper sysPermissionMapper;

    @Operation(summary = "获取权限树")
    @GetMapping("/tree")
    public R<List<SysPermission>> tree() {
        List<SysPermission> all = sysPermissionMapper.selectList(null);
        // 构建树形结构
        Map<Long, List<SysPermission>> childrenMap = all.stream()
                .filter(p -> p.getParentId() != null && p.getParentId() != 0)
                .collect(Collectors.groupingBy(SysPermission::getParentId));

        List<SysPermission> roots = new ArrayList<>();
        for (SysPermission p : all) {
            if (p.getParentId() == 0 || p.getParentId() == null) {
                p.setChildren(buildChildren(p.getId(), childrenMap));
                roots.add(p);
            }
        }
        return R.ok(roots);
    }

    private List<SysPermission> buildChildren(Long parentId, Map<Long, List<SysPermission>> childrenMap) {
        List<SysPermission> children = childrenMap.get(parentId);
        if (children == null) return null;
        for (SysPermission child : children) {
            child.setChildren(buildChildren(child.getId(), childrenMap));
        }
        return children;
    }

    @Operation(summary = "查询所有权限")
    @GetMapping
    public R<List<SysPermission>> list() {
        return R.ok(sysPermissionMapper.selectList(null));
    }
}
