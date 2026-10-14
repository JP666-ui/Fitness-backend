package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户 AI 配置
 * <p>
 * 用户可以填入自己的 API Key；未配置时系统自动使用内置的免费模型。
 */
@Data
@TableName("user_ai_config")
public class UserAiConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 服务商：glm */
    private String provider;

    /** API 密钥（加密存储，不返回给前端） */
    @JsonIgnore
    private String apiKey;

    /** 自定义接口地址，留空使用默认 */
    private String apiUrl;

    /** 模型名称，留空使用默认 */
    private String model;

    /** 是否启用 */
    private Integer enabled;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}