package com.fitness.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fitness.entity.UserAiConfig;

/**
 * 用户 AI 配置 Service
 */
public interface UserAiConfigService extends IService<UserAiConfig> {

    /**
     * 获取用户的 AI 配置
     *
     * @return 配置；未配置返回 null
     */
    UserAiConfig getByUserId(Long userId);

    /**
     * 保存或更新用户的 AI 配置
     *
     * @param userId   用户ID
     * @param provider 服务商
     * @param apiKey   明文密钥（为空表示不修改）
     * @param apiUrl   自定义接口地址
     * @param model    模型名称
     * @param enabled  是否启用
     */
    void saveConfig(Long userId, String provider, String apiKey,
                    String apiUrl, String model, Integer enabled);

    /**
     * 删除用户的 AI 配置（恢复使用系统内置免费模型）
     */
    void removeByUserId(Long userId);
}