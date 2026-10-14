package com.fitness.controller;

import com.fitness.common.Result;
import com.fitness.entity.FitnessPlan;
import com.fitness.entity.User;
import com.fitness.service.CalorieCalculatorService;
import com.fitness.service.FitnessPlanService;
import com.fitness.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 健身计划 Controller
 * <p>
 * 核心设计：进入页面时自动加载当前/上一次的计划，无需每次重新创建。
 */
@RestController
@RequestMapping("/plan")
public class FitnessPlanController {

    @Autowired
    private FitnessPlanService fitnessPlanService;

    @Autowired
    private UserService userService;

    @Autowired
    private CalorieCalculatorService calorieCalculator;

    /**
     * 获取当前计划（自动加载）
     * GET /plan/current
     * <p>
     * 返回：计划 + 热量缺口分析 + 进度信息。
     * 若从未创建过计划，会根据用户资料自动生成一个（资料不足时 plan 为 null，
     * 但仍返回热量分析，供页面提示用户补全资料）。
     */
    @GetMapping("/current")
    public Result<Map<String, Object>> getCurrent(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getById(userId);

        FitnessPlan plan = fitnessPlanService.getActivePlan(userId);
        if (plan == null) {
            // 没有进行中的，尝试复用最近一次
            plan = fitnessPlanService.getLatestPlan(userId);
        }

        // 仍无计划且资料完整时，自动按目标体重生成，避免用户每次手动创建
        if (plan == null && user != null && calorieCalculator.calculateBmr(user) > 0) {
            String type = "maintain";
            if (user.getWeight() != null && user.getTargetWeight() != null) {
                double diff = user.getWeight() - user.getTargetWeight();
                if (diff > 0.5) {
                    type = "loss";
                } else if (diff < -0.5) {
                    type = "gain";
                }
            }
            plan = fitnessPlanService.createDefaultPlan(user, type);
        }

        return Result.success(fitnessPlanService.buildPlanDetail(plan, user));
    }

    /**
     * 获取热量缺口分析（不依赖计划）
     * GET /plan/calorie-analysis?targetWeight=75
     * <p>
     * 返回完整推导过程：需减体重 → 总热量 → 每日缺口 → 建议摄入
     */
    @GetMapping("/calorie-analysis")
    public Result<Map<String, Object>> calorieAnalysis(HttpServletRequest request,
                                                       @RequestParam(required = false) Double targetWeight) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getById(userId);

        Double target = targetWeight != null ? targetWeight
                : (user == null ? null : user.getTargetWeight());

