package com.fitness.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.alibaba.fastjson2.JSON;
import com.fitness.common.Result;
import com.fitness.entity.ExerciseRecord;
import com.fitness.entity.FoodRecord;
import com.fitness.entity.HealthAssessment;
import com.fitness.entity.User;
import com.fitness.mapper.HealthAssessmentMapper;
import com.fitness.service.ExerciseRecordService;
import com.fitness.service.FoodRecordService;
import com.fitness.service.HealthAssessmentService;
import com.fitness.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * 健康评测 Controller
 * <p>
 * 系统核心能力之一：不依赖硬件，纯算法给出体成分与健康评测。
 */
@RestController
@RequestMapping("/health")
public class HealthAssessController {

    @Autowired
    private HealthAssessmentService assessService;

    @Autowired
    private HealthAssessmentMapper assessMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private FoodRecordService foodRecordService;

    @Autowired
    private ExerciseRecordService exerciseRecordService;

    /**
     * 执行健康评测（实时计算，不落库）
     * GET /health/assess
     * <p>
     * 返回体成分、代谢指标、健康评分、营养评分与建议。
     */
    @GetMapping("/assess")
    public Result<Map<String, Object>> assess(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getById(userId);
        if (user == null) return Result.fail(404, "用户不存在");

        // 基础数据校验
        if (user.getHeight() == null || user.getWeight() == null || user.getAge() == null) {
            return Result.fail(400, "请先在个人中心完善身高、体重、年龄后再评测");
        }

        int gender = user.getGender() == null ? 0 : user.getGender();
        int age = user.getAge();
        double height = user.getHeight();
        double weight = user.getWeight();

        Map<String, Object> data = computeAssessment(user, gender, age, height, weight, 30);

        // 建议解析成对象数组
        if (data.get("advice") instanceof String) {
            data.put("advice", JSON.parse((String) data.get("advice")));
        }

        return Result.success(data);
    }

    /**
     * 执行评测并保存
     * POST /health/assess/save
     */
    @PostMapping("/assess/save")
    public Result<HealthAssessment> save(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getById(userId);
        if (user == null) return Result.fail(404, "用户不存在");
        if (user.getHeight() == null || user.getWeight() == null || user.getAge() == null) {
            return Result.fail(400, "请先完善身高、体重、年龄");
        }

        int gender = user.getGender() == null ? 0 : user.getGender();
        Map<String, Object> data = computeAssessment(
                user, gender, user.getAge(), user.getHeight(), user.getWeight(), 30);

        LocalDate today = LocalDate.now();
        HealthAssessment exist = assessMapper.selectOne(
                new QueryWrapper<HealthAssessment>()
                        .eq("user_id", userId)
                        .eq("assess_date", today));

        HealthAssessment entity = new HealthAssessment();
        entity.setUserId(userId);
        entity.setAssessDate(today);
        entity.setBmi((Double) data.get("bmi"));
        entity.setBodyFatRate((Double) data.get("bodyFatRate"));
        entity.setMuscleMass((Double) data.get("muscleMass"));
        entity.setWaterRate((Double) data.get("waterRate"));
        entity.setFatMass((Double) data.get("fatMass"));
        entity.setLeanMass((Double) data.get("leanMass"));
        entity.setBmr((Integer) data.get("bmr"));
        entity.setMetabolicAge((Integer) data.get("metabolicAge"));
        entity.setVisceralFat((Integer) data.get("visceralFat"));
        entity.setHealthScore((Integer) data.get("healthScore"));
        entity.setHealthLevel((String) data.get("healthLevel"));
        entity.setNutritionScore((Integer) data.get("nutritionScore"));
        entity.setAdvice((String) data.get("advice"));

        if (exist != null) {
            // 当天已有记录则覆盖
            entity.setId(exist.getId());
            assessMapper.updateById(entity);
        } else {
            assessMapper.insert(entity);
        }
        return Result.success(entity);
    }

    /**
     * 评测历史
     * GET /health/history?days=30
     */
    @GetMapping("/history")
    public Result<List<HealthAssessment>> history(HttpServletRequest request,
                                                  @RequestParam(defaultValue = "30") Integer days) {
        Long userId = (Long) request.getAttribute("userId");
        if (days == null || days <= 0) days = 30;
        if (days > 365) days = 365;

        LocalDate start = LocalDate.now().minusDays(days - 1L);
        List<HealthAssessment> list = assessMapper.selectList(
                new QueryWrapper<HealthAssessment>()
                        .eq("user_id", userId)
                        .ge("assess_date", start)
                        .orderByAsc("assess_date"));
        return Result.success(list);
    }

