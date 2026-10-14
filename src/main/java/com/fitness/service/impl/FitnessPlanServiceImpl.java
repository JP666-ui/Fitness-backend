package com.fitness.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fitness.entity.FitnessPlan;
import com.fitness.entity.User;
import com.fitness.mapper.FitnessPlanMapper;
import com.fitness.service.CalorieCalculatorService;
import com.fitness.service.FitnessPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 健身计划 Service 实现类
 */
@Service
public class FitnessPlanServiceImpl extends ServiceImpl<FitnessPlanMapper, FitnessPlan>
        implements FitnessPlanService {

    @Autowired
    private CalorieCalculatorService calorieCalculator;

    @Override
    public FitnessPlan getActivePlan(Long userId) {
        return baseMapper.getActivePlan(userId, LocalDate.now());
    }

    @Override
    public FitnessPlan getLatestPlan(Long userId) {
        return baseMapper.getLatestPlan(userId);
    }

    @Override
    public List<FitnessPlan> getPlansByUserId(Long userId) {
        return baseMapper.getPlansByUserId(userId);
    }

    /**
     * 获取或创建：已有进行中的计划直接返回，避免重复创建
     */
    @Override
    public FitnessPlan getOrCreatePlan(User user, String planType) {
        // 1) 优先返回进行中的计划
        FitnessPlan active = getActivePlan(user.getId());
        if (active != null) {
            return active;
        }

        // 2) 没有进行中的，取最近一次计划，若仍在有效期内则复用
        FitnessPlan latest = getLatestPlan(user.getId());
        if (latest != null && latest.getEndDate() != null
                && !latest.getEndDate().isBefore(LocalDate.now())
                && latest.getStatus() != null && latest.getStatus() != 0) {
            // 重新激活
            latest.setStatus(1);
            latest.setUpdateTime(LocalDateTime.now());
            updateById(latest);
            return latest;
        }

        // 3) 确实没有可用计划，才创建新的
        String type = (planType == null || planType.isEmpty())
                ? (latest != null ? latest.getPlanType() : "maintain")
                : planType;
        return createDefaultPlan(user, type);
    }

    @Override
    public FitnessPlan createDefaultPlan(User user, String planType) {
        Long userId = user.getId();

        // 取消当前进行中的计划
        lambdaUpdate()
                .eq(FitnessPlan::getUserId, userId)
                .eq(FitnessPlan::getStatus, 1)
                .set(FitnessPlan::getStatus, 0)
                .update();

        // 目标体重：优先用户已设置的
        Double targetWeight = user.getTargetWeight();

        // 计算计划周期
        long days;
        if ("loss".equals(planType) && targetWeight != null && user.getWeight() != null
                && user.getWeight() > targetWeight) {
            // 按健康速度推算周期
            days = calorieCalculator.suggestDays(user, targetWeight);
        } else {
            days = 30;
        }
        if (days <= 0 || days > 365) {
            days = 30;
        }

        // 计算热量缺口与建议摄入
        int tdee = calorieCalculator.calculateTdee(user);
        int bmr = calorieCalculator.calculateBmr(user);
        int deficit = calorieCalculator.calculateDailyDeficit(user, targetWeight, days);

        int suggestedIntake;
        if ("gain".equals(planType)) {
            suggestedIntake = tdee + 300;
            deficit = -300;  // 负值表示盈余
        } else if ("loss".equals(planType)) {
            suggestedIntake = tdee - deficit;
            // 不低于基础代谢
            if (bmr > 0 && suggestedIntake < bmr) {
                suggestedIntake = bmr;
                deficit = tdee - bmr;
            }
        } else {
            suggestedIntake = tdee > 0 ? tdee : 2000;
            deficit = 0;
        }

        if (suggestedIntake <= 0) {
            suggestedIntake = 2000;
        }

        // 需减体重
        Double weightToLose = null;
        if (targetWeight != null && user.getWeight() != null) {
            double diff = user.getWeight() - targetWeight;
            if (diff > 0) {
                weightToLose = Math.round(diff * 10) / 10.0;
            }
        }

        FitnessPlan plan = new FitnessPlan();
        plan.setUserId(userId);
        plan.setPlanType(planType);
        plan.setTargetWeight(targetWeight);
        plan.setWeightToLose(weightToLose);
        plan.setTargetCalorie(suggestedIntake);
        plan.setDailyDeficit(deficit);
        plan.setStartDate(LocalDate.now());
        plan.setEndDate(LocalDate.now().plusDays(days));
        plan.setStatus(1);
        plan.setCreateTime(LocalDateTime.now());
        plan.setUpdateTime(LocalDateTime.now());

        // 每周减重速度，用于备注说明
        double weeklyLoss = calorieCalculator.weeklyLossByDeficit(deficit);

        switch (planType == null ? "maintain" : planType) {
            case "loss":
                plan.setPlanName("减脂计划");
                plan.setRemark(String.format(
                        "需减重 %.1fkg，每日热量缺口 %d kcal，建议摄入 %d kcal（TDEE %d kcal），每周约减 %.2fkg",
                        weightToLose == null ? 0 : weightToLose, deficit, suggestedIntake, tdee, weeklyLoss));
                break;
            case "gain":
                plan.setPlanName("增肌计划");
                plan.setRemark(String.format(
                        "每日热量盈余 300 kcal，建议摄入 %d kcal（TDEE %d kcal）",
                        suggestedIntake, tdee));
                break;
            case "shape":
                plan.setPlanName("塑形计划");
                plan.setRemark(String.format("建议摄入 %d kcal，配合力量与有氧训练", suggestedIntake));
                break;
            default:
                plan.setPlanName("维持计划");
                plan.setRemark(String.format("建议摄入 %d kcal，保持均衡饮食与规律运动", suggestedIntake));
                break;
        }

        save(plan);
        return plan;
    }

    @Override
    public boolean completePlan(Long planId) {
        FitnessPlan plan = getById(planId);
        if (plan == null) {
            return false;
        }
        plan.setStatus(2);
        plan.setUpdateTime(LocalDateTime.now());
        return updateById(plan);
    }

    @Override
    public boolean cancelPlan(Long planId) {
        FitnessPlan plan = getById(planId);
        if (plan == null) {
            return false;
        }
        plan.setStatus(0);
        plan.setUpdateTime(LocalDateTime.now());
        return updateById(plan);
    }

    /**
     * 构建计划详情：计划本身 + 热量缺口分析 + 进度
     */
    @Override
    public Map<String, Object> buildPlanDetail(FitnessPlan plan, User user) {
        Map<String, Object> result = new HashMap<>();

        if (plan == null) {
            // 无计划时也返回热量分析，方便前端展示基础代谢等信息
            result.put("plan", null);
            result.put("calorie", calorieCalculator.buildCalorieReport(user, null, null));
            return result;
        }

        result.put("plan", plan);

        // 热量缺口分析
        Map<String, Object> calorie = calorieCalculator.buildCalorieReport(
                user, plan.getTargetWeight(), plan.getEndDate());
        result.put("calorie", calorie);

        // 进度计算
        LocalDate start = plan.getStartDate() == null ? LocalDate.now() : plan.getStartDate();
        LocalDate end = plan.getEndDate() == null ? LocalDate.now().plusDays(30) : plan.getEndDate();
        LocalDate today = LocalDate.now();

        long totalDays = ChronoUnit.DAYS.between(start, end);
        long passedDays = ChronoUnit.DAYS.between(start, today);
        long remainingDays = ChronoUnit.DAYS.between(today, end);

        if (totalDays <= 0) {
            totalDays = 1;
        }

        int progress = (int) Math.round(passedDays * 100.0 / totalDays);
        progress = Math.max(0, Math.min(100, progress));

        Map<String, Object> progressInfo = new HashMap<>();
        progressInfo.put("totalDays", totalDays);
        progressInfo.put("passedDays", Math.max(0, passedDays));
        progressInfo.put("remainingDays", Math.max(0, remainingDays));
        progressInfo.put("percent", progress);
        progressInfo.put("startDate", start.toString());
        progressInfo.put("endDate", end.toString());
        result.put("progress", progressInfo);

        return result;
    }
}