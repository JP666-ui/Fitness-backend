package com.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.entity.WeightRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日体重记录 Mapper
 */
@Mapper
public interface WeightRecordMapper extends BaseMapper<WeightRecord> {

    /**
     * 查询指定日期区间的体重记录（按日期升序）
     */
    @Select("SELECT * FROM weight_record WHERE user_id = #{userId} "
            + "AND record_date BETWEEN #{start} AND #{end} ORDER BY record_date ASC")
    List<WeightRecord> findByDateRange(@Param("userId") Long userId,
                                       @Param("start") LocalDate start,
                                       @Param("end") LocalDate end);

    /**
     * 查询某天的记录
     */
    @Select("SELECT * FROM weight_record WHERE user_id = #{userId} AND record_date = #{date} LIMIT 1")
    WeightRecord findByDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * 查询最近一条记录
     */
    @Select("SELECT * FROM weight_record WHERE user_id = #{userId} "
            + "ORDER BY record_date DESC LIMIT 1")
    WeightRecord findLatest(@Param("userId") Long userId);

    /**
     * 查询指定日期当天或之前最近的一条记录
     * <p>
     * 用于获取「计划开始时的体重」作为进度基准。
     */
    @Select("SELECT * FROM weight_record WHERE user_id = #{userId} AND record_date <= #{date} "
            + "ORDER BY record_date DESC LIMIT 1")
    WeightRecord findLatestBefore(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * 查询指定日期当天或之后最早的一条记录
     */
    @Select("SELECT * FROM weight_record WHERE user_id = #{userId} AND record_date >= #{date} "
            + "ORDER BY record_date ASC LIMIT 1")
    WeightRecord findEarliestAfter(@Param("userId") Long userId, @Param("date") LocalDate date);
}