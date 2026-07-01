package com.cupk.amazingteaching.module.ai.service;

import com.cupk.amazingteaching.module.ai.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * AI 大模型服务
 * 调用 OpenAI 兼容格式的 API，支持流式（SSE）和同步两种模式
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    /**
     * 流式调用大模型，通过 SseEmitter 实时推送 token 到前端
     */
    public SseEmitter chatStream(String userMessage) {
        SseEmitter emitter = new SseEmitter(300_000L);

        CompletableFuture.runAsync(() -> {
            try {
                String requestBody = buildRequestBody(userMessage, true);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(aiProperties.getApiUrl()))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + aiProperties.getApiKey())
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                        .timeout(Duration.ofMinutes(5))
                        .build();

                HttpResponse<java.io.InputStream> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() != 200) {
                    String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                    log.error("AI API 调用失败，状态码: {}, 响应: {}", response.statusCode(), errorBody);
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("AI 服务调用失败，状态码: " + response.statusCode()));
                    emitter.complete();
                    return;
                }

                // 逐行读取 SSE 流
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isEmpty()) continue;
                        if ("data: [DONE]".equals(line.trim())) {
                            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                            break;
                        }
                        if (line.startsWith("data: ")) {
                            String jsonStr = line.substring(6).trim();
                            try {
                                JsonNode jsonNode = objectMapper.readTree(jsonStr);
                                JsonNode choices = jsonNode.get("choices");
                                if (choices != null && choices.size() > 0) {
                                    JsonNode delta = choices.get(0).get("delta");
                                    if (delta != null && delta.has("content")) {
                                        JsonNode contentNode = delta.get("content");
                                        // 跳过 null content（流式开头的 role 事件）
                                        if (contentNode.isNull() || contentNode.isMissingNode()) {
                                            continue;
                                        }
                                        String content = contentNode.asText("");
                                        if (!content.isEmpty()) {
                                            emitter.send(SseEmitter.event()
                                                    .name("message")
                                                    .data(content));
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.debug("解析 SSE 数据跳过: {}", jsonStr);
                            }
                        }
                    }
                }
                emitter.complete();

            } catch (Exception e) {
                log.error("AI 流式调用异常", e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("AI 服务异常: " + e.getMessage()));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        emitter.onTimeout(() -> {
            log.warn("AI SSE 连接超时");
            emitter.complete();
        });
        emitter.onError(e -> log.debug("AI SSE 连接错误: {}", e.getMessage()));

        return emitter;
    }

    /**
     * 同步调用大模型，返回完整回复
     */
    public String chat(String userMessage) {
        try {
            String requestBody = buildRequestBody(userMessage, false);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiProperties.getApiUrl()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + aiProperties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .timeout(Duration.ofMinutes(3))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                log.error("AI API 调用失败，状态码: {}, 响应: {}", response.statusCode(), response.body());
                return "AI 服务暂时不可用，请稍后再试。";
            }

            JsonNode jsonNode = objectMapper.readTree(response.body());
            JsonNode choices = jsonNode.get("choices");
            if (choices != null && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null && message.has("content")) {
                    return message.get("content").asText();
                }
            }
            return "AI 未能生成有效回复，请重新提问。";

        } catch (Exception e) {
            log.error("AI 同步调用异常", e);
            return "AI 服务异常: " + e.getMessage();
        }
    }

    /**
     * 使用自定义 System Prompt 调用大模型
     */
    public String chatWithSystemPrompt(String systemPrompt, String userMessage) {
        try {
            String requestBody = buildRequestBodyWithSystem(systemPrompt, userMessage, false);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiProperties.getApiUrl()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + aiProperties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .timeout(Duration.ofMinutes(3))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                log.error("AI API 调用失败，状态码: {}, 响应: {}", response.statusCode(), response.body());
                return null;
            }

            JsonNode jsonNode = objectMapper.readTree(response.body());
            JsonNode choices = jsonNode.get("choices");
            if (choices != null && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null && message.has("content")) {
                    return message.get("content").asText();
                }
            }
            return null;
        } catch (Exception e) {
            log.error("AI 自定义Prompt调用异常", e);
            return null;
        }
    }

    /**
     * 构建带自定义 System Prompt 的请求体
     */
    private String buildRequestBodyWithSystem(String systemPrompt, String userMessage, boolean stream) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", aiProperties.getModel());
            root.put("max_tokens", aiProperties.getMaxTokens());
            root.put("temperature", aiProperties.getTemperature());
            root.put("stream", stream);

            ArrayNode messages = objectMapper.createArrayNode();

            ObjectNode sysMsg = objectMapper.createObjectNode();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.add(sysMsg);

            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            root.set("messages", messages);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("构建请求体失败", e);
        }
    }

    /**
     * 构建 OpenAI 兼容格式的请求体
     */
    private String buildRequestBody(String userMessage, boolean stream) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", aiProperties.getModel());
            root.put("max_tokens", aiProperties.getMaxTokens());
            root.put("temperature", aiProperties.getTemperature());
            root.put("stream", stream);

            ArrayNode messages = objectMapper.createArrayNode();

            ObjectNode systemMsg = objectMapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content", aiProperties.getSystemPrompt());
            messages.add(systemMsg);

            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            root.set("messages", messages);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("构建请求体失败", e);
        }
    }
}
