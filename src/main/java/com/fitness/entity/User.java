package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类（个人健康档案）
 * <p>
 * 纯个人系统：不含邮箱、手机号、角色等社交/权限字段，
 * 只保留账号与健康相关数据。
 */
@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户名（登录用）
     */
    private String username;
    
    /**
     * 密码（MD5 加密存储）
     */
    private String password;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 头像URL
     */
    private String avatar;
    
    /**
     * 性别：0-未知 1-男 2-女
     */
    private Integer gender;
    
    /**
     * 年龄
     */
    private Integer age;
    
    /**
     * 身高(cm)
     */
    private Double height;
    
    /**
     * 体重(kg)
     */
    private Double weight;
    
    /**
     * 目标体重(kg)
     */
    private Double targetWeight;
    
    /**
     * 每日目标热量(kcal)
     */
    private Integer dailyCalorieGoal;
    
    /**
     * 状态：0-禁用 1-正常
     * <p>
     * 纯个人系统仍保留该字段，用于账号自锁/停用，不做管理员管控。
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
