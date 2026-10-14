package com.fitness.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.fitness.entity.UserAiConfig;
import com.fitness.service.UserAiConfigService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * AI 对话客户端
 * <p>
 * <b>密钥解析顺序</b>：
 * <ol>
 *     <li>用户自己配置的 API Key（在「AI 设置」中填写）</li>
 *     <li>系统内置的免费模型 Key（application.yml 中的 ai.glm.api-key）</li>
 * </ol>
 * 也就是说：<b>客户不填密钥也能直接使用系统提供的免费模型</b>；
 * 若填写了自己的密钥，则优先走客户自己的额度。
 * <p>
 * 接口遵循 OpenAI 兼容规范（智谱 GLM-4-Flash 完全免费）。
 * 申请免费 Key：https://open.bigmodel.cn
 */
@Slf4j
@Component
public class AiChatClient {

    /** 系统内置（免费）配置 */
    @Value("${ai.glm.api-key:}")
    private String systemApiKey;

    @Value("${ai.glm.url:https://open.bigmodel.cn/api/paas/v4/chat/completions}")
    private String systemApiUrl;

    @Value("${ai.glm.model:glm-4-flash}")
    private String systemModel;

    @Autowired(required = false)
    private UserAiConfigService userAiConfigService;

    @Autowired
    private ApiKeyCipher cipher;

    private final RestTemplate restTemplate;

    /** 最近一次调用失败的原因 */
    private volatile String lastError;

    /** 最近一次调用实际使用的密钥来源：user / system */
    private volatile String lastKeySource;

    public AiChatClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void init() {
        log.info("AI 对话模型: 智谱 {} | 系统内置 Key 已配置: {}", systemModel, isSystemConfigured());
    }

    /**
     * 系统内置免费模型是否可用
     */
    public boolean isSystemConfigured() {
        return systemApiKey != null && !systemApiKey.trim().isEmpty();
    }

    /**
     * 是否可用（系统内置可用即可，客户无需填写密钥）
     */
    public boolean isConfigured() {
        return isSystemConfigured();
    }

    /**
     * 当前模型名称
     */
    public String getProviderName() {
        return "智谱 " + systemModel;
    }

    /**
     * 最近一次调用失败原因
     */
    public String getLastError() {
        return lastError;
    }

    /**
     * 最近一次调用使用的密钥来源：user=用户自填，system=系统内置
     */
    public String getLastKeySource() {
        return lastKeySource;
    }

