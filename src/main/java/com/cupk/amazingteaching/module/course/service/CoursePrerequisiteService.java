package com.cupk.amazingteaching.module.course.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cupk.amazingteaching.module.course.entity.CoursePrerequisite;

import java.util.List;

/**
 * 课程先修关系服务接口
 */
public interface CoursePrerequisiteService extends IService<CoursePrerequisite> {
    
    /**
     * 查询课程的所有先修课程
     * @param courseId 课程ID
     * @return 先修课程列表
     */
    List<CoursePrerequisite> getPrerequisitesByCourseId(Long courseId);
    
    /**
     * 查询课程的所有后续课程（被哪些课程依赖）
     * @param courseId 课程ID
     * @return 后续课程列表
     */
    List<CoursePrerequisite> getDependentsByCourseId(Long courseId);
    
    /**
     * 查询所有课程先修关系（用于知识图谱）
     * @return 所有先修关系
     */
    List<CoursePrerequisite> getAllPrerequisites();
    
    /**
     * 添加课程先修关系
     * @param courseId 课程ID
     * @param prerequisiteId 先修课程ID
     * @param relationType 关系类型
     * @param description 关系描述
     * @param confidence 置信度
     * @param source 来源
     * @return 保存的关系
     */
    CoursePrerequisite addPrerequisite(Long courseId, Long prerequisiteId, 
                                      String relationType, String description, 
                                      Double confidence, String source);
    
    /**
     * 删除课程先修关系
     * @param courseId 课程ID
     * @param prerequisiteId 先修课程ID
     * @return 是否删除成功
     */
    boolean removePrerequisite(Long courseId, Long prerequisiteId);
    
    /**
     * 批量添加课程先修关系
     * @param prerequisites 先修关系列表
     * @return 保存的关系数量
     */
    int batchAddPrerequisites(List<CoursePrerequisite> prerequisites);
}