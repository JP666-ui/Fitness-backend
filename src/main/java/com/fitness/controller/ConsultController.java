package com.fitness.controller;

import com.fitness.common.Result;
import com.fitness.dto.ConsultChatRequest;
import com.fitness.entity.User;
import com.fitness.service.ExerciseRecordService;
import com.fitness.service.FoodRecordService;
import com.fitness.service.UserService;
import com.fitness.util.AiChatClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * AI 营养咨询 Controller
 * <p>
 * 与 AI 营养师自由对话，不需要上传任何图片。
 * AI 会自动读取用户的身体数据、健身目标与今日饮食运动情况，给出个性化建议。
 */
@RestController
@RequestMapping("/consult")
public class ConsultController {

    @Autowired
    private UserService userService;

    @Autowired
    private FoodRecordService foodRecordService;

    @Autowired
    private ExerciseRecordService exerciseRecordService;

    @Autowired
    private AiChatClient aiChatClient;

    /**
     * 与 AI 自由对话
     * POST /consult/chat
     * <p>
     * 入参：{ "message": "...", "history": [{role, content}] }
     */
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(HttpServletRequest request,
                                            @RequestBody ConsultChatRequest body) {
        Long userId = (Long) request.getAttribute("userId");
        String message = body.getMessage();

        if (message == null || message.trim().isEmpty()) {
            return Result.fail(400, "消息不能为空");
        }

        User user = userService.getById(userId);
        Map<String, Object> data = new HashMap<>();

        String systemPrompt = buildSystemPrompt(user, userId);
        String answer = aiChatClient.chatWithHistory(userId, systemPrompt, body.getHistory(), message.trim());

        // 未配置 API Key 或调用失败时，使用本地降级回复
        boolean fallback = false;
        if (answer == null || answer.isEmpty()) {
            answer = localChatResponse(message, user, userId);
            fallback = true;
            data.put("fallbackReason", aiChatClient.getLastError());
        }

        data.put("answer", answer);
        data.put("fallback", fallback);
        data.put("model", aiChatClient.getProviderName());
        // user=使用客户自己的密钥，system=使用系统内置免费模型
        data.put("keySource", aiChatClient.getLastKeySource());

        return Result.success(data);
    }

    /**
     * 获取营养建议
     * GET /consult/advice
     */
    @GetMapping("/advice")
    public Result<Map<String, Object>> getAdvice(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getById(userId);

        Map<String, Object> advice = new HashMap<>();
        String goal = inferGoal(user);
        int bmr = calculateBmr(user);

        if ("loss".equals(goal)) {
            advice.put("title", "减脂期营养建议");
            advice.put("calorieTarget", bmr - 500);
            advice.put("proteinRatio", "30-35%");
            advice.put("carbRatio", "40-45%");
            advice.put("fatRatio", "20-25%");
            advice.put("tips", Arrays.asList(
                    "控制总热量，保持每日 300-500 kcal 缺口",
                    "增加蛋白质摄入，保护肌肉",
                    "选择低 GI 碳水，稳定血糖",
                    "多吃蔬菜，增加饱腹感"
            ));
        } else if ("gain".equals(goal)) {
            advice.put("title", "增肌期营养建议");
            advice.put("calorieTarget", bmr + 300);
            advice.put("proteinRatio", "25-30%");
            advice.put("carbRatio", "45-50%");
            advice.put("fatRatio", "20-25%");
            advice.put("tips", Arrays.asList(
                    "适当增加热量盈余",
                    "训练后及时补充蛋白质",
                    "保证碳水摄入，支持训练",
                    "注意营养摄入时机"
            ));
        } else {
            advice.put("title", "维持期营养建议");
            advice.put("calorieTarget", bmr);
            advice.put("proteinRatio", "20-25%");
            advice.put("carbRatio", "45-50%");
            advice.put("fatRatio", "25-30%");
            advice.put("tips", Arrays.asList(
                    "保持均衡饮食",
                    "规律进餐时间",
                    "适量运动",
                    "定期监测体重"
            ));
        }

        advice.put("bmr", bmr);
        advice.put("goal", goal);
        advice.put("todayIntake", foodRecordService.getTodayCalorie(userId));
        advice.put("aiEnabled", aiChatClient.isConfigured());
        advice.put("model", aiChatClient.getProviderName());

        return Result.success(advice);
    }

