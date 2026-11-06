package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 健康评测记录
 */
@Data
@TableName("health_assessment")
public class HealthAssessment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 评测日期（每人每天一条） */
    private LocalDate assessDate;

    private Double bmi;

    /** 体脂率 % */
    private Double bodyFatRate;

    /** 肌肉量 kg */
    private Double muscleMass;

    /** 体水分率 % */
    private Double waterRate;

    /** 脂肪量 kg */
    private Double fatMass;

    /** 去脂体重 kg */
    private Double leanMass;

    /** 基础代谢 kcal */
    private Integer bmr;

    /** 代谢年龄 */
    private Integer metabolicAge;

    /** 内脏脂肪等级 */
    private Integer visceralFat;

    /** 综合健康评分 0-100 */
    private Integer healthScore;

    /** 健康等级：优秀/良好/一般/偏弱/较差 */
    private String healthLevel;

    /** 营养均衡评分 0-100 */
    private Integer nutritionScore;

    /** 建议（JSON 数组字符串） */
    private String advice;

    private LocalDateTime createTime;
}
