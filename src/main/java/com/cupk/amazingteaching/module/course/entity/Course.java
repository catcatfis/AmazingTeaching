package com.cupk.amazingteaching.module.course.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("course")
public class Course implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String courseName;
    private Long teacherId;
    private String category;
    private String coverUrl;
    private String description;
    private BigDecimal price;
    private Integer totalHours;
    private Integer studentCount;
    private BigDecimal rating;
    private String tags;
    private Integer difficulty;
    private Integer status;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private String teacherName;  // 教师姓名（关联查询）
}
