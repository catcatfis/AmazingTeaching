package com.cupk.amazingteaching.module.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考试记录表
 */
@Data
@TableName("exam_record")
public class ExamRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long examId;
    private Long studentId;
    private String answersJson;
    private BigDecimal score;
    private Integer isPassed;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
    private Integer status;

    @TableField(exist = false)
    private String examName;
    @TableField(exist = false)
    private String studentName;
}