        return Result.success(calorieCalculator.buildCalorieReport(user, target, null));
    }

    /**
     * 按指定缺口反推所需天数
     * GET /plan/deficit-preview?targetWeight=75&dailyDeficit=500
     * <p>
     * 用于「调整目标」时实时预览：给定缺口能多久达成。
     */
    @GetMapping("/deficit-preview")
    public Result<Map<String, Object>> deficitPreview(HttpServletRequest request,
                                                      @RequestParam Double targetWeight,
                                                      @RequestParam Integer dailyDeficit) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getById(userId);

        Map<String, Object> result = new HashMap<>();

        if (user == null || user.getWeight() == null || targetWeight == null || dailyDeficit == null) {
            return Result.fail(400, "参数不完整");
        }

        double weightToLose = user.getWeight() - targetWeight;
        if (weightToLose <= 0) {
            result.put("message", "目标体重不低于当前体重，无需热量缺口");
            result.put("weightToLose", 0);
            return Result.success(result);
        }

        int tdee = calorieCalculator.calculateTdee(user);
        int bmr = calorieCalculator.calculateBmr(user);

        // 请求的缺口
        int requestedDeficit = dailyDeficit;
        int suggestedIntake = tdee - dailyDeficit;
        boolean bmrProtected = false;

        // 保护：摄入不得低于基础代谢，此时按实际缺口重新计算
        if (bmr > 0 && suggestedIntake < bmr) {
            suggestedIntake = bmr;
            dailyDeficit = tdee - bmr;
            bmrProtected = true;
        }

        long days = calorieCalculator.daysByDeficit(weightToLose, dailyDeficit);
        double weeklyLoss = calorieCalculator.weeklyLossByDeficit(dailyDeficit);
        Map<String, String> safety = calorieCalculator.evaluateSafety(weeklyLoss);

        result.put("weightToLose", Math.round(weightToLose * 10) / 10.0);
        result.put("totalDeficit", Math.round(weightToLose * 7700));
        result.put("requestedDeficit", requestedDeficit);
        result.put("dailyDeficit", dailyDeficit);
        result.put("days", days);
        result.put("endDate", LocalDate.now().plusDays(days).toString());
        result.put("suggestedIntake", suggestedIntake);
        result.put("tdee", tdee);
        result.put("bmr", bmr);
        result.put("bmrProtected", bmrProtected);
        result.put("weeklyLoss", weeklyLoss);
        result.put("safety", safety.get("level"));
        result.put("safetyText", safety.get("text"));

        return Result.success(result);
    }

    /**
     * 获取或创建计划（幂等）
     * POST /plan/get-or-create
     * <p>
     * 已有计划时直接返回，不会重复创建。
     *
     * @param params { planType: "loss" | "gain" | "shape" | "maintain" }
     */
    @PostMapping("/get-or-create")
    public Result<Map<String, Object>> getOrCreate(HttpServletRequest request,
                                                   @RequestBody(required = false) Map<String, Object> params) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getById(userId);

        String planType = params == null ? null : (String) params.get("planType");
        FitnessPlan plan = fitnessPlanService.getOrCreatePlan(user, planType);

        return Result.success(fitnessPlanService.buildPlanDetail(plan, user));
    }

    /**
     * 强制创建新计划（会替换当前计划）
     * POST /plan/create
     */
    @PostMapping("/create")
    public Result<Map<String, Object>> create(HttpServletRequest request,
                                              @RequestBody(required = false) Map<String, Object> params) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getById(userId);

        String planType = params == null ? "maintain" : (String) params.getOrDefault("planType", "maintain");
        FitnessPlan plan = fitnessPlanService.createDefaultPlan(user, planType);

        return Result.success(fitnessPlanService.buildPlanDetail(plan, user));
    }

    /**
     * 更新计划（如调整目标体重、周期）
     * PUT /plan/update/{id}
     */
    @PutMapping("/update/{id}")
    public Result<Map<String, Object>> update(HttpServletRequest request,
                                              @PathVariable Long id,
                                              @RequestBody FitnessPlan plan) {
        Long userId = (Long) request.getAttribute("userId");

        FitnessPlan existing = fitnessPlanService.getById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            return Result.fail(404, "计划不存在");
        }

        plan.setId(id);
        plan.setUserId(userId);

        // 若调整了目标体重或周期，重新计算建议摄入
        User user = userService.getById(userId);
        Double targetWeight = plan.getTargetWeight() != null ? plan.getTargetWeight() : existing.getTargetWeight();
        LocalDate endDate = plan.getEndDate() != null ? plan.getEndDate() : existing.getEndDate();

        if (targetWeight != null && endDate != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), endDate);
            int tdee = calorieCalculator.calculateTdee(user);
            int bmr = calorieCalculator.calculateBmr(user);
            int deficit = calorieCalculator.calculateDailyDeficit(user, targetWeight, days);

            String type = plan.getPlanType() != null ? plan.getPlanType() : existing.getPlanType();
            int intake;
            if ("gain".equals(type)) {
                intake = tdee + 300;
                deficit = -300;
            } else if ("loss".equals(type)) {
                intake = tdee - deficit;
                if (bmr > 0 && intake < bmr) {
                    intake = bmr;
                    deficit = tdee - bmr;
                }
            } else {
                intake = tdee > 0 ? tdee : 2000;
                deficit = 0;
            }
            if (intake > 0) {
                plan.setTargetCalorie(intake);
            }
            plan.setDailyDeficit(deficit);

            // 同步更新需减体重
            if (user.getWeight() != null) {
                double diff = user.getWeight() - targetWeight;
                plan.setWeightToLose(diff > 0 ? Math.round(diff * 10) / 10.0 : null);
            }
        }

        fitnessPlanService.updateById(plan);
        return Result.success(fitnessPlanService.buildPlanDetail(
                fitnessPlanService.getById(id), user));
    }

    /**
     * 获取用户的所有计划
     * GET /plan/list
     */
    @GetMapping("/list")
    public Result<List<FitnessPlan>> getPlans(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(fitnessPlanService.getPlansByUserId(userId));
    }

    /**
     * 获取进行中的计划
     * GET /plan/active
     */
    @GetMapping("/active")
    public Result<FitnessPlan> getActivePlan(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        FitnessPlan plan = fitnessPlanService.getActivePlan(userId);
        if (plan == null) {
            return Result.fail(404, "暂无进行中的计划");
        }
        return Result.success(plan);
    }

    /**
     * 完成计划
     * PUT /plan/complete/{id}
     */
    @PutMapping("/complete/{id}")
    public Result<Boolean> complete(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        FitnessPlan existing = fitnessPlanService.getById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            return Result.fail(404, "计划不存在");
        }
        return Result.success(fitnessPlanService.completePlan(id));
    }

    /**
     * 取消计划
     * PUT /plan/cancel/{id}
     */
    @PutMapping("/cancel/{id}")
    public Result<Boolean> cancel(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        FitnessPlan existing = fitnessPlanService.getById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            return Result.fail(404, "计划不存在");
        }
        return Result.success(fitnessPlanService.cancelPlan(id));
    }

    /**
     * 删除计划
     * DELETE /plan/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        FitnessPlan existing = fitnessPlanService.getById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            return Result.fail(404, "计划不存在");
        }
        return Result.success(fitnessPlanService.removeById(id));
    }

    /**
     * 获取计划类型列表
     * GET /plan/types
     */
    @GetMapping("/types")
    public Result<List<Map<String, Object>>> getPlanTypes() {
        return Result.success(List.of(
                Map.of("type", "loss", "name", "减脂计划",
                        "description", "控制饮食 + 有氧运动，科学减少体脂", "icon", "🔥"),
                Map.of("type", "gain", "name", "增肌计划",
                        "description", "力量训练 + 高蛋白饮食，增加肌肉量", "icon", "💪"),
                Map.of("type", "shape", "name", "塑形计划",
                        "description", "综合训练，塑造理想身材线条", "icon", "✨"),
                Map.of("type", "maintain", "name", "维持计划",
                        "description", "保持健康生活方式，维持当前体重", "icon", "⚖️")
        ));
    }
}