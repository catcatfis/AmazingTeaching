package com.cupk.amazingteaching.module.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cupk.amazingteaching.module.exam.entity.ExamRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考试记录 Mapper
 */
@Mapper
public interface ExamRecordMapper extends BaseMapper<ExamRecord> {
}
