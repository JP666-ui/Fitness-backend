package com.fitness.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fitness.entity.FoodRecord;

/**
 * 饮食记录 Service 接口
 */
public interface FoodRecordService extends IService<FoodRecord> {

    /**
     * 获取指定用户今日摄入总热量
     *
     * @param userId 用户ID
     * @return 今日总热量(kcal)，无记录返回 0
     */
    Integer getTodayCalorie(Long userId);
}