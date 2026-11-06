package com.fitness.service;

import com.fitness.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 健康评测服务
 * <p>
 * 基于用户基础数据（身高/体重/年龄/性别）做多维度健康评测，
 * 不依赖体脂秤等硬件，用经过验证的公式做估算。
 * <p>
 * 涉及公式：
 * <ul>
 *     <li>BMI = 体重 / 身高²</li>
 *     <li>体脂率：Deurenberg 公式（亚洲人系数修正）</li>
 *     <li>去脂体重 = 体重 × (1 - 体脂率)</li>
 *     <li>肌肉量 = 去脂体重 × 0.53（骨骼肌约占去脂体重的一半）</li>
 *     <li>体水分率 = 去脂体重 × 0.73 / 体重</li>
 *     <li>内脏脂肪等级：由 BMI 与腰围相关经验公式推导</li>
 *     <li>基础代谢：Mifflin-St Jeor</li>
 *     <li>代谢年龄：由 BMR 与同龄标准 BMR 比较反推</li>
 * </ul>
 */
@Service
public class HealthAssessmentService {

    @Autowired
    private CalorieCalculatorService calorieCalculator;

    /**
     * 取用户基础代谢（委托给已有的 CalorieCalculatorService，避免重复公式）
     */
    public int getBmr(User user) {
        return calorieCalculator.calculateBmr(user);
    }

    /** 亚洲人群 BMI 分级下限（中国标准，非 WHO 国际标准） */
    private static final double BMI_UNDERWEIGHT = 18.5;
    private static final double BMI_NORMAL_UPPER = 23.9;
    private static final double BMI_OVERWEIGHT_UPPER = 27.9;

    /**
     * 计算 BMI
     */
    public double bmi(double heightCm, double weightKg) {
        if (heightCm <= 0 || weightKg <= 0) return 0;
        double h = heightCm / 100.0;
        return round1(weightKg / (h * h));
    }

    /**
     * 估算体脂率（Deurenberg 公式）
     * <p>
     * 原始公式：体脂率% = 1.2×BMI + 0.23×年龄 − 10.8×性别(男1女0) − 5.4
     * <p>
     * 该公式基于西方人群。亚洲人群在相同 BMI 下体脂率普遍偏高，
     * 因此对亚洲人加约 3 个百分点修正。
     */
    public double bodyFatRate(double bmi, int age, int gender) {
        if (bmi <= 0 || age <= 0) return 0;
        int maleFlag = gender == 1 ? 1 : 0;
        double bf = 1.2 * bmi + 0.23 * age - 10.8 * maleFlag - 5.4;
        // 亚洲人群修正
        bf += 3.0;
        // 生理合理区间约束
        if (bf < 3) bf = 3;
        if (bf > 60) bf = 60;
        return round1(bf);
    }

    /**
     * 计算去脂体重（kg）
     */
    public double leanMass(double weight, double bodyFatRate) {
        if (weight <= 0) return 0;
        return round1(weight * (1 - bodyFatRate / 100.0));
    }

    /**
     * 估算肌肉量（kg）
     * <p>
     * 骨骼肌大约占去脂体重的 50%-55%，取 0.53 作为经验系数。
     */
    public double muscleMass(double leanMass) {
        if (leanMass <= 0) return 0;
        return round1(leanMass * 0.53);
    }

    /**
     * 计算脂肪量（kg）
     */
    public double fatMass(double weight, double bodyFatRate) {
        if (weight <= 0) return 0;
        return round1(weight * bodyFatRate / 100.0);
    }

    /**
     * 估算体水分率（%）
     * <p>
     * 去脂体重中约 73% 是水分，故：水分率 = 去脂体重 × 0.73 / 总体重
     */
    public double waterRate(double leanMass, double weight) {
        if (weight <= 0) return 0;
        return round1(leanMass * 0.73 / weight * 100.0);
    }

    /**
     * 估算内脏脂肪等级（1-20）
     * <p>
     * 无腰围数据时的近似：以 BMI 为主、体脂率为辅。
     * BMI 正常者约 3-6 级，肥胖者可到 10 级以上。
     */
    public int visceralFat(double bmi, double bodyFatRate, int gender) {
        if (bmi <= 0) return 0;
        double base = (bmi - 18.5) * 0.95 + bodyFatRate * 0.10;
        if (gender == 1) base -= 1.0;   // 男性内脏脂肪倾向略低
        int level = (int) Math.round(base + 4);
        if (level < 1) level = 1;
        if (level > 20) level = 20;
        return level;
    }

    /**
     * 估算代谢年龄
     * <p>
     * 思路：以该性别/身高下"标准体重"的 BMR 作为基准，
     * 实际 BMR 高于基准则代谢年龄偏年轻，低于则偏老。
     * 每 1% 的 BMR 差异对应约 0.5 岁。
     */
    public int metabolicAge(int actualAge, int bmr, double heightCm, int gender) {
        if (actualAge <= 0 || bmr <= 0 || heightCm <= 0) return actualAge;

        // 该身高对应的标准体重（BMI 取 22 为理想值）
        double idealWeight = 22 * Math.pow(heightCm / 100.0, 2);

        // 用理想体重、相同年龄估算标准 BMR
        int maleFlag = gender == 1 ? 1 : 0;
        double standardBmr = 10 * idealWeight + 6.25 * heightCm - 5 * actualAge
                + (gender == 1 ? 5 : -161);

        if (standardBmr <= 0) return actualAge;

        double ratio = (bmr - standardBmr) / standardBmr;
        int delta = (int) Math.round(ratio * 0.5 * actualAge);
        int result = actualAge - delta;

        if (result < 16) result = 16;
        if (result > 80) result = 80;
        return result;
    }

