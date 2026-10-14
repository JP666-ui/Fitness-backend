package com.fitness.controller;

import com.fitness.common.Result;
import com.fitness.entity.ExerciseRecord;
import com.fitness.entity.FitnessPlan;
import com.fitness.entity.FoodRecord;
import com.fitness.entity.User;
import com.fitness.service.CalorieCalculatorService;
import com.fitness.service.ExerciseRecordService;
import com.fitness.service.FitnessPlanService;
import com.fitness.service.FoodRecordService;
import com.fitness.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 数据统计 Controller
 * <p>
 * 最小统计单位为「日」，提供每日摄入、每日消耗、每日热量缺口。
 */
@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private FoodRecordService foodRecordService;

    @Autowired
    private ExerciseRecordService exerciseRecordService;

    @Autowired
    private UserService userService;

    @Autowired
    private FitnessPlanService fitnessPlanService;

    @Autowired
    private CalorieCalculatorService calorieCalculator;

    /**
     * 今日统计
     * GET /statistics/today
     */
    @GetMapping("/today")
    public Result<Map<String, Object>> getTodayStatistics(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getById(userId);

        int intake = safeIntake(userId);
        int burn = safeBurn(userId);

        // 目标摄入：优先取进行中计划，其次用户设置，最后按 TDEE 推算
        int target = resolveTargetCalorie(user);

        Map<String, Object> stats = new HashMap<>();
        stats.put("todayIntake", intake);
        stats.put("todayBurn", burn);
        stats.put("netCalorie", intake - burn);
        stats.put("targetCalorie", target);
        stats.put("remaining", target - intake);
        // 今日热量缺口 = 消耗 − 摄入（正数表示有缺口，利于减脂）
        stats.put("todayDeficit", burn - intake);
        // 相对于目标：未超标则为正
        stats.put("targetDeficit", target - intake);

        return Result.success(stats);
    }

    /**
     * 每日统计（核心接口）
     * GET /statistics/daily?days=7
     * <p>
     * 返回每一天的：摄入、消耗、热量缺口、是否达标。
     * <p>
     * 统计起点以<b>训练计划的创建时间</b>为准：
     * 若计划创建于所请求区间之内，则只统计计划开始之后的日期，
     * 避免把建计划之前的空白日子也算进达标率。
     */
    @GetMapping("/daily")
    public Result<Map<String, Object>> getDailyStatistics(HttpServletRequest request,
                                                          @RequestParam(defaultValue = "7") Integer days) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getById(userId);

        if (days == null || days <= 0) {
            days = 7;
        }
        if (days > 90) {
            days = 90;
        }

        LocalDate end = LocalDate.now();
        LocalDate requestedStart = end.minusDays(days - 1L);

        // 取当前计划，用于确定统计起点
        FitnessPlan plan = fitnessPlanService.getActivePlan(userId);
        if (plan == null) {
            plan = fitnessPlanService.getLatestPlan(userId);
        }

        LocalDate start = requestedStart;
        LocalDate planStart = null;
        if (plan != null && plan.getStartDate() != null) {
            planStart = plan.getStartDate();
            // 计划开始晚于请求区间起点时，从计划开始日算起
            if (planStart.isAfter(requestedStart)) {
                start = planStart;
            }
        }
        // 兜底：起点不能晚于今天
        if (start.isAfter(end)) {
            start = end;
        }

        // 实际统计天数
        int actualDays = (int) (java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1);
        if (actualDays <= 0) {
            actualDays = 1;
        }

        // 查询区间内的记录
        Date startDate = toDate(start.atStartOfDay());
        Date endDate = toDate(end.atTime(LocalTime.MAX));
        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endTime = end.atTime(LocalTime.MAX);

        List<FoodRecord> foodRecords = foodRecordService.lambdaQuery()
                .eq(FoodRecord::getUserId, userId)
                .ge(FoodRecord::getRecordTime, startDate)
                .le(FoodRecord::getRecordTime, endDate)
                .list();

        List<ExerciseRecord> exerciseRecords = exerciseRecordService.lambdaQuery()
                .eq(ExerciseRecord::getUserId, userId)
                .ge(ExerciseRecord::getExerciseTime, startTime)
                .le(ExerciseRecord::getExerciseTime, endTime)
                .list();

        // 按天聚合
        Map<String, Integer> intakeMap = new HashMap<>();
        for (FoodRecord r : foodRecords) {
            if (r.getRecordTime() == null || r.getCalorie() == null) {
                continue;
            }
            String day = toLocalDate(r.getRecordTime()).format(DAY_FMT);
            intakeMap.merge(day, r.getCalorie(), Integer::sum);
        }

        Map<String, Integer> burnMap = new HashMap<>();
        for (ExerciseRecord r : exerciseRecords) {
            if (r.getExerciseTime() == null || r.getCalorie() == null) {
                continue;
            }
            String day = r.getExerciseTime().toLocalDate().format(DAY_FMT);
            burnMap.merge(day, r.getCalorie(), Integer::sum);
        }

        int target = resolveTargetCalorie(user);
        int bmr = calorieCalculator.calculateBmr(user);
        // 基础消耗：BMR × 1.2（久坐状态下的日常消耗，不含额外运动）
        // 运动消耗单独累加，避免与 TDEE 的活动中等系数重复计算
        int baseBurn = bmr > 0 ? (int) Math.round(bmr * 1.2) : 0;
        int tdee = calorieCalculator.calculateTdee(user);

        // 组装每日数据
        List<Map<String, Object>> dailyList = new ArrayList<>();
        int totalIntake = 0;
        int totalExerciseBurn = 0;
        int totalBurnAll = 0;
        int daysOnTarget = 0;
        int daysWithRecord = 0;

        for (int i = 0; i < actualDays; i++) {
            LocalDate day = start.plusDays(i);
            String key = day.format(DAY_FMT);

            int intake = intakeMap.getOrDefault(key, 0);
            int exerciseBurn = burnMap.getOrDefault(key, 0);
            // 当日总消耗 = 基础消耗 + 运动消耗
            int totalBurn = baseBurn + exerciseBurn;
            // 热量缺口 = 总消耗 − 摄入；正数表示有缺口（利于减脂），负数表示盈余
            int deficit = totalBurn - intake;
            // 相对于目标摄入的差额：正数=还有余量，负数=超标
            int targetDiff = target - intake;

            totalIntake += intake;
            totalExerciseBurn += exerciseBurn;
            totalBurnAll += totalBurn;
            if (intake > 0) {
                daysWithRecord++;
                if (intake <= target) {
                    daysOnTarget++;
                }
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", key);
            item.put("intake", intake);
            // 运动消耗（来自运动记录）
            item.put("exerciseBurn", exerciseBurn);
            item.put("burn", exerciseBurn);
            // 基础消耗（基础代谢 + 日常活动）
            item.put("baseBurn", baseBurn);
            // 总消耗
            item.put("totalBurn", totalBurn);
            item.put("deficit", deficit);
            item.put("netIntake", intake - totalBurn);
            item.put("targetDiff", targetDiff);
            item.put("onTarget", intake > 0 && intake <= target);
            item.put("hasRecord", intake > 0);
            dailyList.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("daily", dailyList);
        // 实际参与统计的天数（可能因计划开始时间而少于请求天数）
        result.put("days", actualDays);
        result.put("requestedDays", days);
        result.put("startDate", start.format(DAY_FMT));
        result.put("endDate", end.format(DAY_FMT));
        // 统计起点是否因计划而收窄
        result.put("clampedByPlan", planStart != null && planStart.isAfter(requestedStart));

        // ---------- 计划周期（用于「时间进度」） ----------
        if (plan != null) {
            Map<String, Object> planInfo = new LinkedHashMap<>();
            planInfo.put("id", plan.getId());
            planInfo.put("planName", plan.getPlanName());
            planInfo.put("planType", plan.getPlanType());
            planInfo.put("targetWeight", plan.getTargetWeight());
            planInfo.put("targetCalorie", plan.getTargetCalorie());
            planInfo.put("dailyDeficit", plan.getDailyDeficit());

            LocalDate pStart = plan.getStartDate();
            LocalDate pEnd = plan.getEndDate();

            if (pStart != null) {
                planInfo.put("startDate", pStart.format(DAY_FMT));
            }
            if (pEnd != null) {
                planInfo.put("endDate", pEnd.format(DAY_FMT));
            }

            // 时间进度：已过天数 / 总天数
            if (pStart != null && pEnd != null) {
                long totalDays = java.time.temporal.ChronoUnit.DAYS.between(pStart, pEnd);
                if (totalDays <= 0) {
                    totalDays = 1;
                }
                long passedDays = java.time.temporal.ChronoUnit.DAYS.between(pStart, end);
                long remainingDays = java.time.temporal.ChronoUnit.DAYS.between(end, pEnd);

                // 进度百分比
                int timePercent = (int) Math.round(passedDays * 100.0 / totalDays);
                timePercent = Math.max(0, Math.min(100, timePercent));

                planInfo.put("totalDays", totalDays);
                planInfo.put("passedDays", Math.max(0, passedDays));
                planInfo.put("remainingDays", Math.max(0, remainingDays));
                planInfo.put("timePercent", timePercent);
                planInfo.put("expired", pEnd.isBefore(end));
                planInfo.put("notStarted", pStart.isAfter(end));
            }

            result.put("plan", planInfo);
        }
        if (planStart != null) {
            result.put("planStartDate", planStart.format(DAY_FMT));
        }

        // 汇总
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalIntake", totalIntake);
        // 运动消耗合计
        summary.put("totalExerciseBurn", totalExerciseBurn);
        summary.put("totalBurn", totalExerciseBurn);
        // 含基础代谢的总消耗
        summary.put("totalBurnAll", totalBurnAll);
        // 累计缺口 = 累计总消耗 − 累计摄入
        summary.put("totalDeficit", totalBurnAll - totalIntake);
        summary.put("avgIntake", actualDays > 0 ? totalIntake / actualDays : 0);
        summary.put("avgBurn", actualDays > 0 ? totalExerciseBurn / actualDays : 0);
        summary.put("avgTotalBurn", actualDays > 0 ? totalBurnAll / actualDays : 0);
        summary.put("baseBurn", baseBurn);
        summary.put("targetCalorie", target);
        summary.put("bmr", bmr);
        summary.put("tdee", tdee);
        summary.put("daysOnTarget", daysOnTarget);
        summary.put("daysWithRecord", daysWithRecord);
        summary.put("statDays", actualDays);
        summary.put("onTargetRate", daysWithRecord > 0
                ? Math.round(daysOnTarget * 100.0 / daysWithRecord) : 0);
        result.put("summary", summary);

        return Result.success(result);
    }

    /**
     * 获取统计数据（兼容旧接口，内部转发到 daily）
     * GET /statistics?timeRange=week
     */
    @GetMapping
    public Result<Map<String, Object>> getStatistics(HttpServletRequest request,
                                                     @RequestParam(required = false) String timeRange) {
        int days = 7;
        if ("month".equals(timeRange)) {
            days = 30;
        } else if ("year".equals(timeRange)) {
            days = 90;
        }
        return getDailyStatistics(request, days);
    }

    /**
     * 周统计
     * GET /statistics/week
     */
    @GetMapping("/week")
    public Result<Map<String, Object>> getWeekStatistics(HttpServletRequest request) {
        return getDailyStatistics(request, 7);
    }

    /**
     * 月统计
     * GET /statistics/month
     */
    @GetMapping("/month")
    public Result<Map<String, Object>> getMonthStatistics(HttpServletRequest request) {
        return getDailyStatistics(request, 30);
    }

    /**
     * 年统计
     * GET /statistics/year
     */
    @GetMapping("/year")
    public Result<Map<String, Object>> getYearStatistics(HttpServletRequest request) {
        return getDailyStatistics(request, 90);
    }

    // ==================================================================
    // 辅助方法
    // ==================================================================

    /**
     * 解析用户的目标摄入热量
     * 优先级：进行中计划 > 用户设置 > TDEE
     */
    private int resolveTargetCalorie(User user) {
        if (user == null) {
            return 2000;
        }

        // 1) 进行中的计划
        FitnessPlan plan = fitnessPlanService.getActivePlan(user.getId());
        if (plan != null && plan.getTargetCalorie() != null && plan.getTargetCalorie() > 0) {
            return plan.getTargetCalorie();
        }

        // 2) 用户个人设置
        if (user.getDailyCalorieGoal() != null && user.getDailyCalorieGoal() > 0) {
            return user.getDailyCalorieGoal();
        }

        // 3) 按 TDEE 推算
        int tdee = calorieCalculator.calculateTdee(user);
        return tdee > 0 ? tdee : 2000;
    }

    private int safeIntake(Long userId) {
        try {
            Integer v = foodRecordService.getTodayCalorie(userId);
            return v == null ? 0 : v;
        } catch (Exception e) {
            return 0;
        }
    }

    private int safeBurn(Long userId) {
        try {
            Integer v = exerciseRecordService.getTodayCalorieBurn(userId);
            return v == null ? 0 : v;
        } catch (Exception e) {
            return 0;
        }
    }

    private Date toDate(LocalDateTime ldt) {
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }

    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}