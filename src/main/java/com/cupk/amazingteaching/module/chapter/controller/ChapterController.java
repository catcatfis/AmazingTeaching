package com.cupk.amazingteaching.module.chapter.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cupk.amazingteaching.common.annotation.LogOperation;
import com.cupk.amazingteaching.common.result.R;
import com.cupk.amazingteaching.module.chapter.entity.Chapter;
import com.cupk.amazingteaching.module.chapter.mapper.ChapterMapper;
import com.cupk.amazingteaching.module.course.entity.Course;
import com.cupk.amazingteaching.module.course.mapper.CourseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 章节管理控制器
 */
@Tag(name = "章节管理")
@RestController
@RequestMapping("/chapter")
@RequiredArgsConstructor
public class ChapterController {

    private final ChapterMapper chapterMapper;
    private final CourseMapper courseMapper;

    @Operation(summary = "查询课程的所有章节（树形结构）")
    @GetMapping("/course/{courseId}")
    public R<List<Chapter>> tree(@PathVariable Long courseId) {
        List<Chapter> all = chapterMapper.selectList(
                new LambdaQueryWrapper<Chapter>()
                        .eq(Chapter::getCourseId, courseId)
                        .orderByAsc(Chapter::getSortOrder));

        // 构建树：一级章节 parentId=0
        List<Chapter> roots = all.stream()
                .filter(c -> c.getParentId() == 0)
                .toList();
        // 填充课程名
        Course course = courseMapper.selectById(courseId);
        for (Chapter chapter : all) {
            chapter.setCourseName(course != null ? course.getCourseName() : "");
        }
        return R.ok(roots);
    }

    @Operation(summary = "查询章节详情")
    @GetMapping("/{id}")
    public R<Chapter> detail(@PathVariable Long id) {
        return R.ok(chapterMapper.selectById(id));
    }

    @Operation(summary = "新增章节")
    @PostMapping
    @LogOperation(module = "章节管理", operation = "ADD", description = "新增章节")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TEACHER')")
    public R<Chapter> add(@RequestBody Chapter chapter) {
        chapterMapper.insert(chapter);
        return R.ok("新增成功", chapter);
    }

    @Operation(summary = "更新章节")
    @PutMapping("/{id}")
    @LogOperation(module = "章节管理", operation = "UPDATE", description = "更新章节")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TEACHER')")
    public R<Chapter> update(@PathVariable Long id, @RequestBody Chapter chapter) {
        chapter.setId(id);
        chapterMapper.updateById(chapter);
        return R.ok("更新成功", chapter);
    }

    @Operation(summary = "删除章节")
    @DeleteMapping("/{id}")
    @LogOperation(module = "章节管理", operation = "DELETE", description = "删除章节")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TEACHER')")
    public R<Void> delete(@PathVariable Long id) {
        chapterMapper.deleteById(id);
        return R.ok();
    }
}
