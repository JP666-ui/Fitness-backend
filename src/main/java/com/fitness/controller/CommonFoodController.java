package com.fitness.controller;

import com.fitness.common.Result;
import com.fitness.common.cache.LocalCache;
import com.fitness.entity.CommonFood;
import com.fitness.mapper.CommonFoodMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 常见食物库 Controller
 * <p>
 * 供「手动添加饮食记录」的下拉框使用，支持按分类筛选与关键词搜索。
 * <p>
 * 性能优化：食物库属于「读多写少、总量小（151 条）、允许短暂不一致」的数据，
 * 使用本地 LRU 缓存（TTL 10 分钟）避免每次请求都查库。
 */
@RestController
@RequestMapping("/common-food")
public class CommonFoodController {

    /** 缓存 key 前缀，便于按前缀批量失效 */
    private static final String CACHE_PREFIX = "food:";

    /** 缓存有效期：10 分钟 */
    private static final long CACHE_TTL = 10 * 60 * 1000L;

    @Autowired
    private CommonFoodMapper commonFoodMapper;

    @Autowired
    private LocalCache localCache;

    /**
     * 获取食物列表
     * GET /common-food/list?category=主食&keyword=包子
     */
    @GetMapping("/list")
    public Result<List<CommonFood>> list(@RequestParam(required = false) String category,
                                         @RequestParam(required = false) String keyword) {
        // 有关键词优先按关键词搜
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            String cacheKey = CACHE_PREFIX + "search:" + kw;
            List<CommonFood> list = localCache.getOrLoad(cacheKey, CACHE_TTL,
                    () -> commonFoodMapper.search(kw, 200));
            return Result.success(list);
        }
        if (category != null && !category.trim().isEmpty()) {
            String cat = category.trim();
            String cacheKey = CACHE_PREFIX + "category:" + cat;
            List<CommonFood> list = localCache.getOrLoad(cacheKey, CACHE_TTL,
                    () -> commonFoodMapper.findByCategory(cat));
            return Result.success(list);
        }
        List<CommonFood> list = localCache.getOrLoad(CACHE_PREFIX + "all", CACHE_TTL,
                () -> commonFoodMapper.selectList(null));
        return Result.success(list);
    }

    /**
     * 获取所有分类
     * GET /common-food/categories
     */
    @GetMapping("/categories")
    public Result<List<String>> categories() {
        return Result.success(commonFoodMapper.findAllCategories());
    }

    /**
     * 按名称查询单个食物的热量（用于自动填充）
     * GET /common-food/calorie?name=包子
     */
    @GetMapping("/calorie")
    public Result<Map<String, Object>> calorie(@RequestParam String name) {
        List<CommonFood> list = commonFoodMapper.search(name.trim(), 1);
        if (list.isEmpty()) {
            return Result.fail(404, "未找到该食物");
        }
        CommonFood food = list.get(0);
        Map<String, Object> data = new HashMap<>();
        data.put("foodName", food.getFoodName());
        data.put("caloriePer100g", food.getCaloriePer100g());
        data.put("category", food.getCategory());
        data.put("protein", food.getProtein());
        data.put("fat", food.getFat());
        data.put("carbohydrate", food.getCarbohydrate());
        return Result.success(data);
    }

    /**
     * 统计信息
     * GET /common-food/statistics
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics() {
        Map<String, Object> data = new HashMap<>();
        data.put("total", commonFoodMapper.selectCount(null));
        data.put("categories", commonFoodMapper.findAllCategories());
        return Result.success(data);
    }
}