    /**
     * 推荐食谱
     * GET /consult/recipes?type=loss
     */
    @GetMapping("/recipes")
    public Result<List<Map<String, Object>>> getRecipes(@RequestParam(defaultValue = "maintain") String type) {
        List<Map<String, Object>> recipes = new ArrayList<>();

        if ("loss".equals(type)) {
            recipes.add(createRecipe("减脂早餐", "燕麦粥 + 鸡蛋 + 牛奶", 400));
            recipes.add(createRecipe("减脂午餐", "鸡胸肉沙拉 + 全麦面包", 600));
            recipes.add(createRecipe("减脂晚餐", "清蒸鱼 + 蔬菜 + 少量米饭", 500));
        } else if ("gain".equals(type)) {
            recipes.add(createRecipe("增肌早餐", "燕麦 + 鸡蛋 + 牛奶 + 香蕉", 600));
            recipes.add(createRecipe("增肌午餐", "牛肉 + 米饭 + 蔬菜", 800));
            recipes.add(createRecipe("增肌晚餐", "鸡胸肉 + 红薯 + 蔬菜", 700));
        } else {
            recipes.add(createRecipe("均衡早餐", "全麦面包 + 鸡蛋 + 牛奶", 450));
            recipes.add(createRecipe("均衡午餐", "瘦肉 + 米饭 + 蔬菜", 650));
            recipes.add(createRecipe("均衡晚餐", "鱼 + 蔬菜 + 适量主食", 550));
        }

        return Result.success(recipes);
    }

    // ==================================================================
    // 辅助方法
    // ==================================================================

    /**
     * 构建系统提示词：注入用户身体数据与今日饮食情况，让建议更贴合实际
     */
    private String buildSystemPrompt(User user, Long userId) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业、友好的营养师和健身教练，名字叫「小健」。");
        sb.append("请用简洁易懂的中文回答，必要时分点说明，适当使用 emoji 让回答更亲切。");
        sb.append("不要编造用户没有提供的数据。\n\n");

        if (user != null) {
            sb.append("【用户资料】\n");
            if (user.getNickname() != null && !user.getNickname().isEmpty()) {
                sb.append("昵称：").append(user.getNickname()).append("\n");
            }
            if (user.getGender() != null) {
                sb.append("性别：")
                        .append(user.getGender() == 2 ? "女" : (user.getGender() == 1 ? "男" : "未填写"))
                        .append("\n");
            }
            if (user.getAge() != null) {
                sb.append("年龄：").append(user.getAge()).append(" 岁\n");
            }
            if (user.getHeight() != null) {
                sb.append("身高：").append(user.getHeight()).append(" cm\n");
            }
            if (user.getWeight() != null) {
                sb.append("体重：").append(user.getWeight()).append(" kg\n");
            }
            if (user.getTargetWeight() != null) {
                sb.append("目标体重：").append(user.getTargetWeight()).append(" kg\n");
            }
            if (user.getDailyCalorieGoal() != null) {
                sb.append("每日热量目标：").append(user.getDailyCalorieGoal()).append(" kcal\n");
            }
            sb.append("健身目标：").append(goalText(inferGoal(user))).append("\n");
            sb.append("基础代谢率(BMR)：").append(calculateBmr(user)).append(" kcal\n");
        }

        // 今日饮食与运动情况，让建议有依据
        try {
            int intake = foodRecordService.getTodayCalorie(userId);
            int burn = exerciseRecordService.getTodayCalorieBurn(userId);
            sb.append("\n【今日数据】\n");
            sb.append("已摄入：").append(intake).append(" kcal\n");
            sb.append("已消耗：").append(burn).append(" kcal\n");
            sb.append("净摄入：").append(intake - burn).append(" kcal\n");
            sb.append("日期：").append(LocalDate.now()).append("\n");
        } catch (Exception ignored) {
            // 数据获取失败不影响对话
        }

