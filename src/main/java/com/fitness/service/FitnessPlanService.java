package com.fitness.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fitness.entity.FitnessPlan;
import com.fitness.entity.User;

import java.util.List;
import java.util.Map;

/**
 * 健身计划 Service 接口
 */
public interface FitnessPlanService extends IService<FitnessPlan> {

    /**
     * 获取用户进行中的计划
     */
    FitnessPlan getActivePlan(Long userId);

    /**
     * 获取用户最近一次的计划（含已结束的）
     * <p>
     * 用于「自动调用上一次的计划」，避免每次进入页面都要重新创建。
     */
    FitnessPlan getLatestPlan(Long userId);

    /**
     * 获取用户的所有计划
     */
    List<FitnessPlan> getPlansByUserId(Long userId);

    /**
     * 获取或创建计划：已有计划直接返回，没有才创建
     *
     * @param user     用户
     * @param planType 计划类型：loss/gain/shape/maintain
     * @return 计划
     */
    FitnessPlan getOrCreatePlan(User user, String planType);

    /**
     * 创建默认健身计划
     *
     * @param user     用户
     * @param planType 计划类型：loss/gain/shape/maintain
     */
    FitnessPlan createDefaultPlan(User user, String planType);

    /**
     * 完成计划
     */
    boolean completePlan(Long planId);

    /**
     * 取消计划
     */
    boolean cancelPlan(Long planId);

    /**
     * 构建计划详情（含热量缺口分析）
     */
    Map<String, Object> buildPlanDetail(FitnessPlan plan, User user);
}