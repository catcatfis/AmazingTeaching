package com.cupk.amazingteaching.module.course.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.amazingteaching.common.annotation.LogOperation;
import com.cupk.amazingteaching.common.result.R;
import com.cupk.amazingteaching.module.course.entity.Course;
import com.cupk.amazingteaching.module.course.entity.StudentCourse;
import com.cupk.amazingteaching.module.course.mapper.CourseMapper;
import com.cupk.amazingteaching.module.course.mapper.StudentCourseMapper;
import com.cupk.amazingteaching.module.course.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
public class CourseController {

    private final CourseService courseService;
    private final StudentCourseMapper studentCourseMapper;
    private final CourseMapper courseMapper;

    @Operation(summary = "分页查询课程")
    @GetMapping("/page")
    public R<Page<Course>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer difficulty) {
        return R.ok(courseService.pageCourses(page, size, keyword, category, difficulty));
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
        return R.ok("新增成功", courseService.addCourse(course));
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
        long count = studentCourseMapper.selectCount(new LambdaQueryWrapper<StudentCourse>()
                .eq(StudentCourse::getStudentId, studentId)
                .eq(StudentCourse::getCourseId, courseId));
        if (count > 0) {
            return R.error("已选过该课程");
        }
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
        // 将状态设置为0（已退课）
        sc.setStatus(0);
        studentCourseMapper.updateById(sc);
        return R.ok();
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
