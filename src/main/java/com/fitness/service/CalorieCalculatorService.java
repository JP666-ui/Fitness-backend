package com.fitness.service;

import com.fitness.entity.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 热量计算服务
 * <p>
 * 基于 Mifflin-St Jeor 公式计算基础代谢率（BMR），
 * 结合活动系数得到每日总消耗（TDEE），
 * 再根据用户目标体重与时间推算科学的每日热量缺口。
 */
@Service
public class CalorieCalculatorService {

    /** 每公斤脂肪约含 7700 kcal */
    public static final int KCAL_PER_KG_FAT = 7700;

    /** 安全减脂速度：每周 0.5kg（每日缺口 550 kcal） */
    private static final int SAFE_WEEKLY_LOSS_GRAMS = 500;

    /** 每日热量缺口上限，避免过度节食 */
    private static final int MAX_DAILY_DEFICIT = 1000;

    /** 每日热量缺口下限，保证基础营养 */
    private static final int MIN_DAILY_DEFICIT = 200;

    /**
     * 计算基础代谢率 BMR（Mifflin-St Jeor 公式）
     * <p>
     * 男：10×体重 + 6.25×身高 − 5×年龄 + 5
     * 女：10×体重 + 6.25×身高 − 5×年龄 − 161
     *
     * @return BMR(kcal)；资料不全返回 0
     */
    public int calculateBmr(User user) {
        if (user == null || user.getWeight() == null
                || user.getHeight() == null || user.getAge() == null) {
            return 0;
        }
        double bmr = 10 * user.getWeight() + 6.25 * user.getHeight() - 5 * user.getAge();
        // gender: 0-未知 1-男 2-女
        if (user.getGender() != null && user.getGender() == 2) {
            bmr -= 161;
        } else {
            bmr += 5;
        }
        return (int) Math.round(bmr);
    }

    /**
     * 计算每日总消耗 TDEE = BMR × 活动系数
     * <p>
     * 活动系数默认取 1.375（轻度活动：每周运动 3-5 次）
     */
    public int calculateTdee(User user) {
        return calculateTdee(user, 1.375);
    }

    /**
     * 计算每日总消耗 TDEE
     *
     * @param activityFactor 活动系数
     *                       1.2   久坐
     *                       1.375 轻度活动
     *                       1.55  中度活动
     *                       1.725 高度活动
     */
    public int calculateTdee(User user, double activityFactor) {
        int bmr = calculateBmr(user);
        if (bmr == 0) {
            return 0;
        }
        return (int) Math.round(bmr * activityFactor);
    }

    /**
     * 根据目标体重与计划周期，计算科学的每日热量缺口
     * <p>
     * 核心公式：
     * <pre>
     * 需减体重(kg) = 当前体重 − 目标体重
     * 需减总热量(kcal) = 需减体重 × 7700     （1kg 脂肪 ≈ 7700 kcal）
     * 每日热量缺口 = 需减总热量 ÷ 计划天数
     * </pre>
     * 结果限制在安全区间内（200 ~ 1000 kcal），避免过度节食。
     *
     * @param user         用户（需有体重）
     * @param targetWeight 目标体重(kg)
     * @param days         计划天数
     * @return 每日热量缺口(kcal)
     */
    public int calculateDailyDeficit(User user, Double targetWeight, long days) {
        if (user == null || user.getWeight() == null
                || targetWeight == null || days <= 0) {
            return 0;
        }

        double weightToLose = user.getWeight() - targetWeight;
        if (weightToLose <= 0) {
            // 目标体重不低于当前体重，无需缺口
            return 0;
        }

        double totalDeficit = weightToLose * KCAL_PER_KG_FAT;
        int dailyDeficit = (int) Math.round(totalDeficit / days);

        // 限制在安全范围内
        if (dailyDeficit > MAX_DAILY_DEFICIT) {
            return MAX_DAILY_DEFICIT;
        }
        if (dailyDeficit < MIN_DAILY_DEFICIT) {
            return MIN_DAILY_DEFICIT;
        }
        return dailyDeficit;
    }

    /**
     * 根据用户设定的每日热量缺口，反推达成目标体重所需天数
     * <p>
     * 公式：天数 = 需减体重 × 7700 ÷ 每日缺口
     *
     * @param weightToLose  需减体重(kg)
     * @param dailyDeficit  每日缺口(kcal)
     * @return 所需天数；参数无效返回 0
     */
    public long daysByDeficit(double weightToLose, int dailyDeficit) {
        if (weightToLose <= 0 || dailyDeficit <= 0) {
            return 0;
        }
        return (long) Math.ceil(weightToLose * KCAL_PER_KG_FAT / dailyDeficit);
    }

    /**
     * 计算每周减重速度(kg)
     * <p>
     * 公式：每日缺口 × 7 ÷ 7700
     */
    public double weeklyLossByDeficit(int dailyDeficit) {
        if (dailyDeficit <= 0) {
            return 0;
        }
        return Math.round(dailyDeficit * 7.0 / KCAL_PER_KG_FAT * 100) / 100.0;
    }

