package com.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.entity.ExerciseRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 运动记录 Mapper
 */
@Mapper
public interface ExerciseRecordMapper extends BaseMapper<ExerciseRecord> {
    
    /**
     * 统计用户某段时间内的运动数据
     */
    @Select("SELECT COUNT(*) as exerciseCount, " +
            "COALESCE(SUM(duration), 0) as totalDuration, " +
            "COALESCE(SUM(calorie_burn), 0) as totalCalorie " +
            "FROM exercise_record " +
            "WHERE user_id = #{userId} AND record_time BETWEEN #{startTime} AND #{endTime}")
    Map<String, Object> getStatisticsByDateRange(@Param("userId") Long userId,
                                                 @Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);
    
    /**
     * 获取用户今日运动消耗
     */
    @Select("SELECT COALESCE(SUM(calorie_burn), 0) FROM exercise_record " +
            "WHERE user_id = #{userId} AND DATE(record_time) = CURDATE()")
    Integer getTodayCalorieBurn(@Param("userId") Long userId);
    
    /**
     * 按运动类型统计
     */
    @Select("SELECT exercise_type, exercise_name, COUNT(*) as count, " +
            "COALESCE(SUM(calorie_burn), 0) as totalCalorie " +
            "FROM exercise_record " +
            "WHERE user_id = #{userId} AND record_time BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY exercise_type, exercise_name")
    List<Map<String, Object>> getStatisticsByType(@Param("userId") Long userId,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);
}