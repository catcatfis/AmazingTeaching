package com.cupk.amazingteaching.module.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cupk.amazingteaching.module.user.entity.LoginResponse;
import com.cupk.amazingteaching.module.user.entity.SysUser;

import java.util.Map;

/**
 * 用户服务接口
 */
public interface SysUserService extends IService<SysUser> {

    /** 用户登录 */
    LoginResponse login(String username, String password);

    /** 分页查询用户列表 */
    Page<SysUser> pageUsers(int page, int size, String keyword, Integer userType);

    /** 新增用户 */
    SysUser addUser(SysUser user, Long roleId);

    /** 更新用户 */
    SysUser updateUser(SysUser user, Long roleId);

    /** 删除用户 */
    void deleteUser(Long id);

    /** 重置密码 */
    void resetPassword(Long id);

    /** 修改密码 */
    void changePassword(Long userId, String oldPwd, String newPwd);

    /** 获取仪表盘统计数据 */
    Map<String, Object> getDashboardStats();
}