    /**
     * 评估减重速度是否健康
     *
     * @return {level, text}，level 取值 healthy / aggressive / gentle
     */
    public Map<String, String> evaluateSafety(double weeklyLoss) {
        Map<String, String> result = new HashMap<>();
        if (weeklyLoss > 1.0) {
            result.put("level", "aggressive");
            result.put("text", String.format(
                    "每周减重 %.2fkg 偏快，建议适当延长周期或减小缺口，以免流失肌肉", weeklyLoss));
        } else if (weeklyLoss >= 0.4) {
            result.put("level", "healthy");
            result.put("text", String.format(
                    "每周减重 %.2fkg 属于健康范围，推荐保持这个节奏", weeklyLoss));
        } else if (weeklyLoss > 0) {
            result.put("level", "gentle");
            result.put("text", String.format(
                    "每周减重 %.2fkg 节奏较温和，达成目标需要较长时间", weeklyLoss));
        } else {
            result.put("level", "healthy");
            result.put("text", "保持均衡饮食与规律运动即可");
        }
        return result;
    }

    /**
     * 按健康速度推算达成目标所需天数（每周减 0.5kg）
     *
     * @return 建议天数；无需减重返回 0
     */
    public long suggestDays(User user, Double targetWeight) {
        if (user == null || user.getWeight() == null || targetWeight == null) {
            return 0;
        }
        double weightToLose = user.getWeight() - targetWeight;
        if (weightToLose <= 0) {
            return 0;
        }
        // 每周减 500g → 每天约 71.4g；weightToLose 是 kg，需换算成 g
        double dailyLossGrams = SAFE_WEEKLY_LOSS_GRAMS / 7.0;
        return (long) Math.ceil(weightToLose * 1000 / dailyLossGrams);
    }

    /**
     * 生成完整的热量分析报告
     * <p>
     * 这是给前端「热量缺口可视化」用的核心数据结构。
     * 减脂场景下会完整给出推导过程：需减体重 → 总热量 → 每日缺口 → 建议摄入。
     *
     * @param user         用户
     * @param targetWeight 目标体重，可为 null
     * @param endDate      计划结束日期，可为 null
     * @return 分析结果
     */
    public Map<String, Object> buildCalorieReport(User user, Double targetWeight, LocalDate endDate) {
        Map<String, Object> report = new HashMap<>();

        int bmr = calculateBmr(user);
        int tdee = calculateTdee(user);

        report.put("bmr", bmr);
        report.put("tdee", tdee);
        report.put("activityFactor", 1.375);
        report.put("activityLevel", "轻度活动（每周运动 3-5 次）");
        report.put("kcalPerKgFat", KCAL_PER_KG_FAT);

        // 资料是否完整
        boolean profileComplete = bmr > 0;
        report.put("profileComplete", profileComplete);

        if (!profileComplete) {
            report.put("message", "请先在「个人中心」补全身高、体重、年龄信息，才能精确计算热量缺口");
            return report;
        }

        double currentWeight = user.getWeight();
        report.put("currentWeight", currentWeight);
        report.put("targetWeight", targetWeight);

        if (targetWeight == null) {
            // 未设目标体重，只给出维持建议
            report.put("goalType", "maintain");
            report.put("dailyDeficit", 0);
            report.put("suggestedIntake", tdee);
            report.put("message", "设置目标体重后，系统会自动为你计算科学的每日热量缺口");
            return report;
        }

        // 计算天数
        long days;
        if (endDate != null && endDate.isAfter(LocalDate.now())) {
            days = ChronoUnit.DAYS.between(LocalDate.now(), endDate);
        } else {
            days = suggestDays(user, targetWeight);
        }
        if (days <= 0) {
            days = 30;
        }
        report.put("planDays", days);

        double weightDiff = currentWeight - targetWeight;

        if (weightDiff > 0.5) {
            // ==================== 减脂：完整推导 ====================
            int deficit = calculateDailyDeficit(user, targetWeight, days);
            int suggestedIntake = tdee - deficit;

            // 保证不低于基础代谢
            boolean bmrProtected = false;
            if (suggestedIntake < bmr) {
                suggestedIntake = bmr;
                deficit = tdee - bmr;
                bmrProtected = true;
            }

            // 关键数据
            double weightToLose = Math.round(weightDiff * 10) / 10.0;
            long totalDeficit = Math.round(weightToLose * KCAL_PER_KG_FAT);
            double weeklyLoss = weeklyLossByDeficit(deficit);

            report.put("goalType", "loss");
            report.put("weightToLose", weightToLose);
            report.put("totalDeficit", totalDeficit);
            report.put("dailyDeficit", deficit);
            report.put("suggestedIntake", suggestedIntake);
            report.put("bmrProtected", bmrProtected);
            report.put("weeklyLoss", weeklyLoss);

            // ---- 推导过程（供前端逐步展示）----
            List<Map<String, Object>> steps = new ArrayList<>();

            Map<String, Object> s1 = new LinkedHashMap<>();
            s1.put("step", 1);
            s1.put("title", "需减体重");
            s1.put("formula", String.format("%.1f − %.1f", currentWeight, targetWeight));
            s1.put("result", weightToLose + " kg");
            s1.put("desc", "当前体重 − 目标体重");
            steps.add(s1);

            Map<String, Object> s2 = new LinkedHashMap<>();
            s2.put("step", 2);
            s2.put("title", "需减总热量");
            s2.put("formula", String.format("%.1f kg × %d", weightToLose, KCAL_PER_KG_FAT));
            s2.put("result", totalDeficit + " kcal");
            s2.put("desc", "1kg 脂肪约等于 7700 kcal");
            steps.add(s2);

            Map<String, Object> s3 = new LinkedHashMap<>();
            s3.put("step", 3);
            s3.put("title", "每日热量缺口");
            s3.put("formula", String.format("%d ÷ %d 天", totalDeficit, days));
            s3.put("result", deficit + " kcal/天");
            s3.put("desc", "需减总热量 ÷ 计划天数");
            steps.add(s3);

            Map<String, Object> s4 = new LinkedHashMap<>();
            s4.put("step", 4);
            s4.put("title", "建议每日摄入");
            s4.put("formula", String.format("%d − %d", tdee, deficit));
            s4.put("result", suggestedIntake + " kcal/天");
            s4.put("desc", "每日总消耗 − 每日缺口"
                    + (bmrProtected ? "（已保护，不低于基础代谢）" : ""));
            steps.add(s4);

            report.put("steps", steps);

            // 预计结果
            double expectedLoss = (double) deficit * days / KCAL_PER_KG_FAT;
            report.put("expectedLoss", Math.round(expectedLoss * 10) / 10.0);
            report.put("expectedWeight", Math.round((currentWeight - expectedLoss) * 10) / 10.0);

            long realDays = daysByDeficit(weightToLose, deficit);
            report.put("estimatedDays", realDays);
            report.put("estimatedDate", LocalDate.now().plusDays(realDays).toString());

            // 安全性评估
            Map<String, String> safety = evaluateSafety(weeklyLoss);
            report.put("safety", safety.get("level"));
            report.put("safetyText", safety.get("text"));

            // 可选的缺口方案（让用户直观对比不同节奏）
            report.put("alternatives", buildAlternatives(weightToLose, tdee, bmr));

        } else if (weightDiff < -0.5) {
            // ---------- 增肌 ----------
            int surplus = 300;  // 增肌建议小幅盈余
            report.put("goalType", "gain");
            report.put("weightToGain", Math.round(-weightDiff * 10) / 10.0);
            report.put("dailySurplus", surplus);
            report.put("dailyDeficit", -surplus);
            report.put("suggestedIntake", tdee + surplus);
            report.put("safety", "healthy");
            report.put("safetyText", "增肌期建议小幅热量盈余，配合力量训练效果更佳");

        } else {
            // ---------- 维持 ----------
            report.put("goalType", "maintain");
            report.put("dailyDeficit", 0);
            report.put("suggestedIntake", tdee);
            report.put("safety", "healthy");
            report.put("safetyText", "当前体重接近目标，保持均衡饮食与规律运动即可");
        }

        return report;
    }

