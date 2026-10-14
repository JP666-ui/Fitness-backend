package com.fitness.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fitness.entity.User;
import com.fitness.mapper.UserMapper;
import com.fitness.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 用户 Service 实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    
    @Override
    public User register(String username, String password, String nickname) {
        // 检查用户名是否已存在
        if (getByUsername(username) != null) {
            throw new RuntimeException("用户名已存在");
        }
        
        User user = new User();
        user.setUsername(username);
        // MD5加密密码
        user.setPassword(DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8)));
        user.setNickname(nickname != null ? nickname : username);
        user.setStatus(1);
        user.setDailyCalorieGoal(2000); // 默认每日目标2000卡
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        
        save(user);
        return user;
    }
    
    @Override
    public User login(String username, String password) {
        User user = getByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getStatus() == 0) {
            throw new RuntimeException("用户已被禁用");
        }
        
        String encryptedPassword = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        
        return user;
    }
    
    @Override
    public User getByUsername(String username) {
        return lambdaQuery().eq(User::getUsername, username).one();
    }
    
    @Override
    public boolean updateWeight(Long userId, Double weight) {
        User user = getById(userId);
        if (user == null) {
            return false;
        }
        user.setWeight(weight);
        user.setUpdateTime(LocalDateTime.now());
        // 重新计算每日目标热量
        user.setDailyCalorieGoal(calculateDailyCalorie(userId));
        return updateById(user);
    }
    
    @Override
    public Double calculateBMI(Long userId) {
        User user = getById(userId);
        if (user == null || user.getHeight() == null || user.getWeight() == null) {
            return null;
        }
        // BMI = 体重(kg) / 身高(m)²
        double heightInMeter = user.getHeight() / 100.0;
        return user.getWeight() / (heightInMeter * heightInMeter);
    }
    
    @Override
    public Integer calculateDailyCalorie(Long userId) {
        User user = getById(userId);
        if (user == null || user.getWeight() == null || user.getHeight() == null || user.getAge() == null) {
            return 2000; // 默认值
        }
        
        // 使用Mifflin-St Jeor公式计算基础代谢率(BMR)
        // 男性: BMR = 10 × 体重(kg) + 6.25 × 身高(cm) - 5 × 年龄(y) + 5
        // 女性: BMR = 10 × 体重(kg) + 6.25 × 身高(cm) - 5 × 年龄(y) - 161
        double bmr;
        if (user.getGender() != null && user.getGender() == 1) {
            bmr = 10 * user.getWeight() + 6.25 * user.getHeight() - 5 * user.getAge() + 5;
        } else {
            bmr = 10 * user.getWeight() + 6.25 * user.getHeight() - 5 * user.getAge() - 161;
        }
        
        // 假设轻度活动水平，乘以1.375
        return (int) (bmr * 1.375);
    }
}
