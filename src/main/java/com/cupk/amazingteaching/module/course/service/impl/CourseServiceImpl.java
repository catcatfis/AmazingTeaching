package com.cupk.amazingteaching.module.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cupk.amazingteaching.common.exception.BusinessException;
import com.cupk.amazingteaching.module.course.entity.Course;
import com.cupk.amazingteaching.module.course.entity.StudentCourse;
import com.cupk.amazingteaching.module.course.mapper.CourseMapper;
import com.cupk.amazingteaching.module.course.mapper.StudentCourseMapper;
import com.cupk.amazingteaching.module.course.service.CourseService;
import com.cupk.amazingteaching.module.user.entity.SysUser;
import com.cupk.amazingteaching.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 课程服务实现（含推荐算法）
 */
@Service
@RequiredArgsConstructor
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    private final CourseMapper courseMapper;
    private final StudentCourseMapper studentCourseMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public Page<Course> pageCourses(int page, int size, String keyword, String category, Integer difficulty) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>()
                .eq(Course::getDeleted, 0)
                .eq(category != null && !category.isEmpty(), Course::getCategory, category)
                .eq(difficulty != null, Course::getDifficulty, difficulty)
                .and(keyword != null && !keyword.isEmpty(), w -> w
                        .like(Course::getCourseName, keyword)
                        .or()
                        .like(Course::getDescription, keyword))
                .orderByDesc(Course::getStudentCount);

        Page<Course> result = page(new Page<>(page, size), wrapper);
        // 填充教师姓名
        for (Course course : result.getRecords()) {
            if (course.getTeacherId() != null) {
                SysUser teacher = sysUserMapper.selectById(course.getTeacherId());
                course.setTeacherName(teacher != null ? teacher.getRealName() : "未知教师");
            }
        }
        return result;
    }

    @Override
    public Course getCourseDetail(Long id) {
        Course course = getById(id);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        if (course.getTeacherId() != null) {
            SysUser teacher = sysUserMapper.selectById(course.getTeacherId());
            course.setTeacherName(teacher != null ? teacher.getRealName() : "未知教师");
        }
        return course;
    }

    @Override
    public Course addCourse(Course course) {
        course.setStatus(0);
        course.setStudentCount(0);
        save(course);
        return course;
    }

    /**
     * 推荐算法：基于协同过滤
     * 找到与目标学生选修了相同课程的其他学生，
     * 推荐那些其他学生选修了但目标学生没选的课程
     */
    @Override
    public List<Course> recommendCourses(Long studentId, int limit) {
        // 1. 获取目标学生已选课程
        List<StudentCourse> myCourses = studentCourseMapper.selectList(
                new LambdaQueryWrapper<StudentCourse>()
                        .eq(StudentCourse::getStudentId, studentId));
        Set<Long> myCourseIds = myCourses.stream()
                .map(StudentCourse::getCourseId).collect(Collectors.toSet());

        if (myCourseIds.isEmpty()) {
            // 新用户：推荐热门课程
            return getHotCourses(limit);
        }

        // 2. 找到学过同一门课程的其他学生
        List<Long> similarStudentIds = studentCourseMapper.selectList(
                new LambdaQueryWrapper<StudentCourse>()
                        .in(StudentCourse::getCourseId, myCourseIds)
                        .ne(StudentCourse::getStudentId, studentId))
                .stream().map(StudentCourse::getStudentId)
                .distinct().collect(Collectors.toList());

        if (similarStudentIds.isEmpty()) {
            return getHotCourses(limit);
        }

        // 3. 获取相似学生选修的课程
        List<StudentCourse> similarCourses = studentCourseMapper.selectList(
                new LambdaQueryWrapper<StudentCourse>()
                        .in(StudentCourse::getStudentId, similarStudentIds));

        // 4. 统计课程出现次数（简单协同过滤）
        Map<Long, Long> courseScoreMap = similarCourses.stream()
                .filter(sc -> !myCourseIds.contains(sc.getCourseId()))
                .collect(Collectors.groupingBy(StudentCourse::getCourseId, Collectors.counting()));

        // 5. 按得分排序，返回推荐课程
        List<Long> recommendedIds = courseScoreMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (recommendedIds.isEmpty()) {
            return getHotCourses(limit);
        }

        List<Course> recommended = courseMapper.selectBatchIds(recommendedIds);
        // 按推荐分数排序
        Map<Long, Course> courseMap = recommended.stream()
                .collect(Collectors.toMap(Course::getId, c -> c));
        return recommendedIds.stream()
                .map(courseMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<Course> getHotCourses(int limit) {
        return list(new LambdaQueryWrapper<Course>()
                .eq(Course::getDeleted, 0)
                .eq(Course::getStatus, 1)
                .orderByDesc(Course::getStudentCount)
                .orderByDesc(Course::getRating)
                .last("LIMIT " + limit));
    }

    @Override
    public List<Map<String, Object>> getCategoryStats() {
        List<Course> courses = list(new LambdaQueryWrapper<Course>().eq(Course::getDeleted, 0));
        Map<String, Long> categoryCount = courses.stream()
                .collect(Collectors.groupingBy(Course::getCategory, Collectors.counting()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : categoryCount.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", entry.getKey());
            item.put("value", entry.getValue());
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getDifficultyStats() {
        List<Course> courses = list(new LambdaQueryWrapper<Course>().eq(Course::getDeleted, 0));
        String[] labels = {"入门", "初级", "中级", "高级"};
        Map<Integer, Long> diffCount = courses.stream()
                .collect(Collectors.groupingBy(Course::getDifficulty, Collectors.counting()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", labels[i - 1]);
            item.put("value", diffCount.getOrDefault(i, 0L));
            result.add(item);
        }
        return result;
    }
}
