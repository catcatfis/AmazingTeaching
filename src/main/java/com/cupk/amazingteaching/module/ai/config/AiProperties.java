package com.cupk.amazingteaching.module.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 大模型配置属性
 * 支持所有 OpenAI 兼容格式的 API
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /**
     * API 地址（OpenAI 兼容格式）
     * 通义千问: https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
     * DeepSeek: https://api.deepseek.com/v1/chat/completions
     * Moonshot: https://api.moonshot.cn/v1/chat/completions
     * OpenAI:    https://api.openai.com/v1/chat/completions
     */
    private String apiUrl;

    /**
     * API 密钥
     */
    private String apiKey;

    /**
     * 模型名称
     */
    private String model = "qwen-plus";

    /**
     * 最大生成 token 数
     */
    private int maxTokens = 2048;

    /**
     * 温度参数 (0.0 ~ 2.0)，越高越有创意
     */
    private double temperature = 0.7;

    /**
     * 系统提示词
     */
    private String systemPrompt = "你是一个智能教学助手，帮助学生学习编程和技术知识。";
}
