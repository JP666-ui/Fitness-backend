package com.fitness.controller;

import com.fitness.common.Result;
import com.fitness.entity.ExerciseRecord;
import com.fitness.entity.User;
import com.fitness.service.ExerciseRecordService;
import com.fitness.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 运动记录 Controller
 */
@RestController
@RequestMapping("/exercise")
public class ExerciseRecordController {
    
    @Autowired
    private ExerciseRecordService exerciseRecordService;
    
    @Autowired
    private UserService userService;
    
    /**
     * 添加运动记录
     * POST /exercise/add
     */
    @PostMapping("/add")
    public Result<ExerciseRecord> add(HttpServletRequest request, @RequestBody ExerciseRecord record) {
        Long userId = (Long) request.getAttribute("userId");
        record.setUserId(userId);
        
        // 未填运动类型标识时，用运动名称兜底（便于热量计算）
        if (record.getExerciseType() == null || record.getExerciseType().trim().isEmpty()) {
            record.setExerciseType(record.getExerciseName());
        }
        
        // 未提供消耗热量时自动计算
        if (record.getCalorie() == null || record.getCalorie() <= 0) {
            User user = userService.getById(userId);
            Double weight = user == null ? null : user.getWeight();
            if (weight == null) {
                weight = 60.0;  // 未设置体重时使用默认值，保证功能可用
            }
            Integer calorie = exerciseRecordService.calculateCalorie(
                    record.getExerciseType(),
                    record.getDuration(),
                    weight
            );
            record.setCalorie(calorie);
        }
        
        // 运动时间兜底
        if (record.getExerciseTime() == null) {
            record.setExerciseTime(LocalDateTime.now());
        }
        
        exerciseRecordService.save(record);
        return Result.success(record);
    }
    
    /**
     * 更新运动记录
     * PUT /exercise/update/{id}
     */
    @PutMapping("/update/{id}")
    public Result<ExerciseRecord> update(HttpServletRequest request,
                                         @PathVariable Long id,
                                         @RequestBody ExerciseRecord record) {
        Long userId = (Long) request.getAttribute("userId");
        
        ExerciseRecord existing = exerciseRecordService.getById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            return Result.fail(404, "记录不存在");
        }
        
        record.setId(id);
        record.setUserId(userId);
        
        // 重新计算热量
        if (record.getExerciseType() != null && record.getDuration() != null) {
            User user = userService.getById(userId);
            Double weight = user == null ? null : user.getWeight();
            if (weight == null) {
                weight = 60.0;
            }
            record.setCalorie(exerciseRecordService.calculateCalorie(
                    record.getExerciseType(),
                    record.getDuration(),
                    weight
            ));
        }
        
        exerciseRecordService.updateById(record);
        return Result.success(exerciseRecordService.getById(id));
    }
    
    /**
     * 删除运动记录
     * DELETE /exercise/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        
        ExerciseRecord existing = exerciseRecordService.getById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            return Result.fail(404, "记录不存在");
        }
        
        return Result.success(exerciseRecordService.removeById(id));
    }
    
    /**
     * 获取用户的运动记录列表
     * GET /exercise/list
     */
    @GetMapping("/list")
    public Result<List<ExerciseRecord>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        
        List<ExerciseRecord> list = exerciseRecordService.lambdaQuery()
                .eq(ExerciseRecord::getUserId, userId)
                .orderByDesc(ExerciseRecord::getExerciseTime)
                .list();
        
        return Result.success(list);
    }
    
    /**
     * 获取今日运动消耗
     * GET /exercise/today
     */
    @GetMapping("/today")
    public Result<Map<String, Object>> getTodayCalorie(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        
        Map<String, Object> result = new HashMap<>();
        result.put("todayCalorieBurn", exerciseRecordService.getTodayCalorieBurn(userId));
        
        return Result.success(result);
    }
    
    /**
     * 获取运动统计
     * GET /exercise/statistics?startTime=xxx&endTime=xxx
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics(HttpServletRequest request,
                                                     @RequestParam(required = false) String startTime,
                                                     @RequestParam(required = false) String endTime) {
        Long userId = (Long) request.getAttribute("userId");
        
        LocalDateTime start = startTime != null
                ? LocalDateTime.parse(startTime)
                : LocalDateTime.now().minusDays(7);
        LocalDateTime end = endTime != null
                ? LocalDateTime.parse(endTime)
                : LocalDateTime.now();
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", exerciseRecordService.getStatistics(userId, start, end));
        result.put("byType", exerciseRecordService.getStatisticsByType(userId, start, end));
        
        return Result.success(result);
    }
    
    /**
     * 计算运动热量（不保存）
     * POST /exercise/calculateCalorie
     */
    @PostMapping("/calculateCalorie")
    public Result<Map<String, Object>> calculateCalorie(HttpServletRequest request,
                                                        @RequestBody Map<String, Object> params) {
        Long userId = (Long) request.getAttribute("userId");
        
        Object typeObj = params.get("exerciseType");
        Object durationObj = params.get("duration");
        
        if (typeObj == null || durationObj == null) {
            return Result.fail(400, "参数不完整");
        }
        
        String exerciseType = String.valueOf(typeObj);
        int duration = durationObj instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(durationObj));
        
        User user = userService.getById(userId);
        Double weight = user == null ? null : user.getWeight();
        if (weight == null) {
            weight = 60.0;
        }
        
        Integer calorie = exerciseRecordService.calculateCalorie(exerciseType, duration, weight);
        
        Map<String, Object> result = new HashMap<>();
        result.put("calorie", calorie);
        result.put("exerciseType", exerciseType);
        result.put("duration", duration);
        result.put("weight", weight);
        
        return Result.success(result);
    }
}