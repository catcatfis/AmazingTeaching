package com.cupk.amazingteaching.module.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.amazingteaching.common.annotation.LogOperation;
import com.cupk.amazingteaching.common.result.R;
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

    @Operation(summary = "分页查询考试")
    @GetMapping("/page")
    public R<Page<Exam>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long courseId) {
        LambdaQueryWrapper<Exam> wrapper = new LambdaQueryWrapper<Exam>()
                .eq(courseId != null, Exam::getCourseId, courseId)
                .orderByDesc(Exam::getCreateTime);
        return R.ok(new Page<Exam>(page, size).setRecords(
                examMapper.selectPage(new Page<>(page, size), wrapper).getRecords()));
    }

    @Operation(summary = "考试详情")
    @GetMapping("/{id}")
    public R<Exam> detail(@PathVariable Long id) {
        return R.ok(examMapper.selectById(id));
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
        return R.ok("更新成功", exam);
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
        // 自动评分（简单评分逻辑：根据JSON答案计算）
        Exam exam = examMapper.selectById(record.getExamId());
        if (exam != null) {
            // 模拟自动评分（实际项目中需要解析JSON并对答案）
            record.setScore(new BigDecimal("85.0"));
            record.setIsPassed(record.getScore().compareTo(new BigDecimal(exam.getPassScore())) >= 0 ? 1 : 0);
        }
        examRecordMapper.insert(record);
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
