package com.cupk.amazingteaching.module.course.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cupk.amazingteaching.module.course.entity.Course;

import java.util.List;
import java.util.Map;

/**
 * 课程服务接口
 */
public interface CourseService extends IService<Course> {

    /** 分页查询课程 */
    Page<Course> pageCourses(int page, int size, String keyword, String category, Integer difficulty, Integer status);
    
    /** 获取课程列表（用于知识图谱等场景） */
    Page<Course> listCourses(int page, int size, String keyword, String category, Integer difficulty, Integer status);

    /** 获取课程详情（含教师信息） */
    Course getCourseDetail(Long id);

    /** 新增课程 */
    Course addCourse(Course course);

    /** 推荐课程（基于协同过滤的推荐算法） */
    List<Course> recommendCourses(Long studentId, int limit);

    /** 获取热门课程 */
    List<Course> getHotCourses(int limit);

    /** 获取分类统计 */
    List<Map<String, Object>> getCategoryStats();

    /** 获取难度分布 */
    List<Map<String, Object>> getDifficultyStats();

    /** 发布课程 */
    Course publishCourse(Long id);
}
