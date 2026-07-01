package com.cupk.amazingteaching.module.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cupk.amazingteaching.module.log.entity.SysLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 Mapper
 */
@Mapper
public interface SysLogMapper extends BaseMapper<SysLog> {
}
