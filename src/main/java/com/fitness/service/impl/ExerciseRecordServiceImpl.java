package com.fitness.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fitness.entity.ExerciseRecord;
import com.fitness.mapper.ExerciseRecordMapper;
import com.fitness.service.ExerciseRecordService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 运动记录 Service 实现类
 */
@Service
public class ExerciseRecordServiceImpl extends ServiceImpl<ExerciseRecordMapper, ExerciseRecord> 
        implements ExerciseRecordService {
    
    /**
     * 各运动项目的 MET 值（代谢当量）
     * 卡路里 = MET × 体重(kg) × 时长(小时)
     */
    private static final Map<String, Double> EXERCISE_METS = new HashMap<>();
    
    static {
        // 中文名称
        EXERCISE_METS.put("跑步", 8.0);
        EXERCISE_METS.put("骑行", 7.5);
        EXERCISE_METS.put("游泳", 8.0);
        EXERCISE_METS.put("健身", 6.0);
        EXERCISE_METS.put("瑜伽", 3.0);
        EXERCISE_METS.put("快走", 4.5);
        EXERCISE_METS.put("跳绳", 10.0);
        EXERCISE_METS.put("篮球", 8.5);
        EXERCISE_METS.put("羽毛球", 6.5);
        EXERCISE_METS.put("力量训练", 6.0);
        EXERCISE_METS.put("其他", 4.0);
        
        // 英文标识
        EXERCISE_METS.put("running", 8.0);
        EXERCISE_METS.put("cycling", 7.5);
        EXERCISE_METS.put("swimming", 8.0);
        EXERCISE_METS.put("gym", 6.0);
        EXERCISE_METS.put("yoga", 3.0);
        EXERCISE_METS.put("walking", 4.5);
        EXERCISE_METS.put("rope", 10.0);
        EXERCISE_METS.put("basketball", 8.5);
        EXERCISE_METS.put("badminton", 6.5);
        EXERCISE_METS.put("strength", 6.0);
        EXERCISE_METS.put("other", 4.0);
    }
    
    @Override
    public Integer getTodayCalorieBurn(Long userId) {
        if (userId == null) {
            return 0;
        }
        Integer result = baseMapper.getTodayCalorieBurn(userId);
        return result == null ? 0 : result;
    }
    
    @Override
    public Map<String, Object> getStatistics(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.getStatisticsByDateRange(userId, startTime, endTime);
    }
    
    @Override
    public List<Map<String, Object>> getStatisticsByType(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.getStatisticsByType(userId, startTime, endTime);
    }
    
    @Override
    public Integer calculateCalorie(String exerciseType, Integer duration, Double weight) {
        if (exerciseType == null || duration == null || weight == null) {
            return 0;
        }
        
        Double met = EXERCISE_METS.getOrDefault(exerciseType, 4.0);
        // 卡路里 = MET × 体重(kg) × 时长(小时)
        double hours = duration / 60.0;
        return (int) Math.round(met * weight * hours);
    }
}