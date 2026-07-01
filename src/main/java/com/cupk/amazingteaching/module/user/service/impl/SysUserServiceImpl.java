package com.cupk.amazingteaching.module.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cupk.amazingteaching.common.exception.BusinessException;
import com.cupk.amazingteaching.common.security.JwtUtils;
import com.cupk.amazingteaching.common.security.Md5PasswordEncoder;
import com.cupk.amazingteaching.module.user.entity.LoginResponse;
import com.cupk.amazingteaching.module.user.entity.SysUser;
import com.cupk.amazingteaching.module.user.mapper.SysUserMapper;
import com.cupk.amazingteaching.module.user.service.SysUserService;
import com.cupk.amazingteaching.module.role.entity.SysUserRole;
import com.cupk.amazingteaching.module.role.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final JwtUtils jwtUtils;
    private final Md5PasswordEncoder passwordEncoder;
    private final SysUserRoleMapper sysUserRoleMapper;

    @Override
    public LoginResponse login(String username, String password) {
        SysUser user = getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getDeleted, 0));

        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(403, "用户已被禁用");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        updateById(user);

        // 生成JWT
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), Map.of(
                "userType", user.getUserType(),
                "realName", user.getRealName() == null ? "" : user.getRealName()
        ));

        return new LoginResponse(token, user.getUsername(), user.getRealName(), user.getId(), user.getUserType());
    }

    @Override
    public Page<SysUser> pageUsers(int page, int size, String keyword, Integer userType) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeleted, 0)
                .eq(userType != null, SysUser::getUserType, userType)
                .and(keyword != null && !keyword.isEmpty(), w -> w
                        .like(SysUser::getUsername, keyword)
                        .or()
                        .like(SysUser::getRealName, keyword)
                        .or()
                        .like(SysUser::getPhone, keyword))
                .orderByDesc(SysUser::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public SysUser addUser(SysUser user, Long roleId) {
        // 检查用户名是否已存在
        long count = count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, user.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        save(user);

        // 分配角色
        if (roleId != null) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(roleId);
            sysUserRoleMapper.insert(userRole);
        }
        return user;
    }

    @Override
    @Transactional
    public SysUser updateUser(SysUser user, Long roleId) {
        SysUser existUser = getById(user.getId());
        if (existUser == null) {
            throw new BusinessException("用户不存在");
        }

        // 不更新密码（密码单独修改）
        user.setPassword(null);
        updateById(user);

        // 更新角色
        if (roleId != null) {
            sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getUserId, user.getId()));
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(roleId);
            sysUserRoleMapper.insert(userRole);
        }
        return getById(user.getId());
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 逻辑删除
        removeById(id);
        // 删除角色关联
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, id));
    }

    @Override
    public void resetPassword(Long id) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode("123456"));
        updateById(user);
    }

    @Override
    public void changePassword(Long userId, String oldPwd, String newPwd) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPwd, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPwd));
        updateById(user);
    }

    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        // 总用户数
        stats.put("totalUsers", count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeleted, 0)));
        // 教师数
        stats.put("teacherCount", count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeleted, 0).eq(SysUser::getUserType, 2)));
        // 学生数
        stats.put("studentCount", count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeleted, 0).eq(SysUser::getUserType, 3)));
        // 今日新增
        stats.put("todayNewUsers", count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeleted, 0)
                .ge(SysUser::getCreateTime, LocalDateTime.now().toLocalDate().atStartOfDay())));
        return stats;
    }
}
