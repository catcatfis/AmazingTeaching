package com.cupk.amazingteaching.module.course.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程先修关系表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("course_prerequisite")
public class CoursePrerequisite implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 当前课程ID
     */
    private Long courseId;
    
    /**
     * 先修课程ID
     */
    private Long prerequisiteId;
    
    /**
     * 关系类型：前置知识、依赖关系、应用场景、扩展关系
     */
    private String relationType;
    
    /**
     * 关系描述
     */
    private String description;
    
    /**
     * 置信度（0-1）
     */
    private BigDecimal confidence;
    
    /**
     * 来源：manual-手动，ai-自动生成
     */
    private String source;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    @TableField(exist = false)
    private String courseName;  // 当前课程名称（关联查询）
    
    @TableField(exist = false)
    private String prerequisiteName;  // 先修课程名称（关联查询）
}