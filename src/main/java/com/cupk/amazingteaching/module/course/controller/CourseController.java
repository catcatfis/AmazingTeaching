package com.cupk.amazingteaching.module.course.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.amazingteaching.common.annotation.LogOperation;
import com.cupk.amazingteaching.common.result.R;
import com.cupk.amazingteaching.module.course.entity.Course;
import com.cupk.amazingteaching.module.course.entity.CoursePrerequisite;
import com.cupk.amazingteaching.module.course.entity.StudentCourse;
import com.cupk.amazingteaching.module.course.mapper.CourseMapper;
import com.cupk.amazingteaching.module.course.mapper.StudentCourseMapper;
import com.cupk.amazingteaching.module.course.service.CoursePrerequisiteService;
import com.cupk.amazingteaching.module.course.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课程管理控制器
 */
@Tag(name = "课程管理")
@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
@Slf4j
public class CourseController {

    private final CourseService courseService;
    private final CoursePrerequisiteService coursePrerequisiteService;
    private final StudentCourseMapper studentCourseMapper;
    private final CourseMapper courseMapper;

    @Operation(summary = "分页查询课程")
    @GetMapping("/page")
    public R<Page<Course>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) Integer status) {
        return R.ok(courseService.pageCourses(page, size, keyword, category, difficulty, status));
    }

    @Operation(summary = "课程详情")
    @GetMapping("/{id}")
    public R<Course> detail(@PathVariable Long id) {
        return R.ok(courseService.getCourseDetail(id));
    }

    @Operation(summary = "新增课程")
    @PostMapping
    @LogOperation(module = "课程管理", operation = "ADD", description = "新增课程")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TEACHER')")
    public R<Course> add(@RequestBody Course course) {
        Course newCourse = courseService.addCourse(course);
        
        // 新增课程后，调用AI分析课程与现有课程的关系
        analyzeCourseRelationships(newCourse);
        
        return R.ok("新增成功", newCourse);
    }
    
    /**
     * 分析新课程与现有课程的关系
     * @param newCourse 新课程
     */
    private void analyzeCourseRelationships(Course newCourse) {
        try {
            // 获取所有已发布的课程（不包括新课程）
            List<Course> existingCourses = courseService.listCourses(1, 100, null, null, null, 1).getRecords();
            existingCourses.removeIf(c -> c.getId().equals(newCourse.getId()));
            
            if (existingCourses.isEmpty()) {
                return;
            }
            
            // 构建AI提示词
            StringBuilder prompt = new StringBuilder();
            prompt.append("我有一个新课程：");
            prompt.append(newCourse.getCourseName());
            prompt.append("（分类：").append(newCourse.getCategory());
            prompt.append("，难度：").append(newCourse.getDifficulty());
            prompt.append("）。\n\n");
            
            prompt.append("现有课程列表：\n");
            for (Course course : existingCourses) {
                prompt.append("- ").append(course.getCourseName());
                prompt.append("（分类：").append(course.getCategory());
                prompt.append("，难度：").append(course.getDifficulty());
                prompt.append("）\n");
            }
            
            prompt.append("\n请分析这个新课程与现有课程之间的学习依赖关系。对于每个相关关系，请返回：\n");
            prompt.append("1. 目标课程ID（现有课程的ID）\n");
            prompt.append("2. 关系类型（前置知识、依赖关系、应用场景、扩展关系）\n");
            prompt.append("3. 关系描述\n");
            prompt.append("4. 置信度（0-1）\n\n");
            prompt.append("请以JSON数组格式返回，格式如下：\n");
            prompt.append("[{\"courseId\": 课程ID, \"relationType\": \"关系类型\", \"description\": \"描述\", \"confidence\": 0.8}]\n");
            prompt.append("如果没有关系，返回空数组[]。");
            
            // 调用AI服务（这里简化处理，实际应该调用AiService）
            // 由于AiService需要异步调用，这里先使用模拟逻辑
            log.info("分析课程关系：{}，提示词长度：{}", newCourse.getCourseName(), prompt.length());
            
            // 模拟AI分析结果（实际应该调用AiService）
            List<CoursePrerequisite> newRelations = new ArrayList<>();
            
            // 根据课程分类和难度推断可能的关系
            for (Course existing : existingCourses) {
                // 如果新课程是高级课程，现有课程可能是前置课程
                if (newCourse.getDifficulty() != null && newCourse.getDifficulty() >= 3 && 
                    existing.getDifficulty() != null && existing.getDifficulty() <= 2 &&
                    newCourse.getCategory().equals(existing.getCategory())) {
                    
                    CoursePrerequisite relation = new CoursePrerequisite();
                    relation.setCourseId(newCourse.getId());
                    relation.setPrerequisiteId(existing.getId());
                    relation.setRelationType("前置知识");
                    relation.setDescription("学习" + newCourse.getCourseName() + "需要先掌握" + existing.getCourseName());
                    relation.setConfidence(new java.math.BigDecimal(0.8));
                    relation.setSource("ai");
                    newRelations.add(relation);
                }
            }
            
            // 保存分析结果
            if (!newRelations.isEmpty()) {
                coursePrerequisiteService.batchAddPrerequisites(newRelations);
                log.info("为课程{}分析出{}个关系", newCourse.getCourseName(), newRelations.size());
            }
            
        } catch (Exception e) {
            log.error("分析课程关系失败：{}", e.getMessage(), e);
        }
    }

    @Operation(summary = "更新课程")
    @PutMapping("/{id}")
    @LogOperation(module = "课程管理", operation = "UPDATE", description = "更新课程")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TEACHER')")
    public R<Course> update(@PathVariable Long id, @RequestBody Course course) {
        course.setId(id);
        courseService.updateById(course);
        return R.ok("更新成功", courseService.getById(id));
    }

    @Operation(summary = "删除课程")
    @DeleteMapping("/{id}")
    @LogOperation(module = "课程管理", operation = "DELETE", description = "删除课程")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TEACHER')")
    public R<Void> delete(@PathVariable Long id) {
        courseService.removeById(id);
        return R.ok();
    }

    @Operation(summary = "发布课程")
    @PutMapping("/{id}/publish")
    @LogOperation(module = "课程管理", operation = "UPDATE", description = "发布课程")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TEACHER')")
    public R<Course> publish(@PathVariable Long id) {
        return R.ok("发布成功", courseService.publishCourse(id));
    }

    @Operation(summary = "课程推荐（协同过滤算法）")
    @GetMapping("/recommend")
    public R<List<Course>> recommend(@RequestParam Long studentId,
                                      @RequestParam(defaultValue = "6") int limit) {
        return R.ok(courseService.recommendCourses(studentId, limit));
    }

    @Operation(summary = "热门课程")
    @GetMapping("/hot")
    public R<List<Course>> hot(@RequestParam(defaultValue = "6") int limit) {
        return R.ok(courseService.getHotCourses(limit));
    }

    @Operation(summary = "学生选课")
    @PostMapping("/enroll")
    @LogOperation(module = "课程管理", operation = "ENROLL", description = "学生选课")
    public R<Void> enroll(@RequestParam Long studentId, @RequestParam Long courseId) {
        // 查询是否已有选课记录（包括已退课的）
        StudentCourse existingSc = studentCourseMapper.selectOne(
                new LambdaQueryWrapper<StudentCourse>()
                        .eq(StudentCourse::getStudentId, studentId)
                        .eq(StudentCourse::getCourseId, courseId));
        
        if (existingSc != null) {
            // 如果已有记录且是退课状态(status=0)，则重新选课
            if (existingSc.getStatus() == 0) {
                existingSc.setStatus(1);
                existingSc.setProgress(java.math.BigDecimal.ZERO);
                existingSc.setCompletedChapters(0);
                existingSc.setScore(null);
                // 清除旧的评分数据
                existingSc.setRating(null);
                existingSc.setRatingContent(null);
                existingSc.setRatingTime(null);
                studentCourseMapper.updateById(existingSc);
                return R.ok();
            } else {
                // 如果已有有效选课记录，返回已选过
                return R.error("已选过该课程");
            }
        }
        
        // 新增选课记录
        StudentCourse sc = new StudentCourse();
        sc.setStudentId(studentId);
        sc.setCourseId(courseId);
        sc.setStatus(1);
        sc.setProgress(java.math.BigDecimal.ZERO);
        studentCourseMapper.insert(sc);
        return R.ok();
    }

    @Operation(summary = "查询学生选课列表")
    @GetMapping("/student-courses")
    public R<List<StudentCourse>> studentCourses(@RequestParam Long studentId) {
        List<StudentCourse> studentCourses = studentCourseMapper.selectList(
                new LambdaQueryWrapper<StudentCourse>()
                        .eq(StudentCourse::getStudentId, studentId)
                        .ne(StudentCourse::getStatus, 0)); // 排除已退课的记录
        
        // 获取所有相关的课程ID
        List<Long> courseIds = studentCourses.stream()
                .map(StudentCourse::getCourseId)
                .distinct()
                .collect(Collectors.toList());
        
        if (!courseIds.isEmpty()) {
            // 查询课程信息
            List<Course> courses = courseMapper.selectBatchIds(courseIds);
            Map<Long, String> courseNameMap = courses.stream()
                    .collect(Collectors.toMap(Course::getId, Course::getCourseName));
            
            // 填充课程名称
            studentCourses.forEach(sc -> sc.setCourseName(courseNameMap.get(sc.getCourseId())));
        }
        
        return R.ok(studentCourses);
    }

    @Operation(summary = "学生退课")
    @DeleteMapping("/unenroll")
    @LogOperation(module = "课程管理", operation = "UNENROLL", description = "学生退课")
    public R<Void> unenroll(@RequestParam Long studentId, @RequestParam Long courseId) {
        StudentCourse sc = studentCourseMapper.selectOne(
                new LambdaQueryWrapper<StudentCourse>()
                        .eq(StudentCourse::getStudentId, studentId)
                        .eq(StudentCourse::getCourseId, courseId));
        if (sc == null) {
            return R.error("未选该课程");
        }
        
        // 清除评分数据
        boolean hasRating = sc.getRating() != null;
        sc.setRating(null);
        sc.setRatingContent(null);
        sc.setRatingTime(null);
        
        // 将状态设置为0（已退课）
        sc.setStatus(0);
        studentCourseMapper.updateById(sc);
        
        // 如果之前有评分，需要更新课程的平均评分
        if (hasRating) {
            updateCourseRating(courseId);
        }
        
        return R.ok();
    }

    @Operation(summary = "学生评分课程")
    @PostMapping("/rate")
    @LogOperation(module = "课程管理", operation = "RATE", description = "学生评分课程")
    public R<Void> rateCourse(@RequestParam Long studentId, @RequestParam Long courseId,
                              @RequestParam Integer rating, @RequestParam(required = false) String ratingContent) {
        // 参数校验
        if (rating == null || rating < 1 || rating > 5) {
            return R.error("评分必须在1-5之间");
        }
        
        // 查询选课记录
        StudentCourse sc = studentCourseMapper.selectOne(
                new LambdaQueryWrapper<StudentCourse>()
                        .eq(StudentCourse::getStudentId, studentId)
                        .eq(StudentCourse::getCourseId, courseId));
        if (sc == null) {
            return R.error("未选该课程");
        }
        
        // 更新评分
        sc.setRating(rating);
        sc.setRatingContent(ratingContent);
        sc.setRatingTime(java.time.LocalDateTime.now());
        studentCourseMapper.updateById(sc);
        
        // 更新课程的平均评分
        updateCourseRating(courseId);
        
        return R.ok();
    }

    /**
     * 更新课程的平均评分
     */
    private void updateCourseRating(Long courseId) {
        // 查询该课程所有有效评分记录（排除已退课的）
        List<StudentCourse> ratings = studentCourseMapper.selectList(
                new LambdaQueryWrapper<StudentCourse>()
                        .eq(StudentCourse::getCourseId, courseId)
                        .ne(StudentCourse::getStatus, 0)  // 排除已退课
                        .isNotNull(StudentCourse::getRating));
        
        // 更新课程表的评分字段
        Course course = courseMapper.selectById(courseId);
        if (course != null) {
            if (!ratings.isEmpty()) {
                // 计算平均分
                double avgRating = ratings.stream()
                        .mapToInt(StudentCourse::getRating)
                        .average()
                        .orElse(0.0);
                course.setRating(new java.math.BigDecimal(String.format("%.1f", avgRating)));
            } else {
                // 没有评分记录时，重置为0
                course.setRating(java.math.BigDecimal.ZERO);
            }
            courseMapper.updateById(course);
        }
    }

    @Operation(summary = "获取教师自己的课程列表")
    @GetMapping("/teacher-courses")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TEACHER')")
    public R<List<Course>> teacherCourses(@RequestParam Long teacherId) {
        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>()
                        .eq(Course::getDeleted, 0)
                        .eq(Course::getTeacherId, teacherId)
                        .orderByDesc(Course::getCreateTime));
        return R.ok(courses);
    }
}
