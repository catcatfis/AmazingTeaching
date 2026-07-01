package com.cupk.amazingteaching.module.course.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生选课表（多对多：user ↔ course）
 */
@Data
@TableName("student_course")
public class StudentCourse implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long courseId;
    private BigDecimal progress;
    private Integer completedChapters;
    private LocalDateTime enrollTime;
    private LocalDateTime finishTime;
    private Integer status;
    private BigDecimal score;

    @TableField(exist = false)
    private String studentName;  // 学生姓名
    @TableField(exist = false)
    private String courseName;   // 课程名称
}
