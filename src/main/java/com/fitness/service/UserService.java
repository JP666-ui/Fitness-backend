package com.fitness.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fitness.entity.User;

/**
 * 用户 Service 接口
 */
public interface UserService extends IService<User> {
    
    /**
     * 用户注册
     */
    User register(String username, String password, String nickname);
    
    /**
     * 用户登录
     */
    User login(String username, String password);
    
    /**
     * 根据用户名查找用户
     */
    User getByUsername(String username);
    
    /**
     * 更新用户体重
     */
    boolean updateWeight(Long userId, Double weight);
    
    /**
     * 计算用户BMI
     */
    Double calculateBMI(Long userId);
    
    /**
     * 计算每日推荐热量
     */
    Integer calculateDailyCalorie(Long userId);
}
