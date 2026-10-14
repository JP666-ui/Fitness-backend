package com.fitness.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fitness.entity.ExerciseRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 运动记录 Service 接口
 */
public interface ExerciseRecordService extends IService<ExerciseRecord> {
    
    /**
     * 获取用户今日运动消耗
     */
    Integer getTodayCalorieBurn(Long userId);
    
    /**
     * 获取用户某段时间的运动统计
     */
    Map<String, Object> getStatistics(Long userId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 按运动类型统计
     */
    List<Map<String, Object>> getStatisticsByType(Long userId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 计算运动消耗的热量
     *
     * @param exerciseType 运动类型标识（如 running / cycling）或运动名称（如 跑步）
     * @param duration     时长(分钟)
     * @param weight       体重(kg)
     * @return 消耗卡路里
     */
    Integer calculateCalorie(String exerciseType, Integer duration, Double weight);
}