    /**
     * 综合健康评分（0-100）
     * <p>
     * 四个维度加权：
     * <ul>
     *     <li>BMI 合理性 35%</li>
     *     <li>体脂率合理性 30%</li>
     *     <li>内脏脂肪 20%</li>
     *     <li>肌肉量充足度 15%</li>
     * </ul>
     */
    public int healthScore(double bmi, double bodyFatRate, int visceralFat,
                           double muscleMass, double weight, int gender, int age) {
        // --- BMI 得分 ---
        double bmiScore;
        if (bmi <= 0) {
            bmiScore = 0;
        } else if (bmi < BMI_UNDERWEIGHT) {
            // 偏瘦：越瘦分越低
            bmiScore = Math.max(40, 100 - (BMI_UNDERWEIGHT - bmi) * 25);
        } else if (bmi <= BMI_NORMAL_UPPER) {
            bmiScore = 100;              // 正常区间满分
        } else if (bmi <= BMI_OVERWEIGHT_UPPER) {
            bmiScore = 100 - (bmi - BMI_NORMAL_UPPER) * 10;   // 超重递减
        } else {
            bmiScore = Math.max(20, 60 - (bmi - BMI_OVERWEIGHT_UPPER) * 15);
        }

        // --- 体脂率得分 ---
        double bfLow = gender == 1 ? 10 : 18;
        double bfHigh = gender == 1 ? 20 : 28;
        double bfScore;
        if (bodyFatRate <= 0) {
            bfScore = 0;
        } else if (bodyFatRate < bfLow) {
            bfScore = Math.max(50, 100 - (bfLow - bodyFatRate) * 5);
        } else if (bodyFatRate <= bfHigh) {
            bfScore = 100;
        } else if (bodyFatRate <= bfHigh + 10) {
            bfScore = 100 - (bodyFatRate - bfHigh) * 6;
        } else {
            bfScore = Math.max(20, 40 - (bodyFatRate - bfHigh - 10) * 3);
        }

        // --- 内脏脂肪得分 ---
        double vfScore;
        if (visceralFat <= 0) {
            vfScore = 0;
        } else if (visceralFat <= 9) {
            vfScore = 100;
        } else if (visceralFat <= 14) {
            vfScore = 100 - (visceralFat - 9) * 12;
        } else {
            vfScore = Math.max(20, 40 - (visceralFat - 14) * 12);
        }

        // --- 肌肉量得分 ---
        // 参考：骨骼肌占体重 30%-40% 为良好（男性略高）
        double muscleRatio = weight > 0 ? muscleMass / weight * 100 : 0;
        double mRatioLow = gender == 1 ? 28 : 22;
        double mRatioHigh = gender == 1 ? 40 : 34;
        double mScore;
        if (muscleRatio <= 0) {
            mScore = 0;
        } else if (muscleRatio < mRatioLow) {
            mScore = Math.max(40, 100 - (mRatioLow - muscleRatio) * 6);
        } else if (muscleRatio <= mRatioHigh) {
            mScore = 100;
        } else {
            mScore = 95;    // 肌肉超常也算良好
        }

        double total = bmiScore * 0.35 + bfScore * 0.30 + vfScore * 0.20 + mScore * 0.15;
        return clamp((int) Math.round(total), 0, 100);
    }

    /**
     * 健康等级文字
     */
    public String healthLevel(int score) {
        if (score >= 85) return "优秀";
        if (score >= 70) return "良好";
        if (score >= 55) return "一般";
        if (score >= 40) return "偏弱";
        return "较差";
    }

