package com.cupk.amazingteaching.module.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cupk.amazingteaching.module.course.entity.CoursePrerequisite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 课程先修关系 Mapper
 */
@Mapper
public interface CoursePrerequisiteMapper extends BaseMapper<CoursePrerequisite> {
    
    /**
     * 查询课程的所有先修课程
     * @param courseId 课程ID
     * @return 先修课程列表
     */
    @Select("SELECT cp.*, c.course_name as prerequisite_name " +
            "FROM course_prerequisite cp " +
            "LEFT JOIN course c ON cp.prerequisite_id = c.id " +
            "WHERE cp.course_id = #{courseId} AND c.deleted = 0")
    List<CoursePrerequisite> selectPrerequisitesByCourseId(@Param("courseId") Long courseId);
    
    /**
     * 查询课程的所有后续课程（被哪些课程依赖）
     * @param courseId 课程ID
     * @return 后续课程列表
     */
    @Select("SELECT cp.*, c.course_name as course_name " +
            "FROM course_prerequisite cp " +
            "LEFT JOIN course c ON cp.course_id = c.id " +
            "WHERE cp.prerequisite_id = #{courseId} AND c.deleted = 0")
    List<CoursePrerequisite> selectDependentsByCourseId(@Param("courseId") Long courseId);
    
    /**
     * 查询所有课程先修关系（用于知识图谱）
     * @return 所有先修关系
     */
    @Select("SELECT cp.*, c1.course_name as course_name, c2.course_name as prerequisite_name " +
            "FROM course_prerequisite cp " +
            "LEFT JOIN course c1 ON cp.course_id = c1.id " +
            "LEFT JOIN course c2 ON cp.prerequisite_id = c2.id " +
            "WHERE c1.deleted = 0 AND c2.deleted = 0")
    List<CoursePrerequisite> selectAllPrerequisites();
}