    /**
     * 评测趋势（用于图表）
     * GET /health/trend?days=30
     */
    @GetMapping("/trend")
    public Result<Map<String, Object>> trend(HttpServletRequest request,
                                             @RequestParam(defaultValue = "30") Integer days) {
        Long userId = (Long) request.getAttribute("userId");
        if (days == null || days <= 0) days = 30;
        if (days > 365) days = 365;

        LocalDate start = LocalDate.now().minusDays(days - 1L);
        List<HealthAssessment> list = assessMapper.selectList(
                new QueryWrapper<HealthAssessment>()
                        .eq("user_id", userId)
                        .ge("assess_date", start)
                        .orderByAsc("assess_date"));

        List<Map<String, Object>> points = new ArrayList<>();
        for (HealthAssessment a : list) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("date", a.getAssessDate().toString());
            p.put("bmi", a.getBmi());
            p.put("bodyFatRate", a.getBodyFatRate());
            p.put("healthScore", a.getHealthScore());
            p.put("muscleMass", a.getMuscleMass());
            points.add(p);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("days", days);
        data.put("count", points.size());

        // 变化幅度：首末对比
        if (points.size() >= 2) {
            Map<String, Object> first = points.get(0);
            Map<String, Object> last = points.get(points.size() - 1);
            Map<String, Object> change = new LinkedHashMap<>();
            change.put("bmiDelta", delta(first.get("bmi"), last.get("bmi")));
            change.put("bodyFatDelta", delta(first.get("bodyFatRate"), last.get("bodyFatRate")));
            change.put("scoreDelta", delta(first.get("healthScore"), last.get("healthScore")));
            data.put("change", change);
        }
        data.put("points", points);
        return Result.success(data);
    }

    private double delta(Object a, Object b) {
        if (a == null || b == null) return 0;
        double x = ((Number) a).doubleValue();
        double y = ((Number) b).doubleValue();
        return Math.round((y - x) * 10.0) / 10.0;
    }

    /**
     * 核心计算：组装完整评测结果
     */
    private Map<String, Object> computeAssessment(User user, int gender, int age,
                                                  double height, double weight, int days) {
        // 体成分
        double bmi = assessService.bmi(height, weight);
        double bodyFat = assessService.bodyFatRate(bmi, age, gender);
        double leanMass = assessService.leanMass(weight, bodyFat);
        double fatMass = assessService.fatMass(weight, bodyFat);
        double muscleMass = assessService.muscleMass(leanMass);
        double waterRate = assessService.waterRate(leanMass, weight);
        double muscleRatio = weight > 0 ? Math.round(muscleMass / weight * 1000) / 10.0 : 0;

        // 代谢指标
        int bmr = Math.round(assessService.getBmr(user));
        int metabolicAge = assessService.metabolicAge(age, bmr, height, gender);
        int visceralFat = assessService.visceralFat(bmi, bodyFat, gender);

        // 健康评分
        int score = assessService.healthScore(bmi, bodyFat, visceralFat, muscleMass, weight, gender, age);
        String level = assessService.healthLevel(score);

        // 营养评分（基于近 N 天饮食记录）
        int target = user.getDailyCalorieGoal() == null ? 2000 : user.getDailyCalorieGoal();
        LocalDate start = LocalDate.now().minusDays(days - 1L);
        List<FoodRecord> foods = foodRecordService.lambdaQuery()
                .eq(FoodRecord::getUserId, user.getId())
                .ge(FoodRecord::getRecordDate, start)
                .list();

        Map<LocalDate, Integer> intakeMap = new HashMap<>();
        for (FoodRecord f : foods) {
            if (f.getRecordDate() != null && f.getCalorie() != null) {
                intakeMap.merge(f.getRecordDate(), f.getCalorie(), Integer::sum);
            }
        }
        List<Integer> intakes = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            intakes.add(intakeMap.getOrDefault(d, 0));
        }
        int nutritionScore = assessService.nutritionScore(intakes, target, intakeMap.size());

        // 建议
        List<Map<String, Object>> advice = assessService.buildAdvice(
                bmi, bodyFat, visceralFat, muscleRatio, gender, age);
        String adviceJson = JSON.toJSONString(advice);

        // 组装返回
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> basic = new LinkedHashMap<>();
        basic.put("height", height);
        basic.put("weight", weight);
        basic.put("age", age);
        basic.put("gender", gender == 1 ? "男" : gender == 2 ? "女" : "未设置");
        basic.put("targetWeight", user.getTargetWeight());
        data.put("basic", basic);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("bmi", bmi);
        body.put("bmiLevel", bmiLevel(bmi));
        body.put("bodyFatRate", bodyFat);
        body.put("bodyFatLevel", bodyFatLevel(bodyFat, gender));
        body.put("fatMass", fatMass);
        body.put("leanMass", leanMass);
        body.put("muscleMass", muscleMass);
        body.put("muscleRatio", muscleRatio);
        body.put("waterRate", waterRate);
        data.put("body", body);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("bmr", bmr);
        meta.put("metabolicAge", metabolicAge);
        meta.put("ageDiff", metabolicAge - age);
        meta.put("visceralFat", visceralFat);
        meta.put("visceralLevel", visceralLevel(visceralFat));
        data.put("metabolic", meta);