    /**
     * 营养均衡评分（0-100）
     * <p>
     * 基于近 N 天的饮食记录，从三个角度评估：
     * <ul>
     *     <li>摄入达标度（相对目标热量的偏差）45%</li>
     *     <li>记录完整度（有记录的天数占比）30%</li>
     *     <li>摄入稳定性（日间波动越小越好）25%</li>
     * </ul>
     *
     * @param intakes     每日摄入热量列表（按天，无记录为 0）
     * @param target      每日目标摄入
     * @param recordDays  有记录的天数
     */
    public int nutritionScore(List<Integer> intakes, int target, int recordDays) {
        if (intakes == null || intakes.isEmpty() || target <= 0) {
            return 0;
        }
        int days = intakes.size();

        // --- 达标度：只统计有记录的日子 ---
        List<Integer> valid = new ArrayList<>();
        for (Integer v : intakes) {
            if (v != null && v > 0) valid.add(v);
        }
        double targetScore = 0;
        if (!valid.isEmpty()) {
            double sum = 0;
            for (Integer v : valid) {
                // 偏差在 ±10% 内算满分，超出按比例扣分
                double deviation = Math.abs(v - target) / (double) target;
                double s = deviation <= 0.10 ? 100 : Math.max(20, 100 - (deviation - 0.10) * 130);
                sum += s;
            }
            targetScore = sum / valid.size();
        }

        // --- 完整度 ---
        double coverageScore = recordDays * 100.0 / days;

        // --- 稳定性：标准差越小越好 ---
        double stabilityScore = 0;
        if (valid.size() >= 2) {
            double mean = valid.stream().mapToInt(Integer::intValue).average().orElse(0);
            double varSum = 0;
            for (Integer v : valid) {
                varSum += Math.pow(v - mean, 2);
            }
            double sd = Math.sqrt(varSum / valid.size());
            double cv = mean > 0 ? sd / mean : 0;       // 变异系数
            // CV < 0.10 优秀；0.10-0.30 递减
            if (cv <= 0.10) {
                stabilityScore = 100;
            } else {
                stabilityScore = Math.max(20, 100 - (cv - 0.10) * 250);
            }
        } else if (valid.size() == 1) {
            stabilityScore = 60;    // 数据太少，给中间分
        }

        double total = targetScore * 0.45 + coverageScore * 0.30 + stabilityScore * 0.25;
        return clamp((int) Math.round(total), 0, 100);
    }

    /**
     * 生成针对性建议（按优先级排序，最多 6 条）
     */
    public List<Map<String, Object>> buildAdvice(double bmi, double bodyFatRate,
                                                 int visceralFat, double muscleRatio,
                                                 int gender, int age) {
        List<Map<String, Object>> list = new ArrayList<>();

        // BMI 相关
        if (bmi > 0) {
            if (bmi < BMI_UNDERWEIGHT) {
                list.add(advice("warning", "体重偏轻",
                        String.format("BMI %.1f 低于 18.5，建议增加优质蛋白与碳水摄入，配合抗阻训练增肌。", bmi)));
            } else if (bmi > BMI_OVERWEIGHT_UPPER) {
                list.add(advice("danger", "体重超标",
                        String.format("BMI %.1f 超过 27.9，建议控制总热量，每周 150 分钟中等强度有氧。", bmi)));
            } else if (bmi > BMI_NORMAL_UPPER) {
                list.add(advice("warning", "体重偏高",
                        String.format("BMI %.1f 处于超重区间，建议制造 300-500 kcal/天热量缺口。", bmi)));
            } else {
                list.add(advice("success", "体重正常",
                        String.format("BMI %.1f 在健康区间（18.5-23.9），继续保持。", bmi)));
            }
        }

        // 体脂率相关
        double bfLow = gender == 1 ? 10 : 18;
        double bfHigh = gender == 1 ? 20 : 28;
        if (bodyFatRate > 0) {
            if (bodyFatRate > bfHigh) {
                list.add(advice("danger", "体脂率偏高",
                        String.format("体脂率 %.1f%% 高于健康上限 %.0f%%，建议增加有氧并减少精制碳水。",
                                bodyFatRate, bfHigh)));
            } else if (bodyFatRate < bfLow) {
                list.add(advice("warning", "体脂率偏低",
                        String.format("体脂率 %.1f%% 低于健康下限 %.0f%%，注意必需脂肪酸摄入。",
                                bodyFatRate, bfLow)));
            }
        }

        // 内脏脂肪
        if (visceralFat > 14) {
            list.add(advice("danger", "内脏脂肪偏高",
                    String.format("内脏脂肪等级 %d（≥15 为高危），优先减腹部内脏脂肪，控制糖分与酒精。",
                            visceralFat)));
        } else if (visceralFat > 9) {
            list.add(advice("warning", "内脏脂肪偏高",
                    String.format("内脏脂肪等级 %d 处于偏高区间，建议增加核心训练与有氧。", visceralFat)));
        }

        // 肌肉量
        double mRatioLow = gender == 1 ? 28 : 22;
        if (muscleRatio > 0 && muscleRatio < mRatioLow) {
            list.add(advice("warning", "肌肉量不足",
                    String.format("肌肉占比 %.1f%% 低于建议值 %.0f%%，建议每周 2-3 次抗阻训练并补足蛋白质。",
                            muscleRatio, mRatioLow)));
        }

        // 减脂期蛋白提醒
        if (bmi > BMI_OVERWEIGHT_UPPER) {
            list.add(advice("info", "蛋白摄入",
                    "减脂期建议蛋白质摄入 1.2-1.6 g/kg 体重，防止肌肉流失。"));
        }

        // 排序：danger > warning > info > success
        Map<String, Integer> order = Map.of("danger", 0, "warning", 1, "info", 2, "success", 3);
        list.sort(Comparator.comparingInt(a -> order.getOrDefault((String) a.get("level"), 9)));
        return list.size() > 6 ? list.subList(0, 6) : list;
    }

    private Map<String, Object> advice(String level, String title, String content) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("level", level);
        m.put("title", title);
        m.put("content", content);
        return m;
    }

    // ---------- 工具 ----------
    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
