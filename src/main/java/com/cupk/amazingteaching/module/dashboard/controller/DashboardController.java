package com.cupk.amazingteaching.module.dashboard.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cupk.amazingteaching.common.result.R;
import com.cupk.amazingteaching.module.course.entity.Course;
import com.cupk.amazingteaching.module.course.entity.StudentCourse;
import com.cupk.amazingteaching.module.course.mapper.CourseMapper;
import com.cupk.amazingteaching.module.course.mapper.StudentCourseMapper;
import com.cupk.amazingteaching.module.course.service.CourseService;
import com.cupk.amazingteaching.module.exam.entity.ExamRecord;
import com.cupk.amazingteaching.module.exam.mapper.ExamRecordMapper;
import com.cupk.amazingteaching.module.user.entity.SysUser;
import com.cupk.amazingteaching.module.user.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据看板控制器（数据分析 + 可视化）
 */
@Tag(name = "数据看板")
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final SysUserService sysUserService;
    private final CourseService courseService;
    private final CourseMapper courseMapper;
    private final StudentCourseMapper studentCourseMapper;
    private final ExamRecordMapper examRecordMapper;

    @Operation(summary = "获取仪表盘概览数据")
    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        Map<String, Object> data = new HashMap<>();

        // 用户统计
        Map<String, Object> userStats = sysUserService.getDashboardStats();

        // 课程统计
        long totalCourses = courseMapper.selectCount(
                new LambdaQueryWrapper<Course>().eq(Course::getDeleted, 0));
        long publishedCourses = courseMapper.selectCount(
                new LambdaQueryWrapper<Course>().eq(Course::getDeleted, 0).eq(Course::getStatus, 1));

        // 选课统计
        long totalEnrollments = studentCourseMapper.selectCount(null);
        long activeStudents = studentCourseMapper.selectList(
                new LambdaQueryWrapper<StudentCourse>().eq(StudentCourse::getStatus, 1))
                .stream().map(StudentCourse::getStudentId).distinct().count();

        // 考试统计
        long totalExamRecords = examRecordMapper.selectCount(null);
        Double avgScore = examRecordMapper.selectList(null).stream()
                .filter(r -> r.getScore() != null)
                .mapToDouble(r -> r.getScore().doubleValue())
                .average().orElse(0);

        data.put("userStats", userStats);
        data.put("totalCourses", totalCourses);
        data.put("publishedCourses", publishedCourses);
        data.put("totalEnrollments", totalEnrollments);
        data.put("activeStudents", activeStudents);
        data.put("totalExamRecords", totalExamRecords);
        data.put("avgScore", Math.round(avgScore * 100.0) / 100.0);

        return R.ok(data);
    }

    @Operation(summary = "课程分类分布（饼图数据）")
    @GetMapping("/category-pie")
    public R<List<Map<String, Object>>> categoryPie() {
        return R.ok(courseService.getCategoryStats());
    }

    @Operation(summary = "课程难度分布（柱状图数据）")
    @GetMapping("/difficulty-bar")
    public R<List<Map<String, Object>>> difficultyBar() {
        return R.ok(courseService.getDifficultyStats());
    }

    @Operation(summary = "学生选课趋势（折线图数据-近12个月）")
    @GetMapping("/enrollment-trend")
    public R<List<Map<String, Object>>> enrollmentTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (int i = 11; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            LocalDateTime start = month.withDayOfMonth(1).atStartOfDay();
            LocalDateTime end = month.plusMonths(1).withDayOfMonth(1).atStartOfDay();

            long count = studentCourseMapper.selectCount(
                    new LambdaQueryWrapper<StudentCourse>()
                            .ge(StudentCourse::getEnrollTime, start)
                            .lt(StudentCourse::getEnrollTime, end));

            Map<String, Object> item = new HashMap<>();
            item.put("month", month.getYear() + "-" + String.format("%02d", month.getMonthValue()));
            item.put("count", count);
            trend.add(item);
        }
        return R.ok(trend);
    }

    @Operation(summary = "用户类型分布（饼图数据）")
    @GetMapping("/user-type-pie")
    public R<List<Map<String, Object>>> userTypePie() {
        List<Map<String, Object>> result = new ArrayList<>();
        String[] types = {"未知", "管理员", "教师", "学生"};

        List<SysUser> users = sysUserService.list(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeleted, 0));
        Map<Integer, Long> typeMap = users.stream()
                .collect(Collectors.groupingBy(SysUser::getUserType, Collectors.counting()));

        for (int i = 1; i <= 3; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", types[i]);
            item.put("value", typeMap.getOrDefault(i, 0L));
            result.add(item);
        }
        return R.ok(result);
    }

    @Operation(summary = "考试通过率趋势（折线图数据）")
    @GetMapping("/exam-pass-trend")
    public R<List<Map<String, Object>>> examPassTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        List<ExamRecord> allRecords = examRecordMapper.selectList(null);

        Map<String, List<ExamRecord>> monthlyGroups = allRecords.stream()
                .filter(r -> r.getSubmitTime() != null)
                .collect(Collectors.groupingBy(r ->
                        r.getSubmitTime().getYear() + "-" + String.format("%02d", r.getSubmitTime().getMonthValue())));

        for (Map.Entry<String, List<ExamRecord>> entry : monthlyGroups.entrySet()) {
            List<ExamRecord> records = entry.getValue();
            long passed = records.stream().filter(r -> r.getIsPassed() != null && r.getIsPassed() == 1).count();
            double rate = records.isEmpty() ? 0 : (double) passed / records.size() * 100;

            Map<String, Object> item = new HashMap<>();
            item.put("month", entry.getKey());
            item.put("passRate", Math.round(rate * 100.0) / 100.0);
            item.put("totalCount", records.size());
            trend.add(item);
        }
        trend.sort(Comparator.comparing(m -> m.get("month").toString()));
        return R.ok(trend);
    }

    @Operation(summary = "热门课程Top5（排行数据）")
    @GetMapping("/top-courses")
    public R<List<Map<String, Object>>> topCourses() {
        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>()
                        .eq(Course::getDeleted, 0)
                        .orderByDesc(Course::getStudentCount)
                        .last("LIMIT 5"));

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < courses.size(); i++) {
            Course c = courses.get(i);
            Map<String, Object> item = new HashMap<>();
            item.put("rank", i + 1);
            item.put("name", c.getCourseName());
            item.put("studentCount", c.getStudentCount());
            item.put("rating", c.getRating());
            result.add(item);
        }
        return R.ok(result);
    }
}
