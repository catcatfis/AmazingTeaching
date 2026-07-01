package com.cupk.amazingteaching.module.chapter.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 章节表（一对多：course → chapters）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("chapter")
public class Chapter implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long parentId;
    private String chapterName;
    private Integer chapterType;
    private String videoUrl;
    private String content;
    private Integer duration;
    private Integer sortOrder;
    private Integer isFree;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String courseName;  // 课程名称（关联查询）
}
