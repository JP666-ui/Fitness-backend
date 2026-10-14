package com.fitness.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fitness.entity.UserAiConfig;
import com.fitness.mapper.UserAiConfigMapper;
import com.fitness.service.UserAiConfigService;
import com.fitness.util.ApiKeyCipher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户 AI 配置 Service 实现
 */
@Service
public class UserAiConfigServiceImpl extends ServiceImpl<UserAiConfigMapper, UserAiConfig>
        implements UserAiConfigService {

    @Autowired
    private ApiKeyCipher cipher;

    @Override
    public UserAiConfig getByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return lambdaQuery()
                .eq(UserAiConfig::getUserId, userId)
                .one();
    }

    @Override
    public void saveConfig(Long userId, String provider, String apiKey,
                           String apiUrl, String model, Integer enabled) {
        UserAiConfig existing = getByUserId(userId);

        UserAiConfig config = existing == null ? new UserAiConfig() : existing;
        config.setUserId(userId);

        if (provider != null && !provider.trim().isEmpty()) {
            config.setProvider(provider.trim());
        } else if (config.getProvider() == null) {
            config.setProvider("glm");
        }

        // 密钥为空表示「不修改」，避免前端回显掩码时误覆盖
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            config.setApiKey(cipher.encrypt(apiKey.trim()));
        }

        config.setApiUrl(apiUrl == null ? null : apiUrl.trim());
        config.setModel(model == null ? null : model.trim());
        config.setEnabled(enabled == null ? 1 : enabled);
        config.setUpdateTime(LocalDateTime.now());

        if (existing == null) {
            config.setCreateTime(LocalDateTime.now());
            save(config);
        } else {
            updateById(config);
        }
    }

    @Override
    public void removeByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        lambdaUpdate()
                .eq(UserAiConfig::getUserId, userId)
                .remove();
    }
}