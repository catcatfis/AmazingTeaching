package com.cupk.amazingteaching.module.ai.controller;

import com.cupk.amazingteaching.common.result.R;
import com.cupk.amazingteaching.module.ai.service.AiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

/**
 * AI 智能辅助控制器
 * 提供智能问答、学习建议、自动出题等AI功能
 */
@Tag(name = "AI智能辅助")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
public class AIController {

    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private static final Random RANDOM = new Random();

    @Operation(summary = "AI智能问答（同步）")
    @PostMapping("/chat")
    public R<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        String question = request.getOrDefault("question", "");
        String answer = aiService.chat(question);
        Map<String, Object> response = new HashMap<>();
        response.put("question", question);
        response.put("answer", answer);
        response.put("timestamp", System.currentTimeMillis());
        return R.ok(response);
    }

    @Operation(summary = "AI智能问答（流式SSE）")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, String> request) {
        String question = request.getOrDefault("question", "");
        return aiService.chatStream(question);
    }

    @Operation(summary = "AI学习建议")
    @GetMapping("/study-advice")
    public R<Map<String, Object>> studyAdvice(@RequestParam Long studentId,
                                               @RequestParam(required = false) Long courseId) {
        Map<String, Object> advice = new HashMap<>();
        advice.put("studentId", studentId);
        advice.put("advice", generateStudyAdvice());
        advice.put("focusPoints", List.of(
                "建议每日学习2-3小时，保持持续学习节奏",
                "注重实践练习，完成每章节的编程作业",
                "定期复习知识点，制作思维导图",
                "参与课程讨论区，与同学交流学习心得"
        ));
        advice.put("nextSteps", List.of(
                "完成当前章节的课后练习",
                "预习下一章节的基础概念",
                "查看推荐的相关课程"
        ));
        return R.ok(advice);
    }

    @Operation(summary = "AI自动出题")
    @PostMapping("/generate-questions")
    public R<Map<String, Object>> generateQuestions(@RequestBody Map<String, Object> request) {
        String topic = (String) request.getOrDefault("topic", "Java基础");
        int count = (int) request.getOrDefault("count", 5);
        String difficulty = (String) request.getOrDefault("difficulty", "中等");

        String systemPrompt = "你是一个专业的出题老师。请根据指定主题、数量和难度，生成不重复的单选题。"
                + "必须返回纯 JSON 数组，不要包含 markdown 代码块标记或任何其他文字。"
                + "JSON 格式要求：[{\"question\": 题目文本, \"options\": A/B/C/D四个选项（每行一个，格式为 \"A. xxx\\nB. xxx\\nC. xxx\\nD. xxx\"）, \"answer\": 正确选项字母（如 \"B\"）}]"
                + "要求：题目互不相同，覆盖该主题的不同知识点，选项要有干扰性。";

        String userMsg = String.format("主题：%s，数量：%d，难度：%s", topic, count, difficulty);
        String aiReply = aiService.chatWithSystemPrompt(systemPrompt, userMsg);

        Map<String, Object> result = new HashMap<>();
        result.put("topic", topic);
        result.put("difficulty", difficulty);

        try {
            String json = aiReply;
            if (json.contains("[")) {
                json = json.substring(json.indexOf("["));
            }
            if (json.endsWith("]")) {
                json = json.substring(0, json.lastIndexOf("]") + 1);
            }
            List<Map<String, Object>> questions = objectMapper.readValue(json, List.class);
            // 为每道题补上序号
            for (int i = 0; i < questions.size(); i++) {
                questions.get(i).put("id", i + 1);
            }
            result.put("questions", questions);
            result.put("totalCount", questions.size());
        } catch (Exception e) {
            log.warn("AI 出题解析失败: {}", e.getMessage());
            // 兜底：返回空列表和错误提示
            result.put("questions", List.of(Map.of("id", 1, "question",
                    "AI 出题暂时不可用，请稍后再试。原始回复：" + (aiReply != null ? aiReply : "null"),
                    "options", "", "answer", "")));
            result.put("totalCount", 0);
        }
        return R.ok(result);
    }

    @Operation(summary = "AI知识图谱分析")
    @GetMapping("/knowledge-graph")
    public R<Map<String, Object>> knowledgeGraph(@RequestParam Long studentId) {
        Map<String, Object> graph = new HashMap<>();
        graph.put("studentId", studentId);

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        // 模拟知识图谱节点
        String[] topics = {"Java基础", "面向对象", "集合框架", "多线程", "Spring Boot",
                          "数据库", "前端基础", "数据结构", "算法设计", "设计模式"};
        for (int i = 0; i < topics.length; i++) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", i + 1);
            node.put("name", topics[i]);
            node.put("mastery", RANDOM.nextInt(40) + 60); // 掌握度 60-100
            node.put("category", i < 5 ? "后端" : i < 7 ? "数据库" : "计算机基础");
            nodes.add(node);
        }

        // 模拟知识关联
        int[][] relations = {{1,2},{2,3},{3,4},{1,5},{5,6},{1,7},{7,8},{8,9},{9,10}};
        for (int[] rel : relations) {
            Map<String, Object> edge = new HashMap<>();
            edge.put("source", rel[0]);
            edge.put("target", rel[1]);
            edge.put("relation", "前置知识");
            edges.add(edge);
        }

        graph.put("nodes", nodes);
        graph.put("edges", edges);
        return R.ok(graph);
    }

    @Operation(summary = "AI学习路径规划")
    @GetMapping("/learning-path")
    public R<Map<String, Object>> learningPath(@RequestParam Long studentId,
                                                @RequestParam String goal) {
        String systemPrompt = "你是一个专业的学习路径规划师。请根据用户的学习目标，生成一个详细的学习路径。"
                + "必须返回纯 JSON 数组，不要包含 markdown 代码块标记或任何其他文字。"
                + "JSON 格式要求：[{\"stage\": 阶段序号, \"name\": 阶段名称, \"duration\": 预计时长, \"content\": 具体学习内容描述}]"
                + "请规划 3-5 个阶段，每个阶段的 duration 用中文（如 '2周'、'1个月'），content 要详细具体。";

        String userMsg = "我的学习目标是：" + goal;
        String aiReply = aiService.chatWithSystemPrompt(systemPrompt, userMsg);

        Map<String, Object> result = new HashMap<>();
        result.put("studentId", studentId);
        result.put("goal", goal);

        try {
            // 提取 JSON 数组（兼容大模型返回带 markdown 代码块的情况）
            String json = aiReply;
            if (json.contains("[")) {
                json = json.substring(json.indexOf("["));
            }
            if (json.endsWith("]")) {
                json = json.substring(0, json.lastIndexOf("]") + 1);
            }
            result.put("stages", objectMapper.readValue(json, List.class));
        } catch (Exception e) {
            log.warn("AI 学习路径解析失败，使用默认路径: {}", e.getMessage());
            // 兜底：返回默认路径
            result.put("stages", List.of(
                    Map.of("stage", 1, "name", "基础入门", "duration", "2周", "content", aiReply)
            ));
        }
        return R.ok(result);
    }

    // ========== 私有辅助方法 ==========

    private String generateAIAnswer(String question) {
        if (question.contains("Java") || question.contains("java")) {
            return "Java是一种广泛使用的面向对象编程语言，具有跨平台特性。"
                   + "Java的核心特点包括：\n1. 面向对象：支持封装、继承、多态\n"
                   + "2. 跨平台：通过JVM实现一次编写、到处运行\n"
                   + "3. 安全性：提供沙箱安全机制\n"
                   + "4. 多线程：内置多线程支持\n建议从基础语法开始学习，逐步深入框架开发。";
        } else if (question.contains("Spring") || question.contains("spring")) {
            return "Spring Boot是Spring框架的扩展，用于简化Spring应用开发。"
                   + "核心特性包括：\n1. 自动配置：根据依赖自动配置项目\n"
                   + "2. 起步依赖：简化依赖管理\n"
                   + "3. 内嵌服务器：无需部署WAR包\n"
                   + "4. Actuator：提供生产级监控功能\n"
                   + "建议从Spring Boot官方文档开始学习。";
        } else if (question.contains("数据库") || question.contains("SQL") || question.contains("MySQL")) {
            return "MySQL是目前最流行的开源关系型数据库之一。"
                   + "学习重点包括：\n1. SQL基础：增删改查、聚合函数\n"
                   + "2. 索引优化：B+树索引、覆盖索引\n"
                   + "3. 事务管理：ACID特性、隔离级别\n"
                   + "4. 表设计：三大范式、反范式设计\n建议结合EXPLAIN分析查询性能。";
        }
        return "感谢你的提问！作为AI智能教学助手，我建议你从基础知识开始系统学习。"
               + "如果在学习过程中遇到困难，可以：\n"
               + "1. 查看课程文档和视频资料\n"
               + "2. 在讨论区与同学交流\n"
               + "3. 完成课后练习巩固知识\n"
               + "4. 向老师请教疑难问题\n继续加油！";
    }

    private String generateStudyAdvice() {
        String[] advices = {
            "根据你的学习进度，建议重点复习面向对象编程的核心概念，"
            + "特别是多态和接口的应用场景。",

            "你在集合框架部分的学习进度较快，可以尝试挑战更高难度的算法题目来巩固知识。",

            "多线程编程是你的薄弱环节，建议通过实际项目练习来加深理解，"
            + "可以从简单的生产者-消费者模型开始。",

            "你的考试成绩稳步提升！继续保持当前学习节奏，"
            + "注意定期回顾之前学过的知识点，做好知识体系的串联。"
        };
        return advices[RANDOM.nextInt(advices.length)];
    }

    private List<Map<String, Object>> generateQuestions(String topic, int count, String difficulty) {
        List<Map<String, Object>> questions = new ArrayList<>();

        String[][] questionBank = {
            {"Java中 == 和 equals() 的区别是什么？", "A. ==比较引用，equals比较值\nB. ==比较值，equals比较引用\nC. 两者完全相同\nD. 两者完全不同", "A"},
            {"下列哪个不是Java的基本数据类型？", "A. int\nB. String\nC. boolean\nD. double", "B"},
            {"Spring Boot的自动配置原理基于什么？", "A. @Autowired\nB. @Conditional\nC. @ComponentScan\nD. @ConfigurationProperties", "B"},
            {"MySQL中，哪种索引类型支持最左前缀原则？", "A. Hash索引\nB. B+Tree索引\nC. Full-text索引\nD. R-tree索引", "B"},
            {"关于多线程，以下说法正确的是？", "A. synchronized是悲观锁\nB. volatile保证原子性\nC. Lock不支持中断\nD. ThreadLocal是全局变量", "A"},
            {"以下哪个是Java 8的新特性？", "A. 泛型\nB. Lambda表达式\nC. 注解\nD. 枚举", "B"},
            {"HTTP协议中，200状态码表示什么？", "A. 重定向\nB. 请求成功\nC. 服务器错误\nD. 未授权", "B"},
            {"在Vue3中，ref和reactive的区别是？", "A. ref用于基本类型，reactive用于对象\nB. 两者完全相同\nC. ref不能用于对象\nD. reactive不能用于数组", "A"}
        };

        for (int i = 0; i < Math.min(count, questionBank.length); i++) {
            String[] q = questionBank[RANDOM.nextInt(questionBank.length)];
            Map<String, Object> question = new LinkedHashMap<>();
            question.put("id", i + 1);
            question.put("topic", topic);
            question.put("difficulty", difficulty);
            question.put("question", q[0]);
            question.put("options", q[1]);
            question.put("answer", q[2]);
            question.put("score", 10);
            questions.add(question);
        }
        return questions;
    }
}