        Map<String, Object> evaluation = new LinkedHashMap<>();
        evaluation.put("healthScore", score);
        evaluation.put("healthLevel", level);
        evaluation.put("nutritionScore", nutritionScore);
        // 无饮食记录时营养分无意义，前端据此显示「暂无数据」
        evaluation.put("hasNutritionData", intakeMap.size() > 0);
        evaluation.put("nutritionDays", intakeMap.size());
        evaluation.put("nutritionLevel", intakeMap.size() > 0
                ? assessService.healthLevel(nutritionScore) : "暂无数据");
        // 各维度分数，用于雷达图
        evaluation.put("dimensions", dimensions(bmi, bodyFat, visceralFat, muscleRatio, gender));
        data.put("evaluation", evaluation);

        data.put("advice", adviceJson);

        // 供 save 落库使用的扁平字段
        data.put("bmi", bmi);
        data.put("bodyFatRate", bodyFat);
        data.put("muscleMass", muscleMass);
        data.put("waterRate", waterRate);
        data.put("fatMass", fatMass);
        data.put("leanMass", leanMass);
        data.put("bmr", bmr);
        data.put("metabolicAge", metabolicAge);
        data.put("visceralFat", visceralFat);
        data.put("healthScore", score);
        data.put("healthLevel", level);
        data.put("nutritionScore", nutritionScore);
        return data;
    }

    private Map<String, Object> dimensions(double bmi, double bodyFat, int visceralFat,
                                           double muscleRatio, int gender) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("体重指数", bmiScore(bmi));
        d.put("体脂水平", bfScore(bodyFat, gender));
        d.put("内脏脂肪", vfScore(visceralFat));
        d.put("肌肉量", muscleScore(muscleRatio, gender));
        d.put("代谢活力", 0);   // 占位，由前端按 bmr 与代谢年龄补齐
        return d;
    }

    private int bmiScore(double bmi) {
        if (bmi <= 0) return 0;
        if (bmi < 18.5) return Math.max(40, (int) (100 - (18.5 - bmi) * 25));
        if (bmi <= 23.9) return 100;
        if (bmi <= 27.9) return (int) Math.max(20, 100 - (bmi - 23.9) * 10);
        return (int) Math.max(20, 60 - (bmi - 27.9) * 15);
    }

    private int bfScore(double bf, int gender) {
        double lo = gender == 1 ? 10 : 18;
        double hi = gender == 1 ? 20 : 28;
        if (bf <= 0) return 0;
        if (bf < lo) return (int) Math.max(50, 100 - (lo - bf) * 5);
        if (bf <= hi) return 100;
        if (bf <= hi + 10) return (int) (100 - (bf - hi) * 6);
        return (int) Math.max(20, 40 - (bf - hi - 10) * 3);
    }

    private int vfScore(int vf) {
        if (vf <= 0) return 0;
        if (vf <= 9) return 100;
        if (vf <= 14) return 100 - (vf - 9) * 12;
        return Math.max(20, 40 - (vf - 14) * 12);
    }

    private int muscleScore(double ratio, int gender) {
        double lo = gender == 1 ? 28 : 22;
        double hi = gender == 1 ? 40 : 34;
        if (ratio <= 0) return 0;
        if (ratio < lo) return (int) Math.max(40, 100 - (lo - ratio) * 6);
        if (ratio <= hi) return 100;
        return 95;
    }

    private String bmiLevel(double bmi) {
        if (bmi <= 0) return "未知";
        if (bmi < 18.5) return "偏瘦";
        if (bmi <= 23.9) return "正常";
        if (bmi <= 27.9) return "超重";
        return "肥胖";
    }

    private String bodyFatLevel(double bf, int gender) {
        double lo = gender == 1 ? 10 : 18;
        double hi = gender == 1 ? 20 : 28;
        if (bf <= 0) return "未知";
        if (bf < lo) return "偏低";
        if (bf <= hi) return "标准";
        if (bf <= hi + 10) return "偏高";
        return "过高";
    }

    private String visceralLevel(int vf) {
        if (vf <= 0) return "未知";
        if (vf <= 9) return "标准";
        if (vf <= 14) return "偏高";
        return "过高";
    }
}
