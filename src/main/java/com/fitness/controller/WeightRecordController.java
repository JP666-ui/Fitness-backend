package com.fitness.controller;

import com.fitness.common.Result;
import com.fitness.entity.FitnessPlan;
import com.fitness.entity.User;
import com.fitness.entity.WeightRecord;
import com.fitness.mapper.WeightRecordMapper;
import com.fitness.service.FitnessPlanService;
import com.fitness.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 每日体重记录 Controller
 * <p>
 * 支持每天记录一次体重，并查看变化趋势。
 * 记录体重时会同步更新用户当前体重，进而影响热量缺口计算。
 * <p>
 * 「距目标」与达标进度以<b>训练计划</b>为准：
 * 目标体重取自计划，基准体重取计划开始时的体重，
 * 这样即使中途修改目标也能正确反映进度。
 */
@RestController
@RequestMapping("/weight")
public class WeightRecordController {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private WeightRecordMapper weightRecordMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private FitnessPlanService fitnessPlanService;

    /**
     * 记录今日体重（每天一条，重复记录则覆盖）
     * POST /weight/add
     * <p>
     * 入参：{ weight: 84.5, recordDate: "2026-10-14"(可选), remark: "..." }
     */
    @PostMapping("/add")
    public Result<Map<String, Object>> add(HttpServletRequest request,
                                           @RequestBody Map<String, Object> params) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getById(userId);

        Object weightObj = params.get("weight");
        if (weightObj == null) {
            return Result.fail(400, "请填写体重");
        }
        double weight;
        try {
            weight = Double.parseDouble(String.valueOf(weightObj));
        } catch (NumberFormatException e) {
            return Result.fail(400, "体重格式不正确");
        }
        if (weight < 20 || weight > 300) {
            return Result.fail(400, "体重需在 20 ~ 300 kg 之间");
        }

        // 记录日期，默认今天
        LocalDate recordDate = LocalDate.now();
        Object dateObj = params.get("recordDate");
        if (dateObj != null && !String.valueOf(dateObj).isEmpty()) {
            try {
                recordDate = LocalDate.parse(String.valueOf(dateObj));
            } catch (Exception e) {
                return Result.fail(400, "日期格式不正确，应为 yyyy-MM-dd");
            }
        }
        if (recordDate.isAfter(LocalDate.now())) {
            return Result.fail(400, "不能记录未来日期的体重");
        }

        // 计算 BMI
        Double bmi = null;
        if (user != null && user.getHeight() != null && user.getHeight() > 0) {
            double h = user.getHeight() / 100.0;
            bmi = Math.round(weight / (h * h) * 10) / 10.0;
        }

        // 已存在则更新（每天一条）
        WeightRecord existing = weightRecordMapper.findByDate(userId, recordDate);
        boolean isUpdate = existing != null;

        WeightRecord record = isUpdate ? existing : new WeightRecord();
        record.setUserId(userId);
        record.setWeight(weight);
        record.setBmi(bmi);
        record.setRecordDate(recordDate);
        record.setRemark(params.get("remark") == null ? null : String.valueOf(params.get("remark")));

        if (isUpdate) {
            record.setUpdateTime(LocalDateTime.now());
            weightRecordMapper.updateById(record);
        } else {
            weightRecordMapper.insert(record);
        }

        // 同步更新用户当前体重（仅当记录日期是最新时）
        WeightRecord latest = weightRecordMapper.findLatest(userId);
        if (latest != null && latest.getRecordDate().equals(recordDate)) {
            user.setWeight(weight);
            user.setUpdateTime(LocalDateTime.now());
            userService.updateById(user);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("record", record);
        data.put("isUpdate", isUpdate);
        data.put("message", isUpdate ? "已更新今日体重" : "记录成功");

        return Result.success(data);
    }

