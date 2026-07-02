package com.cupk.amazingteaching.module.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.amazingteaching.common.annotation.LogOperation;
import com.cupk.amazingteaching.common.result.R;
import com.cupk.amazingteaching.module.course.entity.Course;
import com.cupk.amazingteaching.module.course.mapper.CourseMapper;
import com.cupk.amazingteaching.module.exam.entity.Exam;
import com.cupk.amazingteaching.module.exam.entity.ExamRecord;
import com.cupk.amazingteaching.module.exam.mapper.ExamMapper;
import com.cupk.amazingteaching.module.exam.mapper.ExamRecordMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 考试管理控制器
 */
@Tag(name = "考试管理")
@RestController
@RequestMapping("/exam")
@RequiredArgsConstructor
public class ExamController {

    private final ExamMapper examMapper;
    private final ExamRecordMapper examRecordMapper;
    private final CourseMapper courseMapper;

    @Operation(summary = "分页查询考试")
    @GetMapping("/page")
    public R<Page<Exam>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long courseId) {
        LambdaQueryWrapper<Exam> wrapper = new LambdaQueryWrapper<Exam>()
                .eq(courseId != null, Exam::getCourseId, courseId)
                .orderByDesc(Exam::getCreateTime);
        Page<Exam> result = examMapper.selectPage(new Page<>(page, size), wrapper);
        // 根据当前时间动态计算考试状态
        LocalDateTime now = LocalDateTime.now();
        // 填充课程名称
        List<Long> courseIds = result.getRecords().stream()
                .map(Exam::getCourseId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> courseNameMap = new java.util.HashMap<>();
        if (!courseIds.isEmpty()) {
            List<Course> courses = courseMapper.selectBatchIds(courseIds);
            courseNameMap = courses.stream()
                    .collect(Collectors.toMap(Course::getId, Course::getCourseName));
        }
        for (Exam exam : result.getRecords()) {
            exam.setStatus(calculateExamStatus(exam, now));
            if (exam.getCourseId() != null) {
                exam.setCourseName(courseNameMap.get(exam.getCourseId()));
            }
        }
        return R.ok(result);
    }

    /**
     * 根据当前时间动态计算考试状态
     * @param exam 考试对象
     * @param now 当前时间
     * @return 状态：0-未开始，1-进行中，2-已结束
     */
    private Integer calculateExamStatus(Exam exam, LocalDateTime now) {
        if (exam.getStartTime() == null || exam.getEndTime() == null) {
            // 如果没有设置时间，使用数据库中的状态
            return exam.getStatus();
        }
        if (now.isBefore(exam.getStartTime())) {
            return 0; // 未开始
        } else if (now.isAfter(exam.getEndTime())) {
            return 2; // 已结束
        } else {
            return 1; // 进行中
        }
    }

    @Operation(summary = "考试详情")
    @GetMapping("/{id}")
    public R<Exam> detail(@PathVariable Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam != null) {
            exam.setStatus(calculateExamStatus(exam, LocalDateTime.now()));
            if (exam.getCourseId() != null) {
                Course course = courseMapper.selectById(exam.getCourseId());
                if (course != null) {
                    exam.setCourseName(course.getCourseName());
                }
            }
        }
        return R.ok(exam);
    }

    @Operation(summary = "创建考试")
    @PostMapping
    @LogOperation(module = "考试管理", operation = "ADD", description = "创建考试")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TEACHER')")
    public R<Exam> add(@RequestBody Exam exam) {
        examMapper.insert(exam);
        return R.ok("创建成功", exam);
    }

    @Operation(summary = "更新考试")
    @PutMapping("/{id}")
    @LogOperation(module = "考试管理", operation = "UPDATE", description = "更新考试")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TEACHER')")
    public R<Exam> update(@PathVariable Long id, @RequestBody Exam exam) {
        exam.setId(id);
        examMapper.updateById(exam);
        return R.ok("更新成功", examMapper.selectById(id));
    }

    @Operation(summary = "删除考试")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TEACHER')")
    public R<Void> delete(@PathVariable Long id) {
        examMapper.deleteById(id);
        return R.ok();
    }

    @Operation(summary = "提交考试")
    @PostMapping("/submit")
    @LogOperation(module = "考试管理", operation = "SUBMIT", description = "提交考试")
    public R<ExamRecord> submit(@RequestBody ExamRecord record) {
        record.setSubmitTime(LocalDateTime.now());
        record.setStatus(2);
        
        // 使用前端传来的分数（已由前端计算：答对题数 × 每题分值）
        Exam exam = examMapper.selectById(record.getExamId());
        if (exam != null && record.getScore() != null) {
            record.setIsPassed(record.getScore().compareTo(new BigDecimal(exam.getPassScore())) >= 0 ? 1 : 0);
        }
        
        // 检查是否已存在记录，存在则更新，不存在则插入
        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, record.getExamId())
                .eq(ExamRecord::getStudentId, record.getStudentId());
        ExamRecord existing = examRecordMapper.selectOne(wrapper);
        
        if (existing != null) {
            // 更新已有记录
            record.setId(existing.getId());
            examRecordMapper.updateById(record);
        } else {
            // 插入新记录
            examRecordMapper.insert(record);
        }
        
        return R.ok("提交成功", record);
    }

    @Operation(summary = "查询考试记录")
    @GetMapping("/records")
    public R<List<ExamRecord>> records(@RequestParam(required = false) Long studentId,
                                        @RequestParam(required = false) Long examId) {
        return R.ok(examRecordMapper.selectList(
                new LambdaQueryWrapper<ExamRecord>()
                        .eq(studentId != null, ExamRecord::getStudentId, studentId)
                        .eq(examId != null, ExamRecord::getExamId, examId)
                        .orderByDesc(ExamRecord::getSubmitTime)));
    }

    @Operation(summary = "查询学生已提交的考试ID列表")
    @GetMapping("/submitted")
    public R<List<Long>> submittedExamIds(@RequestParam Long studentId) {
        List<ExamRecord> records = examRecordMapper.selectList(
                new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getStudentId, studentId)
                        .select(ExamRecord::getExamId));
        List<Long> examIds = records.stream()
                .map(ExamRecord::getExamId)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        return R.ok(examIds);
    }

    @Operation(summary = "获取考试统计数据")
    @GetMapping("/stats")
    public R<java.util.Map<String, Object>> stats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalExams", examMapper.selectCount(null));
        stats.put("totalRecords", examRecordMapper.selectCount(null));

        // 平均分
        List<ExamRecord> records = examRecordMapper.selectList(null);
        BigDecimal avgScore = records.stream()
                .filter(r -> r.getScore() != null)
                .map(ExamRecord::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(Math.max(records.size(), 1)), 2, java.math.RoundingMode.HALF_UP);
        stats.put("avgScore", avgScore);

        // 通过率
        long passedCount = records.stream().filter(r -> r.getIsPassed() != null && r.getIsPassed() == 1).count();
        double passRate = records.isEmpty() ? 0 : (double) passedCount / records.size() * 100;
        stats.put("passRate", Math.round(passRate * 100.0) / 100.0);
        stats.put("passedCount", passedCount);

        return R.ok(stats);
    }
}