        sb.append("\n请基于以上信息，针对用户的问题给出个性化建议。");
        return sb.toString();
    }

    /**
     * 本地降级回复：未配置 API Key 或调用失败时使用
     */
    private String localChatResponse(String message, User user, Long userId) {
        String lower = message == null ? "" : message.toLowerCase();

        if (lower.contains("热量") || lower.contains("卡路里") || lower.contains("kcal")) {
            int todayIntake = safeTodayIntake(userId);
            int target = user != null && user.getDailyCalorieGoal() != null
                    ? user.getDailyCalorieGoal() : 2000;
            int remaining = target - todayIntake;

            return "📊 今日已摄入 " + todayIntake + " kcal，目标 " + target + " kcal，"
                    + (remaining > 0 ? "还可摄入约 " + remaining + " kcal。" : "已超出约 " + (-remaining) + " kcal。")
                    + "\n\n常见食物热量参考（每 100g）：\n"
                    + "• 米饭 116 kcal　• 鸡胸肉 165 kcal　• 鸡蛋 155 kcal\n"
                    + "• 西兰花 34 kcal　• 香蕉 89 kcal\n\n"
                    + "你可以把吃了什么、多少克告诉我，我帮你估算总热量。";
        }

        if (lower.contains("减脂") || lower.contains("减肥")) {
            return "减脂期饮食建议：\n\n"
                    + "1. 控制总热量：每日保持 300-500 kcal 缺口\n"
                    + "2. 高蛋白：蛋白质占比 30-40%，保护肌肉\n"
                    + "3. 适量碳水：优先燕麦、糙米等低 GI 食物\n"
                    + "4. 多吃蔬菜：增加饱腹感\n"
                    + "5. 少油少盐：以蒸、煮、烤为主\n\n"
                    + "示例：早餐燕麦+鸡蛋+牛奶，午餐米饭+鸡胸肉+蔬菜，晚餐鱼+蔬菜沙拉。";
        }

        if (lower.contains("增肌")) {
            return "增肌期饮食建议：\n\n"
                    + "1. 热量盈余：每日比消耗多 300-500 kcal\n"
                    + "2. 蛋白质：每公斤体重 1.6-2.0g\n"
                    + "3. 碳水充足：支撑高强度训练\n"
                    + "4. 训练后 30 分钟内补充碳水和蛋白质\n\n"
                    + "推荐：鸡胸肉、牛肉、鸡蛋、牛奶、燕麦、红薯。";
        }

        if (lower.contains("运动") || lower.contains("锻炼") || lower.contains("健身")) {
            return "运动后营养补充建议：\n\n"
                    + "黄金窗口期（运动后 30 分钟内）：\n"
                    + "• 碳水:蛋白质 ≈ 3:1 或 4:1\n"
                    + "• 推荐：香蕉 + 蛋白粉、全麦面包 + 鸡蛋、酸奶 + 水果\n\n"
                    + "注意及时补水、补充电解质，避免高脂食物影响吸收。";
        }

        if (lower.contains("蛋白质")) {
            return "高蛋白食物（每 100g）：\n\n"
                    + "动物蛋白：鸡胸肉 31g、牛肉 26g、鱼肉 20-25g、鸡蛋 13g\n"
                    + "植物蛋白：黄豆 36g、豆腐 8g、坚果 15-25g\n\n"
                    + "需求参考：普通成年人 0.8-1.2g/kg 体重，运动人群 1.2-1.6g/kg。";
        }

        if (lower.contains("早餐")) {
            return "早餐建议（约 400-500 kcal）：\n\n"
                    + "• 鸡蛋 1-2 个 + 全麦面包 2 片 + 牛奶 250ml\n"
                    + "• 燕麦 50g + 香蕉 1 根 + 酸奶 150g\n"
                    + "• 包子 1 个 + 豆浆 300ml + 鸡蛋 1 个\n\n"
                    + "早餐要吃够蛋白质，有助于全天血糖稳定和减少零食欲望。";
        }

        if (lower.contains("晚餐") || lower.contains("晚上")) {
            return "晚餐建议（约 450-550 kcal）：\n\n"
                    + "• 清蒸鱼 150g + 西兰花 200g + 米饭 100g\n"
                    + "• 鸡胸肉 120g + 蔬菜沙拉 + 红薯 150g\n"
                    + "• 豆腐 150g + 菠菜 + 杂粮饭 100g\n\n"
                    + "晚餐尽量在睡前 3 小时吃完，少油少盐，主食减半。";
        }

        if (lower.contains("你好") || lower.contains("hello") || lower.contains("hi") || lower.contains("在吗")) {
            return "你好！我是你的 AI 营养师小健 😊\n\n"
                    + "你可以直接问我任何饮食、营养、健身方面的问题，比如：\n"
                    + "• 减脂期晚餐吃什么好？\n"
                    + "• 我今天吃了包子油条，热量高吗？\n"
                    + "• 运动后应该补充什么？\n\n"
                    + "今天想了解点什么？";
        }

        return "收到你的问题！以下是一些通用建议：\n\n"
                + "1. 三餐定时定量，规律饮食\n"
                + "2. 多吃蔬菜水果，保证维生素摄入\n"
                + "3. 每周至少 150 分钟中等强度运动\n"
                + "4. 保证每天 7-8 小时睡眠\n"
                + "5. 每天饮水 2000-2500ml\n\n"
                + "想了解更具体的内容，可以把吃了什么、多少克告诉我。";
    }

    private int safeTodayIntake(Long userId) {
        try {
            return foodRecordService.getTodayCalorie(userId);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 根据当前体重与目标体重推断用户目标
     */
    private String inferGoal(User user) {
        if (user == null || user.getWeight() == null || user.getTargetWeight() == null) {
            return "maintain";
        }
        double diff = user.getTargetWeight() - user.getWeight();
        if (diff < -0.5) {
            return "loss";
        }
        if (diff > 0.5) {
            return "gain";
        }
        return "maintain";
    }

    private String goalText(String goal) {
        Map<String, String> map = new HashMap<>();
        map.put("loss", "减脂");
        map.put("gain", "增肌");
        map.put("maintain", "维持");
        return map.getOrDefault(goal, "维持");
    }

    /**
     * Mifflin-St Jeor 公式计算基础代谢率
     */
    private int calculateBmr(User user) {
        if (user == null || user.getWeight() == null || user.getHeight() == null || user.getAge() == null) {
            return 2000;
        }

        double bmr = 10 * user.getWeight() + 6.25 * user.getHeight() - 5 * user.getAge();
        if (user.getGender() != null && user.getGender() == 2) {
            bmr -= 161;
        } else {
            bmr += 5;
        }
        return (int) Math.round(bmr);
    }

    private Map<String, Object> createRecipe(String name, String content, int calorie) {
        Map<String, Object> recipe = new HashMap<>();
        recipe.put("name", name);
        recipe.put("content", content);
        recipe.put("calorie", calorie);
        return recipe;
    }
}