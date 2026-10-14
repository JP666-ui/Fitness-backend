package com.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.entity.FitnessPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 健身计划 Mapper
 */
@Mapper
public interface FitnessPlanMapper extends BaseMapper<FitnessPlan> {

    /**
     * 获取用户进行中的计划
     */
    @Select("SELECT * FROM fitness_plan WHERE user_id = #{userId} AND status = 1 "
            + "AND (end_date IS NULL OR end_date >= #{today}) "
            + "ORDER BY create_time DESC LIMIT 1")
    FitnessPlan getActivePlan(@Param("userId") Long userId, @Param("today") LocalDate today);

    /**
     * 获取用户最近一次的计划（不论状态）
     * <p>
     * 用于自动复用上一次的计划，避免重复创建。
     */
    @Select("SELECT * FROM fitness_plan WHERE user_id = #{userId} "
            + "ORDER BY create_time DESC LIMIT 1")
    FitnessPlan getLatestPlan(@Param("userId") Long userId);

    /**
     * 获取用户的所有计划
     */
    @Select("SELECT * FROM fitness_plan WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<FitnessPlan> getPlansByUserId(@Param("userId") Long userId);
}