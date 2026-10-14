package com.fitness.controller;

import com.fitness.common.Result;
import com.fitness.entity.UserAiConfig;
import com.fitness.service.UserAiConfigService;
import com.fitness.util.AiChatClient;
import com.fitness.util.ApiKeyCipher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户 AI 配置 Controller
 * <p>
 * 客户可以填入自己的 API Key；不填则自动使用系统内置的免费模型。
 */
@RestController
@RequestMapping("/ai-config")
public class UserAiConfigController {

    @Autowired
    private UserAiConfigService userAiConfigService;

    @Autowired
    private ApiKeyCipher cipher;

    @Autowired
    private AiChatClient aiChatClient;

    @Value("${ai.glm.api-key:}")
    private String systemApiKey;

    /**
     * 获取当前用户的 AI 配置（密钥以掩码返回）
     * GET /ai-config
     */
    @GetMapping
    public Result<Map<String, Object>> get(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        UserAiConfig config = userAiConfigService.getByUserId(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("systemAvailable", aiChatClient.isSystemConfigured());
        data.put("provider", "glm");

        if (config == null || config.getApiKey() == null || config.getApiKey().isEmpty()) {
            // 未配置个人密钥 —— 使用系统内置免费模型
            data.put("hasCustomKey", false);
            data.put("apiKeyMask", "");
            data.put("enabled", 0);
            data.put("usingSystem", true);
        } else {
            String plain = cipher.decrypt(config.getApiKey());
            data.put("hasCustomKey", true);
            data.put("apiKeyMask", cipher.mask(plain));
            data.put("enabled", config.getEnabled());
            data.put("apiUrl", config.getApiUrl());
            data.put("model", config.getModel());
            data.put("usingSystem", config.getEnabled() == null || config.getEnabled() == 0);
        }

        data.put("defaultModel", "glm-4-flash");
        data.put("defaultUrl", "https://open.bigmodel.cn/api/paas/v4/chat/completions");

        return Result.success(data);
    }

    /**
     * 保存当前用户的 AI 配置
     * POST /ai-config
     * <p>
     * 入参：{ apiKey, apiUrl, model, enabled }
     * apiKey 留空表示不修改已有密钥。
     */
    @PostMapping
    public Result<Boolean> save(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        Long userId = (Long) request.getAttribute("userId");

        String apiKey = params.get("apiKey") == null ? null : String.valueOf(params.get("apiKey"));
        String apiUrl = params.get("apiUrl") == null ? null : String.valueOf(params.get("apiUrl"));
        String model = params.get("model") == null ? null : String.valueOf(params.get("model"));
        Integer enabled = params.get("enabled") == null
                ? 1 : Integer.valueOf(String.valueOf(params.get("enabled")));

        userAiConfigService.saveConfig(userId, "glm", apiKey, apiUrl, model, enabled);
        return Result.success(true);
    }

    /**
     * 删除当前用户的 AI 配置（恢复使用系统内置免费模型）
     * DELETE /ai-config
     */
    @DeleteMapping
    public Result<Boolean> remove(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userAiConfigService.removeByUserId(userId);
        return Result.success(true);
    }

    /**
     * 测试连接：用指定密钥（或已保存的密钥）发一次真实请求
     * POST /ai-config/test
     */
    @PostMapping("/test")
    public Result<Map<String, Object>> test(HttpServletRequest request,
                                            @RequestBody(required = false) Map<String, Object> params) {
        Long userId = (Long) request.getAttribute("userId");

        Map<String, Object> result = new HashMap<>();

        // 若本次传了新的 apiKey，先临时保存再测试
        if (params != null && params.get("apiKey") != null
                && !String.valueOf(params.get("apiKey")).trim().isEmpty()) {
            userAiConfigService.saveConfig(userId, "glm",
                    String.valueOf(params.get("apiKey")),
                    params.get("apiUrl") == null ? null : String.valueOf(params.get("apiUrl")),
                    params.get("model") == null ? null : String.valueOf(params.get("model")),
                    1);
        }

        String reply = aiChatClient.chat(userId, "你是一个测试助手，请只回复两个字：连接成功。", "测试连接");

        if (reply != null && !reply.isEmpty()) {
            result.put("success", true);
            result.put("source", aiChatClient.getLastKeySource());
            result.put("model", aiChatClient.getProviderName());
            result.put("reply", reply.length() > 100 ? reply.substring(0, 100) + "..." : reply);
            return Result.success(result);
        }

        result.put("success", false);
        result.put("error", aiChatClient.getLastError());
        result.put("systemConfigured", aiChatClient.isSystemConfigured());
        return Result.success(result);
    }

    /**
     * 查询系统内置免费模型是否可用（供页面提示）
     * GET /ai-config/status
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        Map<String, Object> data = new HashMap<>();
        data.put("systemAvailable", aiChatClient.isSystemConfigured());
        data.put("model", aiChatClient.getProviderName());
        data.put("systemKeyMask", cipher.mask(systemApiKey));
        return Result.success(data);
    }
}