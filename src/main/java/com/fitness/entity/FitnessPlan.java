package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 健身计划实体类
 * <p>
 * 对应数据库表 fitness_plan
 */
@Data
@TableName("fitness_plan")
public class FitnessPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 计划名称 */
    private String planName;

    /** 计划类型：loss-减脂 gain-增肌 shape-塑形 maintain-维持 */
    private String planType;

    /** 目标体重(kg) */
    private Double targetWeight;

    /** 需减体重(kg) = 当前体重 − 目标体重 */
    @TableField("weight_to_lose")
    private Double weightToLose;

    /** 每日目标热量(kcal)，对应列 target_calorie */
    @TableField("target_calorie")
    private Integer targetCalorie;

    /** 每日热量缺口(kcal)，对应列 daily_deficit */
    @TableField("daily_deficit")
    private Integer dailyDeficit;

    /** 计划开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /** 计划结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /** 状态：0-已取消 1-进行中 2-已完成 */
    private Integer status;

    /** 备注 */
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}