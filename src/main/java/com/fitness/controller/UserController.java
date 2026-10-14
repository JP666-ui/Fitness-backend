package com.fitness.controller;

import com.fitness.common.Result;
import com.fitness.entity.FitnessPlan;
import com.fitness.entity.User;
import com.fitness.service.CalorieCalculatorService;
import com.fitness.service.FitnessPlanService;
import com.fitness.service.UserService;
import com.fitness.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户 Controller
 */
@RestController
@RequestMapping("/user")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private FitnessPlanService fitnessPlanService;

    @Autowired
    private CalorieCalculatorService calorieCalculator;
    
    /**
     * 用户注册
     * POST /user/register
     */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> params) {
        try {
            String username = params.get("username");
            String password = params.get("password");
            String nickname = params.get("nickname");
            
            if (username == null || password == null) {
                return Result.fail(400, "用户名和密码不能为空");
            }
            
            User user = userService.register(username, password, nickname);
            
            // 生成 token
            String token = jwtUtil.generateToken(user.getId(), user.getUsername());
            
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("user", user);
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.fail(500, e.getMessage());
        }
    }
    
    /**
     * 用户登录
     * POST /user/login
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        try {
            String username = params.get("username");
            String password = params.get("password");
            
            if (username == null || password == null) {
                return Result.fail(400, "用户名和密码不能为空");
            }
            
            User user = userService.login(username, password);
            
            // 生成 token
            String token = jwtUtil.generateToken(user.getId(), user.getUsername());
            
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("user", user);
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.fail(500, e.getMessage());
        }
    }
    
    /**
     * 获取当前用户信息
     * GET /user/info
     */
    @GetMapping("/info")
    public Result<User> getUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getById(userId);
        if (user == null) {
            return Result.fail(404, "用户不存在");
        }
        // 不返回密码
        user.setPassword(null);
        return Result.success(user);
    }
    
    /**
     * 更新用户信息
     * PUT /user/update
     * <p>
     * 设置目标体重后，会自动重新计算每日热量缺口并同步到当前计划。
     */
    @PutMapping("/update")
    public Result<Map<String, Object>> updateUser(HttpServletRequest request, @RequestBody User userInfo) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getById(userId);
        if (user == null) {
            return Result.fail(404, "用户不存在");
        }

        // 记录旧的目标体重，用于判断是否需要重算缺口
        Double oldTargetWeight = user.getTargetWeight();
        Double oldWeight = user.getWeight();

        // 更新允许修改的字段
        if (userInfo.getNickname() != null) {
            user.setNickname(userInfo.getNickname());
        }
        if (userInfo.getEmail() != null) {
            user.setEmail(userInfo.getEmail());
        }
        if (userInfo.getPhone() != null) {
            user.setPhone(userInfo.getPhone());
        }
        if (userInfo.getAvatar() != null) {
            user.setAvatar(userInfo.getAvatar());
        }
        if (userInfo.getGender() != null) {
            user.setGender(userInfo.getGender());
        }
        if (userInfo.getAge() != null) {
            user.setAge(userInfo.getAge());
        }
        if (userInfo.getHeight() != null) {
            user.setHeight(userInfo.getHeight());
        }
        if (userInfo.getWeight() != null) {
            user.setWeight(userInfo.getWeight());
        }
        if (userInfo.getTargetWeight() != null) {
            user.setTargetWeight(userInfo.getTargetWeight());
        }

        userService.updateById(user);

        Map<String, Object> data = new HashMap<>();
        data.put("user", user);

        // 目标体重或当前体重发生变化时，自动重算热量缺口
        boolean targetChanged = userInfo.getTargetWeight() != null
                && !userInfo.getTargetWeight().equals(oldTargetWeight);
        boolean weightChanged = userInfo.getWeight() != null
                && !userInfo.getWeight().equals(oldWeight);

        if (targetChanged || weightChanged) {
            Map<String, Object> recalculated = recalculatePlan(user);
            if (recalculated != null) {
                data.put("recalculated", true);
                data.put("plan", recalculated);
            } else {
                data.put("recalculated", false);
                data.put("message", "已保存。补全身高、体重、年龄后可自动计算热量缺口");
            }
        }

        user.setPassword(null);
        return Result.success(data);
    }

    /**
     * 根据最新的用户资料重算当前计划的热量缺口
     *
     * @return 重算后的计划信息；资料不足或无需计算时返回 null
     */
    private Map<String, Object> recalculatePlan(User user) {
        // 资料不足无法计算
        if (calorieCalculator.calculateBmr(user) <= 0
                || user.getTargetWeight() == null
                || user.getWeight() == null) {
            return null;
        }

        double diff = user.getWeight() - user.getTargetWeight();

        // 判断目标类型
        String planType;
        if (diff > 0.5) {
            planType = "loss";
        } else if (diff < -0.5) {
            planType = "gain";
        } else {
            planType = "maintain";
        }

        // 复用已有计划，没有则新建
        FitnessPlan plan = fitnessPlanService.getActivePlan(user.getId());
        if (plan == null) {
            plan = fitnessPlanService.getLatestPlan(user.getId());
        }

        if (plan == null) {
            plan = fitnessPlanService.createDefaultPlan(user, planType);
        } else {
            // 重算并更新现有计划
            int tdee = calorieCalculator.calculateTdee(user);
            int bmr = calorieCalculator.calculateBmr(user);

            // 保持原有周期；若无结束日期则按健康速度推算
            long days;
            if (plan.getEndDate() != null && plan.getEndDate().isAfter(LocalDate.now())) {
                days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), plan.getEndDate());
            } else {
                days = calorieCalculator.suggestDays(user, user.getTargetWeight());
                if (days <= 0) {
                    days = 30;
                }
                plan.setEndDate(LocalDate.now().plusDays(days));
            }

            int deficit = calorieCalculator.calculateDailyDeficit(user, user.getTargetWeight(), days);
            int intake;
            if ("gain".equals(planType)) {
                intake = tdee + 300;
                deficit = -300;
            } else if ("loss".equals(planType)) {
                intake = tdee - deficit;
                if (bmr > 0 && intake < bmr) {
                    intake = bmr;
                    deficit = tdee - bmr;
                }
            } else {
                intake = tdee > 0 ? tdee : 2000;
                deficit = 0;
            }

            plan.setPlanType(planType);
            plan.setTargetWeight(user.getTargetWeight());
            plan.setWeightToLose(diff > 0 ? Math.round(diff * 10) / 10.0 : null);
            plan.setTargetCalorie(intake > 0 ? intake : 2000);
            plan.setDailyDeficit(deficit);
            plan.setStatus(1);
            plan.setUpdateTime(java.time.LocalDateTime.now());

            if ("loss".equals(planType)) {
                plan.setPlanName("减脂计划");
                plan.setRemark(String.format(
                        "需减重 %.1fkg，每日热量缺口 %d kcal，建议摄入 %d kcal（TDEE %d kcal）",
                        diff, deficit, plan.getTargetCalorie(), tdee));
            } else if ("gain".equals(planType)) {
                plan.setPlanName("增肌计划");
                plan.setRemark(String.format("每日热量盈余 300 kcal，建议摄入 %d kcal", plan.getTargetCalorie()));
            } else {
                plan.setPlanName("维持计划");
                plan.setRemark(String.format("建议摄入 %d kcal，保持均衡饮食", plan.getTargetCalorie()));
            }

            fitnessPlanService.updateById(plan);
        }

        return fitnessPlanService.buildPlanDetail(plan, user);
    }
    
    /**
     * 更新体重
     * PUT /user/weight
     */
    @PutMapping("/weight")
    public Result<Map<String, Object>> updateWeight(HttpServletRequest request, @RequestBody Map<String, Double> params) {
        Long userId = (Long) request.getAttribute("userId");
        Double weight = params.get("weight");
        
        if (weight == null || weight <= 0) {
            return Result.fail(400, "体重数据无效");
        }
        
        boolean success = userService.updateWeight(userId, weight);
        if (!success) {
            return Result.fail(500, "更新失败");
        }
        
        // 返回BMI和每日目标热量
        Double bmi = userService.calculateBMI(userId);
        Integer dailyCalorie = userService.calculateDailyCalorie(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("weight", weight);
        result.put("bmi", bmi);
        result.put("dailyCalorieGoal", dailyCalorie);
        
        return Result.success(result);
    }
    
    /**
     * 获取用户健康数据（BMI、每日目标热量等）
     * GET /user/health
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> getHealthData(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        
        User user = userService.getById(userId);
        if (user == null) {
            return Result.fail(404, "用户不存在");
        }
        
        Double bmi = userService.calculateBMI(userId);
        Integer dailyCalorie = userService.calculateDailyCalorie(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("height", user.getHeight());
        result.put("weight", user.getWeight());
        result.put("targetWeight", user.getTargetWeight());
        result.put("bmi", bmi);
        result.put("bmiStatus", getBMIStatus(bmi));
        result.put("dailyCalorieGoal", dailyCalorie);
        
        return Result.success(result);
    }
    
    /**
     * 根据BMI值获取状态
     */
    private String getBMIStatus(Double bmi) {
        if (bmi == null) {
            return "未知";
        }
        if (bmi < 18.5) {
            return "偏瘦";
        } else if (bmi < 24) {
            return "正常";
        } else if (bmi < 28) {
            return "偏胖";
        } else {
            return "肥胖";
        }
    }
}
