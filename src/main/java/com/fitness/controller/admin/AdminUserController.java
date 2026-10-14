package com.fitness.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fitness.common.Result;
import com.fitness.entity.User;
import com.fitness.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员用户管理控制器
 */
@RestController
@RequestMapping("/admin/user")
public class AdminUserController {

    @Autowired
    private UserService userService;

    /**
     * 获取用户列表（分页）
     */
    @GetMapping("/list")
    public Result<Page<User>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        
        // 关键词搜索（用户名或昵称）
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                .like("username", keyword)
                .or()
                .like("nickname", keyword)
            );
        }
        
        // 按创建时间倒序
        queryWrapper.orderByDesc("create_time");
        
        Page<User> userPage = userService.page(new Page<>(page, size), queryWrapper);
        
        // 清除敏感信息
        userPage.getRecords().forEach(user -> user.setPassword(null));
        
        return Result.success(userPage);
    }

    /**
     * 获取所有用户（不分页）
     */
    @GetMapping("/all")
    public Result<List<User>> getAllUsers() {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        
        List<User> users = userService.list(queryWrapper);
        
        // 清除敏感信息
        users.forEach(user -> user.setPassword(null));
        
        return Result.success(users);
    }

    /**
     * 根据ID获取用户详情
     */
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user != null) {
            user.setPassword(null);
            return Result.success(user);
        }
        return Result.<User>error("用户不存在");
    }

    /**
     * 更新用户状态（启用/禁用）
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        
        boolean success = userService.updateById(user);
        if (success) {
            return Result.<Void>success(null);
        }
        return Result.<Void>error("更新失败");
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean success = userService.removeById(id);
        if (success) {
            return Result.<Void>success(null);
        }
        return Result.<Void>error("删除失败");
    }

    /**
     * 获取用户统计
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        // 总用户数
        long totalCount = userService.count();
        
        // 今日新增
        QueryWrapper<User> todayWrapper = new QueryWrapper<>();
        todayWrapper.ge("create_time", java.time.LocalDateTime.now().toLocalDate().atStartOfDay());
        long todayCount = userService.count(todayWrapper);
        
        // 活跃用户（状态为1）
        QueryWrapper<User> activeWrapper = new QueryWrapper<>();
        activeWrapper.eq("status", 1);
        long activeCount = userService.count(activeWrapper);
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalCount", totalCount);
        statistics.put("todayCount", todayCount);
        statistics.put("activeCount", activeCount);
        
        return Result.success(statistics);
    }
}
