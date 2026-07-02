package com.cupk.amazingteaching.module.log.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.amazingteaching.common.result.R;
import com.cupk.amazingteaching.module.log.entity.SysLog;
import com.cupk.amazingteaching.module.log.mapper.SysLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志控制器
 */
@Tag(name = "操作日志")
@RestController
@RequestMapping("/log")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class LogController {

    private final SysLogMapper sysLogMapper;

    @Operation(summary = "分页查询日志")
    @GetMapping("/page")
    public R<Page<SysLog>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String username) {
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<SysLog>()
                .eq(module != null && !module.isEmpty(), SysLog::getModule, module)
                .like(username != null && !username.isEmpty(), SysLog::getUsername, username)
                .orderByDesc(SysLog::getCreateTime);
        return R.ok(sysLogMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @Operation(summary = "查询日志详情")
    @GetMapping("/{id}")
    public R<SysLog> detail(@PathVariable Long id) {
        return R.ok(sysLogMapper.selectById(id));
    }
}
