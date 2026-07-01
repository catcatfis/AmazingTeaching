package com.cupk.amazingteaching.module.chapter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cupk.amazingteaching.module.chapter.entity.Chapter;
import org.apache.ibatis.annotations.Mapper;

/**
 * 章节 Mapper
 */
@Mapper
public interface ChapterMapper extends BaseMapper<Chapter> {
}
