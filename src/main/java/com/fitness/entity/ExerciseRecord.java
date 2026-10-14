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
 * 运动记录实体类
 * <p>
 * 对应数据库表 exercise_record
 */
@Data
@TableName("exercise_record")
public class ExerciseRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 运动名称，如：跑步、骑行、游泳 */
    private String exerciseName;

    /** 运动类型标识，如：running / cycling / swimming / gym / yoga / other */
    private String exerciseType;

    /** 运动时长(分钟) */
    private Integer duration;

    /** 消耗热量(kcal) */
    @TableField("calorie_burn")
    private Integer calorie;

    /** 记录日期（用于按天分组统计） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField(fill = FieldFill.INSERT)
    private LocalDate recordDate;

    /** 记录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "record_time", fill = FieldFill.INSERT)
    private LocalDateTime exerciseTime;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}