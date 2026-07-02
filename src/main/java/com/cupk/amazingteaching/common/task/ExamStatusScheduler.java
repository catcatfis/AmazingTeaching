package com.cupk.amazingteaching.common.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cupk.amazingteaching.module.exam.entity.Exam;
import com.cupk.amazingteaching.module.exam.mapper.ExamMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试状态定时更新任务
 * 根据考试的开始时间和结束时间自动更新考试状态
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamStatusScheduler {

    private final ExamMapper examMapper;

    /**
     * 每分钟执行一次，检查并更新考试状态
     * 状态规则：
     * - 当前时间 < start_time -> 状态为0（未发布）
     * - start_time <= 当前时间 <= end_time -> 状态为1（进行中）
     * - 当前时间 > end_time -> 状态为2（已结束）
     */
    @Scheduled(cron = "0 * * * * ?")
    public void updateExamStatus() {
        log.debug("开始执行考试状态更新任务...");
        
        LocalDateTime now = LocalDateTime.now();
        
        // 查询所有需要更新状态的考试（状态不是进行中的考试）
        // 为了避免频繁更新，只查询状态可能需要变化的考试
        LambdaQueryWrapper<Exam> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Exam::getStartTime)
               .isNotNull(Exam::getEndTime);
        
        List<Exam> exams = examMapper.selectList(wrapper);
        
        int updatedCount = 0;
        
        for (Exam exam : exams) {
            Integer newStatus = calculateNewStatus(exam, now);
            
            // 如果状态发生变化，则更新
            if (newStatus != null && !newStatus.equals(exam.getStatus())) {
                exam.setStatus(newStatus);
                examMapper.updateById(exam);
                updatedCount++;
                log.info("更新考试状态：考试ID={}, 考试名称={}, 原状态={}, 新状态={}", 
                        exam.getId(), exam.getExamName(), exam.getStatus(), newStatus);
            }
        }
        
        if (updatedCount > 0) {
            log.info("考试状态更新任务完成，共更新{}条考试记录", updatedCount);
        }
    }
    
    /**
     * 根据考试时间和当前时间计算新状态
     * 
     * @param exam 考试信息
     * @param now 当前时间
     * @return 新状态，如果不需要更新返回null
     */
    private Integer calculateNewStatus(Exam exam, LocalDateTime now) {
        LocalDateTime startTime = exam.getStartTime();
        LocalDateTime endTime = exam.getEndTime();
        
        if (startTime == null || endTime == null) {
            return null;
        }
        
        Integer currentStatus = exam.getStatus();
        
        // 如果当前时间在开始时间之前，状态应该是未发布（0）
        if (now.isBefore(startTime)) {
            if (currentStatus != 0) {
                return 0;
            }
        }
        // 如果当前时间在开始时间和结束时间之间，状态应该是进行中（1）
        else if (!now.isAfter(endTime)) {
            if (currentStatus != 1) {
                return 1;
            }
        }
        // 如果当前时间在结束时间之后，状态应该是已结束（2）
        else {
            if (currentStatus != 2) {
                return 2;
            }
        }
        
        return null;
    }
}