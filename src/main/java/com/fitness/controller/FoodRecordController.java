package com.fitness.controller;

import com.fitness.common.Result;
import com.fitness.common.annotation.Idempotent;
import com.fitness.entity.FoodRecord;
import com.fitness.entity.User;
import com.fitness.entity.export.FoodRecordExport;
import com.fitness.service.FoodRecordService;
import com.fitness.service.UserService;
import com.fitness.util.ExportUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/foodRecord")
public class FoodRecordController {

    private final FoodRecordService foodRecordService;

    @Autowired
    private UserService userService;

    @Autowired
    private ExportUtil exportUtil;

    public FoodRecordController(FoodRecordService foodRecordService) {
        this.foodRecordService = foodRecordService;
    }

    /**
     * 查询当前用户的饮食记录
     * GET /foodRecord/list
     */
    @GetMapping("/list")
    public Result<List<FoodRecord>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }

        List<FoodRecord> list = foodRecordService.lambdaQuery()
                .eq(FoodRecord::getUserId, userId)
                .orderByDesc(FoodRecord::getRecordTime)
                .list();

        return Result.success(list);
    }

    /**
     * 根据用户ID查询饮食记录
     * GET /foodRecord/list/{userId}
     */
    @GetMapping("/list/{userId}")
    public Result<List<FoodRecord>> listByUserId(@PathVariable Long userId) {
        return Result.success(foodRecordService.lambdaQuery()
                .eq(FoodRecord::getUserId, userId)
                .orderByDesc(FoodRecord::getRecordTime)
                .list());
    }

    /**
     * 添加饮食记录
     * POST /foodRecord/add
     * <p>
     * 幂等：3 秒内同一用户提交相同内容只成功一次，防止连点产生重复记录
     */
    @Idempotent(seconds = 3, message = "请勿重复提交，3 秒后再试")
    @PostMapping("/add")
    public Result<FoodRecord> add(HttpServletRequest request, @RequestBody FoodRecord foodRecord) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }

        foodRecord.setId(null);
        foodRecord.setUserId(userId);

        // 未填热量时，按「常见食物库」的每100g热量估算
        if (foodRecord.getCalorie() == null || foodRecord.getCalorie() <= 0) {
            int weight = foodRecord.getWeight() == null ? 100 : foodRecord.getWeight();
            int unit = CommonFood.caloriePer100g(foodRecord.getFoodName());
            foodRecord.setCalorie(unit * weight / 100);
        }

        // 日期与时间必须保持一致：
        // 统计接口按 record_time 聚合，若只设 record_date 而 record_time 走默认值，
        // 所有补录的数据都会被算到「录入当天」，导致历史数据全部堆在一天。
        LocalDate recordDate = foodRecord.getRecordDate();
        if (recordDate == null) {
            recordDate = LocalDate.now();
            foodRecord.setRecordDate(recordDate);
        }
        // record_time 是 java.util.Date 类型
        LocalDateTime now = LocalDateTime.now();
        if (foodRecord.getRecordTime() == null) {
            foodRecord.setRecordTime(toDate(recordDate.atTime(
                    now.getHour(), now.getMinute(), now.getSecond())));
        } else {
            LocalDate timeDate = toLocalDate(foodRecord.getRecordTime());
            if (!timeDate.equals(recordDate)) {
                // 时间与日期冲突时，保留时分秒、把日期改成 record_date
                LocalDateTime t = toLocalDateTime(foodRecord.getRecordTime());
                foodRecord.setRecordTime(toDate(recordDate.atTime(t.toLocalTime())));
            }
        }

        foodRecordService.save(foodRecord);
        return Result.success(foodRecord);
    }

    private java.util.Date toDate(LocalDateTime ldt) {
        return java.util.Date.from(ldt.atZone(java.time.ZoneId.systemDefault()).toInstant());
    }

    private LocalDate toLocalDate(java.util.Date d) {
        return d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    private LocalDateTime toLocalDateTime(java.util.Date d) {
        return d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * 更新饮食记录
     * PUT /foodRecord/update
     */
    @PutMapping("/update")
    public Result<Boolean> update(HttpServletRequest request, @RequestBody FoodRecord foodRecord) {
        Long userId = (Long) request.getAttribute("userId");

        FoodRecord existing = foodRecordService.getById(foodRecord.getId());
        if (existing == null || !existing.getUserId().equals(userId)) {
            return Result.fail(404, "记录不存在");
        }

        foodRecord.setUserId(userId);
        return Result.success(foodRecordService.updateById(foodRecord));
    }

    /**
     * 删除饮食记录
     * DELETE /foodRecord/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");

        FoodRecord existing = foodRecordService.getById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            return Result.fail(404, "记录不存在");
        }

        return Result.success(foodRecordService.removeById(id));
    }

    /**
     * 导出饮食记录为 Excel
     * GET /foodRecord/export
     */
    @GetMapping("/export")
    public void export(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long userId = (Long) request.getAttribute("userId");

        List<FoodRecord> records = foodRecordService.lambdaQuery()
                .eq(FoodRecord::getUserId, userId)
                .orderByDesc(FoodRecord::getRecordTime)
                .list();

        User user = userService.getById(userId);
        String username = user == null ? "" : user.getUsername();

        List<FoodRecordExport> exportData = new ArrayList<>();
        int index = 1;
        for (FoodRecord record : records) {
            FoodRecordExport export = new FoodRecordExport();
            export.setIndex(index++);
            export.setUsername(username);
            export.setFoodName(record.getFoodName());
            export.setWeight(record.getWeight());
            export.setCalorie(record.getCalorie());
            export.setRecordTime(record.getRecordTime());
            export.setRemark(record.getRemark());
            exportData.add(export);
        }

        exportUtil.export(response, "饮食记录", "记录", exportData, FoodRecordExport.class);
    }

    /**
     * 常见食物每 100g 热量参考表（与前端下拉框保持一致）
     */
    private static final class CommonFood {
        private static final Map<String, Integer> TABLE = new HashMap<>();

        static {
            TABLE.put("米饭", 116);
            TABLE.put("馒头", 223);
            TABLE.put("面条", 137);
            TABLE.put("鸡蛋", 155);
            TABLE.put("鸡胸肉", 165);
            TABLE.put("牛肉", 250);
            TABLE.put("猪肉", 263);
            TABLE.put("鱼肉", 104);
            TABLE.put("虾", 93);
            TABLE.put("苹果", 52);
            TABLE.put("香蕉", 89);
            TABLE.put("牛奶", 54);
            TABLE.put("酸奶", 72);
            TABLE.put("燕麦", 389);
            TABLE.put("全麦面包", 246);
            TABLE.put("红薯", 86);
            TABLE.put("土豆", 77);
            TABLE.put("西红柿", 18);
            TABLE.put("黄瓜", 15);
            TABLE.put("胡萝卜", 41);
            TABLE.put("西兰花", 34);
            TABLE.put("菠菜", 23);
            TABLE.put("豆腐", 76);
            TABLE.put("坚果", 607);
            TABLE.put("巧克力", 546);
            TABLE.put("可乐", 42);
            TABLE.put("啤酒", 43);
        }

        /** 默认按 200 kcal / 100g 估算未知食物 */
        static int caloriePer100g(String foodName) {
            if (foodName == null) {
                return 200;
            }
            return TABLE.getOrDefault(foodName.trim(), 200);
        }
    }
}