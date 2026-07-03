package com.cupk.amazingteaching.module.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cupk.amazingteaching.common.exception.BusinessException;
import com.cupk.amazingteaching.module.course.entity.CoursePrerequisite;
import com.cupk.amazingteaching.module.course.mapper.CoursePrerequisiteMapper;
import com.cupk.amazingteaching.module.course.service.CoursePrerequisiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程先修关系服务实现
 */
@Service
@RequiredArgsConstructor
public class CoursePrerequisiteServiceImpl extends ServiceImpl<CoursePrerequisiteMapper, CoursePrerequisite> 
        implements CoursePrerequisiteService {

    private final CoursePrerequisiteMapper coursePrerequisiteMapper;

    @Override
    public List<CoursePrerequisite> getPrerequisitesByCourseId(Long courseId) {
        return coursePrerequisiteMapper.selectPrerequisitesByCourseId(courseId);
    }

    @Override
    public List<CoursePrerequisite> getDependentsByCourseId(Long courseId) {
        return coursePrerequisiteMapper.selectDependentsByCourseId(courseId);
    }

    @Override
    public List<CoursePrerequisite> getAllPrerequisites() {
        return coursePrerequisiteMapper.selectAllPrerequisites();
    }

    @Override
    @Transactional
    public CoursePrerequisite addPrerequisite(Long courseId, Long prerequisiteId, 
                                            String relationType, String description, 
                                            Double confidence, String source) {
        // 检查课程是否存在
        if (courseId == null || prerequisiteId == null) {
            throw new BusinessException("课程ID不能为空");
        }
        
        // 检查是否已存在相同关系
        long count = count(new LambdaQueryWrapper<CoursePrerequisite>()
                .eq(CoursePrerequisite::getCourseId, courseId)
                .eq(CoursePrerequisite::getPrerequisiteId, prerequisiteId));
        
        if (count > 0) {
            throw new BusinessException("该先修关系已存在");
        }
        
        // 检查不能自己依赖自己
        if (courseId.equals(prerequisiteId)) {
            throw new BusinessException("课程不能将自己设为先修课程");
        }
        
        CoursePrerequisite prerequisite = new CoursePrerequisite();
        prerequisite.setCourseId(courseId);
        prerequisite.setPrerequisiteId(prerequisiteId);
        prerequisite.setRelationType(relationType != null ? relationType : "前置知识");
        prerequisite.setDescription(description);
        prerequisite.setConfidence(confidence != null ? BigDecimal.valueOf(confidence) : BigDecimal.ONE);
        prerequisite.setSource(source != null ? source : "manual");
        prerequisite.setCreateTime(LocalDateTime.now());
        
        save(prerequisite);
        return prerequisite;
    }

    @Override
    @Transactional
    public boolean removePrerequisite(Long courseId, Long prerequisiteId) {
        return remove(new LambdaQueryWrapper<CoursePrerequisite>()
                .eq(CoursePrerequisite::getCourseId, courseId)
                .eq(CoursePrerequisite::getPrerequisiteId, prerequisiteId));
    }

    @Override
    @Transactional
    public int batchAddPrerequisites(List<CoursePrerequisite> prerequisites) {
        if (prerequisites == null || prerequisites.isEmpty()) {
            return 0;
        }
        
        int count = 0;
        for (CoursePrerequisite prerequisite : prerequisites) {
            try {
                addPrerequisite(prerequisite.getCourseId(), 
                              prerequisite.getPrerequisiteId(),
                              prerequisite.getRelationType(),
                              prerequisite.getDescription(),
                              prerequisite.getConfidence() != null ? 
                                  prerequisite.getConfidence().doubleValue() : 1.0,
                              prerequisite.getSource());
                count++;
            } catch (BusinessException e) {
                // 记录日志但继续处理其他关系
                System.out.println("添加先修关系失败: " + e.getMessage());
            }
        }
        return count;
    }
}