    /**
     * 获取体重记录列表
     * GET /weight/list?days=30
     */
    @GetMapping("/list")
    public Result<List<WeightRecord>> list(HttpServletRequest request,
                                           @RequestParam(defaultValue = "30") Integer days) {
        Long userId = (Long) request.getAttribute("userId");
        if (days == null || days <= 0) {
            days = 30;
        }
        if (days > 365) {
            days = 365;
        }
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);
        return Result.success(weightRecordMapper.findByDateRange(userId, start, end));
    }

    /**
     * 体重变化统计
     * GET /weight/statistics?days=30
     * <p>
     * 「距目标」与达标进度以<b>训练计划</b>为准：
     * <ul>
     *     <li>目标体重取自当前计划的 targetWeight</li>
     *     <li>基准体重取计划开始时的体重（计划 startDate 当天或之前最近的记录）</li>
     *     <li>无计划时回退到用户设置的目标体重</li>
     * </ul>
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics(HttpServletRequest request,
                                                  @RequestParam(defaultValue = "30") Integer days) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getById(userId);

        if (days == null || days <= 0) {
            days = 30;
        }
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);

        // 取计划，用于确定目标体重与基准起点
        FitnessPlan plan = fitnessPlanService.getActivePlan(userId);
        if (plan == null) {
            plan = fitnessPlanService.getLatestPlan(userId);
        }

        List<WeightRecord> records = weightRecordMapper.findByDateRange(userId, start, end);

        Map<String, Object> result = new HashMap<>();
        result.put("days", days);
        result.put("records", records);
        result.put("recordCount", records.size());

        // 计划信息
        if (plan != null) {
            Map<String, Object> planInfo = new HashMap<>();
            planInfo.put("id", plan.getId());
            planInfo.put("planName", plan.getPlanName());
            planInfo.put("planType", plan.getPlanType());
            planInfo.put("startDate", plan.getStartDate() == null ? null : plan.getStartDate().format(DAY_FMT));
            planInfo.put("endDate", plan.getEndDate() == null ? null : plan.getEndDate().format(DAY_FMT));
            planInfo.put("targetWeight", plan.getTargetWeight());
            planInfo.put("dailyDeficit", plan.getDailyDeficit());
            planInfo.put("targetCalorie", plan.getTargetCalorie());
            result.put("plan", planInfo);
        }

        if (records.isEmpty()) {
            result.put("hasData", false);
            result.put("message", "还没有体重记录，从今天开始记录吧");
            return Result.success(result);
        }

        result.put("hasData", true);

        WeightRecord first = records.get(0);
        WeightRecord last = records.get(records.size() - 1);

        double currentWeight = last.getWeight();

        result.put("currentWeight", currentWeight);
        result.put("startDate", first.getRecordDate().format(DAY_FMT));
        result.put("lastDate", last.getRecordDate().format(DAY_FMT));
        result.put("bmi", last.getBmi());

        // ---------- 以计划为准的基准起点 ----------
        // 优先使用计划开始时的体重作为基准
        Double baselineWeight = null;
        String baselineSource = null;
        LocalDate planStartDate = plan == null ? null : plan.getStartDate();

        if (planStartDate != null) {
            // 计划开始当天或之前最近的一条记录
            WeightRecord baselineRecord = weightRecordMapper.findLatestBefore(userId, planStartDate);
            if (baselineRecord == null) {
                // 计划开始当天没有记录，退而取计划开始后的第一条
                baselineRecord = weightRecordMapper.findEarliestAfter(userId, planStartDate);
            }
            if (baselineRecord != null) {
                baselineWeight = baselineRecord.getWeight();
                baselineSource = baselineRecord.getRecordDate().format(DAY_FMT);
            }
        }

        // 计划内没有记录时，回退到统计区间首条
        if (baselineWeight == null) {
            baselineWeight = first.getWeight();
            baselineSource = first.getRecordDate().format(DAY_FMT);
        }

        result.put("startWeight", baselineWeight);
        result.put("baselineWeight", baselineWeight);
        result.put("baselineDate", baselineSource);
        // 区间内变化量（负数表示减重）
        result.put("change", Math.round((currentWeight - first.getWeight()) * 10) / 10.0);
        // 相对计划起点的变化量
        result.put("planChange", Math.round((currentWeight - baselineWeight) * 10) / 10.0);

        // ---------- 目标体重：优先取计划 ----------
        Double targetWeight = null;
        if (plan != null && plan.getTargetWeight() != null) {
            targetWeight = plan.getTargetWeight();
            result.put("targetSource", "plan");
        } else if (user != null && user.getTargetWeight() != null) {
            targetWeight = user.getTargetWeight();
            result.put("targetSource", "user");
        }

        if (targetWeight != null) {
            double toGo = Math.round((currentWeight - targetWeight) * 10) / 10.0;
            result.put("targetWeight", targetWeight);
            result.put("remaining", toGo);

            // 达标进度：以计划起点的体重为基准
            double totalNeed = baselineWeight - targetWeight;
            if (totalNeed > 0.1) {
                double done = baselineWeight - currentWeight;
                int progress = (int) Math.round(done * 100 / totalNeed);
                result.put("progress", Math.max(0, Math.min(100, progress)));
                // 还需减多少
                result.put("doneAmount", Math.round(done * 10) / 10.0);
                result.put("totalNeed", Math.round(totalNeed * 10) / 10.0);
            } else {
                result.put("progress", currentWeight <= targetWeight ? 100 : 0);
                result.put("doneAmount", 0);
                result.put("totalNeed", 0);
            }
            result.put("reached", currentWeight <= targetWeight);
        }

        // 最近一次变化（与上一条对比）
        if (records.size() >= 2) {
            WeightRecord prev = records.get(records.size() - 2);
            double lastChange = Math.round((last.getWeight() - prev.getWeight()) * 10) / 10.0;
            result.put("lastChange", lastChange);
        }

        return Result.success(result);
    }

    /**
     * 删除某条体重记录
     * DELETE /weight/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        WeightRecord record = weightRecordMapper.selectById(id);
        if (record == null || !record.getUserId().equals(userId)) {
            return Result.fail(404, "记录不存在");
        }
        return Result.success(weightRecordMapper.deleteById(id) > 0);
    }

    /**
     * 查询今日是否已记录
     * GET /weight/today
     */
    @GetMapping("/today")
    public Result<Map<String, Object>> today(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        WeightRecord record = weightRecordMapper.findByDate(userId, LocalDate.now());

        Map<String, Object> data = new HashMap<>();
        data.put("recorded", record != null);
        data.put("record", record);
        return Result.success(data);
    }
}