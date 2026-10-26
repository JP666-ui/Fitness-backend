package com.fitness.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fitness.common.Result;
import com.fitness.entity.ExerciseRecord;
import com.fitness.entity.FitnessPlan;
import com.fitness.entity.FoodRecord;
import com.fitness.entity.User;
import com.fitness.entity.WeightRecord;
import com.fitness.mapper.WeightRecordMapper;
import com.fitness.service.ExerciseRecordService;
import com.fitness.service.FitnessPlanService;
import com.fitness.service.FoodRecordService;
import com.fitness.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 管理员 · 用户任务执行监控
 * <p>
 * 展示每个普通用户的计划执行与记录打卡情况：
 * <ul>
 *     <li>是否有进行中的健身计划、目标缺口</li>
 *     <li>今日是否记录饮食 / 运动 / 体重</li>
 *     <li>近 7 天打卡天数与达标天数</li>
 *     <li>综合执行率评分（用于排序与提醒）</li>
 * </ul>
 * 路径 /admin/** 由 JwtInterceptor 统一做管理员鉴权。
 */
@RestController
@RequestMapping("/admin/task")
public class AdminTaskController {

    @Autowired
    private UserService userService;

    @Autowired
    private FitnessPlanService fitnessPlanService;

    @Autowired
    private FoodRecordService foodRecordService;

    @Autowired
    private ExerciseRecordService exerciseRecordService;

    @Autowired
    private WeightRecordMapper weightRecordMapper;

    /**
     * 所有普通用户的任务执行总览
     * GET /admin/task/overview?days=7
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(@RequestParam(defaultValue = "7") Integer days) {
        if (days == null || days <= 0) days = 7;
        if (days > 30) days = 30;

        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(days - 1L);
        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endTime = today.atTime(LocalTime.MAX);

        // 只监控普通用户（role=0）且未禁用的
        QueryWrapper<User> uw = new QueryWrapper<>();
        uw.eq("status", 1);
        uw.and(w -> w.eq("role", 0).or().isNull("role"));
        List<User> users = userService.list(uw);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (User u : users) {
            rows.add(buildUserRow(u, today, startTime, endTime, days));
        }

        // 按执行率降序，方便管理员先看懈怠的排在后面、勤奋的在前
        rows.sort((a, b) -> Integer.compare(
                (int) b.getOrDefault("score", 0),
                (int) a.getOrDefault("score", 0)));

        Map<String, Object> data = new HashMap<>();
        data.put("days", days);
        data.put("startDate", start.toString());
        data.put("endDate", today.toString());
        data.put("total", rows.size());
        data.put("list", rows);

        // 汇总
        long activeUsers = rows.stream().filter(r -> (int) r.getOrDefault("recordDays", 0) > 0).count();
        data.put("activeUsers", activeUsers);
        data.put("idleUsers", rows.size() - activeUsers);

        return Result.success(data);
    }

    /**
     * 单个用户的任务执行详情
     * GET /admin/task/detail/{userId}?days=7
     */
    @GetMapping("/detail/{userId}")
    public Result<Map<String, Object>> detail(@PathVariable Long userId,
                                              @RequestParam(defaultValue = "7") Integer days) {
        if (days == null || days <= 0) days = 7;
        if (days > 30) days = 30;

        User u = userService.getById(userId);
        if (u == null) {
            return Result.fail(404, "用户不存在");
        }
        u.setPassword(null);

        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(days - 1L);
        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endTime = today.atTime(LocalTime.MAX);

        Map<String, Object> row = buildUserRow(u, today, startTime, endTime, days);

        // 追加每日明细
        row.put("daily", dailyDetail(userId, start, today));

        return Result.success(row);
    }

    // ================= 内部方法 =================