    /**
     * 单轮对话
     */
    public String chat(Long userId, String systemPrompt, String userMessage) {
        JSONArray messages = new JSONArray();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(JSONObject.of("role", "system", "content", systemPrompt));
        }
        messages.add(JSONObject.of("role", "user", "content", userMessage));
        return doChat(userId, messages, 0.7);
    }

    /**
     * 带历史上下文的多轮对话
     */
    public String chatWithHistory(Long userId, String systemPrompt,
                                  List<Map<String, String>> history, String userMessage) {
        JSONArray messages = new JSONArray();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(JSONObject.of("role", "system", "content", systemPrompt));
        }

        if (history != null) {
            // 只保留最近 10 条，避免 token 过多
            int start = Math.max(0, history.size() - 10);
            for (int i = start; i < history.size(); i++) {
                Map<String, String> item = history.get(i);
                if (item == null) {
                    continue;
                }
                String role = item.get("role");
                String content = item.get("content");
                if (role == null || content == null || content.isEmpty()) {
                    continue;
                }
                if (!"assistant".equals(role)) {
                    role = "user";
                }
                messages.add(JSONObject.of("role", role, "content", content));
            }
        }

        messages.add(JSONObject.of("role", "user", "content", userMessage));
        return doChat(userId, messages, 0.7);
    }

    /**
     * 解析本次调用要用的凭据：优先用户自填，其次系统内置
     */
    private ResolvedConfig resolve(Long userId) {
        // 1) 用户自己配置的密钥
        if (userId != null && userAiConfigService != null) {
            try {
                UserAiConfig config = userAiConfigService.getByUserId(userId);
                if (config != null && config.getEnabled() != null && config.getEnabled() == 1
                        && config.getApiKey() != null && !config.getApiKey().isEmpty()) {
                    String plainKey = cipher.decrypt(config.getApiKey());
                    if (plainKey != null && !plainKey.trim().isEmpty()) {
                        return new ResolvedConfig(
                                plainKey.trim(),
                                notBlank(config.getApiUrl()) ? config.getApiUrl().trim() : systemApiUrl,
                                notBlank(config.getModel()) ? config.getModel().trim() : systemModel,
                                "user"
                        );
                    }
                }
            } catch (Exception e) {
                log.warn("读取用户 AI 配置失败，回退到系统内置模型: {}", e.getMessage());
            }
        }

        // 2) 系统内置的免费模型
        if (isSystemConfigured()) {
            return new ResolvedConfig(systemApiKey.trim(), systemApiUrl, systemModel, "system");
        }

        return null;
    }

    /**
     * 执行请求
     */
    private String doChat(Long userId, JSONArray messages, double temperature) {
        ResolvedConfig config = resolve(userId);

        if (config == null) {
            lastKeySource = null;
            lastError = "系统未配置免费模型密钥，且你尚未填写自己的 API Key";
            log.debug("无可用 AI 凭据，跳过远程调用");
            return null;
        }

        lastKeySource = config.source;

        try {
            JSONObject body = new JSONObject();
            body.put("model", config.model);
            body.put("messages", messages);
            body.put("temperature", temperature);
            body.put("max_tokens", 1200);
            body.put("stream", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.apiKey);

            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
            String respStr = restTemplate.postForObject(config.apiUrl, entity, String.class);

            if (respStr == null || respStr.isEmpty()) {
                lastError = "AI 服务无响应";
                return null;
            }

            JSONObject resp = JSON.parseObject(respStr);

            if (resp.containsKey("error")) {
                Object errObj = resp.get("error");
                String msg = errObj instanceof JSONObject e
                        ? e.getString("message")
                        : String.valueOf(errObj);
                lastError = explainError(msg);
                log.warn("GLM 返回错误: {}", msg);
                return null;
            }

            JSONArray choices = resp.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                lastError = "AI 未返回内容";
                log.warn("GLM 返回结构异常: {}", respStr);
                return null;
            }

            String content = choices.getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            if (content == null || content.isEmpty()) {
                lastError = "AI 返回内容为空";
                return null;
            }

            lastError = null;
            return content;
        } catch (Exception e) {
            lastError = explainError(e.getMessage());
            log.warn("调用 GLM 失败({}): {}", config.source, e.getMessage());
            return null;
        }
    }

    /**
     * 把底层异常翻译成可读提示
     */
    private String explainError(String raw) {
        if (raw == null) {
            return "AI 调用失败";
        }
        if (raw.contains("401")) {
            return "API Key 无效，请检查密钥是否正确";
        }
        if (raw.contains("402")) {
            return "账户余额不足或额度已用完";
        }
        if (raw.contains("403")) {
            return "拒绝访问，请确认已开通该模型";
        }
        if (raw.contains("429")) {
            return "请求过于频繁，请稍后重试";
        }
        if (raw.contains("404")) {
            return "接口地址或模型名不正确：" + systemModel;
        }
        if (raw.contains("timeout") || raw.contains("timed out")) {
            return "连接超时，请检查网络";
        }
        if (raw.contains("Connection refused") || raw.contains("connect")) {
            return "无法连接 AI 服务，请检查网络";
        }
        return "AI 调用失败：" + raw;
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /**
     * 解析后的调用凭据
     */
    private static final class ResolvedConfig {
        final String apiKey;
        final String apiUrl;
        final String model;
        /** user=用户自填 system=系统内置 */
        final String source;

        ResolvedConfig(String apiKey, String apiUrl, String model, String source) {
            this.apiKey = apiKey;
            this.apiUrl = apiUrl;
            this.model = model;
            this.source = source;
        }
    }
}