    /**
     * 生成不同减重节奏下的缺口方案，供用户对比选择
     * <p>
     * 给出三种典型速度：温和（0.25kg/周）、标准（0.5kg/周）、快速（0.75kg/周）。
     * 若某方案的摄入低于基础代谢，会做保护处理，并按「保护后的实际缺口」
     * 重新计算每周减重与所需天数，保证展示数据与真实情况一致。
     */
    private List<Map<String, Object>> buildAlternatives(double weightToLose, int tdee, int bmr) {
        List<Map<String, Object>> list = new ArrayList<>();

        double[] weeklyTargets = {0.25, 0.5, 0.75};
        String[] names = {"温和", "标准", "快速"};

        for (int i = 0; i < weeklyTargets.length; i++) {
            double targetWeekly = weeklyTargets[i];
            int deficit = (int) Math.round(targetWeekly * KCAL_PER_KG_FAT / 7);
            int intake = tdee - deficit;

            // 不低于基础代谢
            boolean bmrProtected = false;
            if (bmr > 0 && intake < bmr) {
                intake = bmr;
                deficit = tdee - bmr;
                bmrProtected = true;
            }

            // 用「实际缺口」反算真实速度与天数
            double actualWeekly = weeklyLossByDeficit(deficit);
            long days = daysByDeficit(weightToLose, deficit);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", names[i]);
            item.put("targetWeeklyLoss", targetWeekly);
            item.put("weeklyLoss", actualWeekly);
            item.put("dailyDeficit", deficit);
            item.put("suggestedIntake", intake);
            item.put("days", days);
            item.put("recommended", i == 1);  // 标准方案为推荐
            item.put("bmrProtected", bmrProtected);
            list.add(item);
        }

        return list;
    }

    /**
     * 活动系数对应的说明
     */
    public String activityText(double factor) {
        if (factor <= 1.2) {
            return "久坐（几乎不运动）";
        }
        if (factor <= 1.375) {
            return "轻度活动（每周运动 3-5 次）";
        }
        if (factor <= 1.55) {
            return "中度活动（每周运动 6-7 次）";
        }
        return "高度活动（体力工作或每日高强度训练）";
    }
}