    private Map<String, Object> buildUserRow(User u, LocalDate today,
                                             LocalDateTime startTime, LocalDateTime endTime, int days) {
        Long uid = u.getId();

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("userId", uid);
        row.put("username", u.getUsername());
        row.put("nickname", u.getNickname());
        row.put("weight", u.getWeight());
        row.put("targetWeight", u.getTargetWeight());

        // 计划
        FitnessPlan plan = fitnessPlanService.getActivePlan(uid);
        if (plan == null) {
            plan = fitnessPlanService.getLatestPlan(uid);
        }
        if (plan != null) {
            row.put("planName", plan.getPlanName());
            row.put("planType", plan.getPlanType());
            row.put("planStatus", plan.getStatus());
            row.put("targetCalorie", plan.getTargetCalorie());
            row.put("dailyDeficit", plan.getDailyDeficit());
            row.put("planStart", plan.getStartDate() == null ? null : plan.getStartDate().toString());
            row.put("planEnd", plan.getEndDate() == null ? null : plan.getEndDate().toString());
        } else {
            row.put("planName", null);
        }

        // 今日打卡
        boolean foodToday = foodRecordService.lambdaQuery()
                .eq(FoodRecord::getUserId, uid)
                .eq(FoodRecord::getRecordDate, today)
                .count() > 0;
        boolean exerciseToday = exerciseRecordService.lambdaQuery()
                .eq(ExerciseRecord::getUserId, uid)
                .eq(ExerciseRecord::getRecordDate, today)
                .count() > 0;
        boolean weightToday = weightRecordMapper.selectCount(
                new QueryWrapper<WeightRecord>()
                        .eq("user_id", uid)
                        .eq("record_date", today)) > 0;
        row.put("foodToday", foodToday);
        row.put("exerciseToday", exerciseToday);
        row.put("weightToday", weightToday);

        // 近 N 天有记录的天数（饮食或运动任一即算打卡）
        List<FoodRecord> foods = foodRecordService.lambdaQuery()
                .eq(FoodRecord::getUserId, uid)
                .ge(FoodRecord::getRecordDate, startTime.toLocalDate())
                .le(FoodRecord::getRecordDate, endTime.toLocalDate())
                .list();
        List<ExerciseRecord> exs = exerciseRecordService.lambdaQuery()
                .eq(ExerciseRecord::getUserId, uid)
                .ge(ExerciseRecord::getRecordDate, startTime.toLocalDate())
                .le(ExerciseRecord::getRecordDate, endTime.toLocalDate())
                .list();

        Set<LocalDate> recordDays = new HashSet<>();
        for (FoodRecord f : foods) {
            if (f.getRecordDate() != null) recordDays.add(f.getRecordDate());
        }
        for (ExerciseRecord e : exs) {
            if (e.getRecordDate() != null) recordDays.add(e.getRecordDate());
        }
        row.put("recordDays", recordDays.size());
        row.put("totalDays", days);

        // 执行率评分：打卡天数占比 60% + 今日三项打卡 40%
        int checkInScore = (int) Math.round(recordDays.size() * 100.0 / days * 0.6);
        int todayScore = ((foodToday ? 1 : 0) + (exerciseToday ? 1 : 0) + (weightToday ? 1 : 0)) * (40 / 3);
        int score = Math.min(100, checkInScore + todayScore);
        row.put("score", score);
        row.put("status", score >= 80 ? "优秀" : score >= 60 ? "良好" : score >= 30 ? "一般" : "懈怠");

        return row;
    }

    private List<Map<String, Object>> dailyDetail(Long userId, LocalDate start, LocalDate end) {
        List<FoodRecord> foods = foodRecordService.lambdaQuery()
                .eq(FoodRecord::getUserId, userId)
                .ge(FoodRecord::getRecordDate, start)
                .le(FoodRecord::getRecordDate, end)
                .list();
        List<ExerciseRecord> exs = exerciseRecordService.lambdaQuery()
                .eq(ExerciseRecord::getUserId, userId)
                .ge(ExerciseRecord::getRecordDate, start)
                .le(ExerciseRecord::getRecordDate, end)
                .list();

        Map<LocalDate, Integer> intakeMap = new HashMap<>();
        Map<LocalDate, Integer> burnMap = new HashMap<>();
        for (FoodRecord f : foods) {
            if (f.getRecordDate() != null && f.getCalorie() != null) {
                intakeMap.merge(f.getRecordDate(), f.getCalorie(), Integer::sum);
            }
        }
        for (ExerciseRecord e : exs) {
            if (e.getRecordDate() != null && e.getCalorie() != null) {
                burnMap.merge(e.getRecordDate(), e.getCalorie(), Integer::sum);
            }
        }

        List<Map<String, Object>> daily = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", d.toString());
            item.put("intake", intakeMap.getOrDefault(d, 0));
            item.put("burn", burnMap.getOrDefault(d, 0));
            item.put("hasRecord", intakeMap.containsKey(d) || burnMap.containsKey(d));
            daily.add(item);
        }
        return daily;
    }
}
