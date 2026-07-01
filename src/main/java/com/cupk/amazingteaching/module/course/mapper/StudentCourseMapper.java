package com.cupk.amazingteaching.module.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cupk.amazingteaching.module.course.entity.StudentCourse;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学生选课 Mapper
 */
@Mapper
public interface StudentCourseMapper extends BaseMapper<StudentCourse> {
}
