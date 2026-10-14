package com.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.entity.CommonFood;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 常见食物 Mapper
 */
@Mapper
public interface CommonFoodMapper extends BaseMapper<CommonFood> {

    /**
     * 按关键词搜索（名称或分类）
     */
    @Select("SELECT * FROM common_food WHERE food_name LIKE CONCAT('%', #{keyword}, '%') "
            + "OR category LIKE CONCAT('%', #{keyword}, '%') "
            + "ORDER BY category, id LIMIT #{limit}")
    List<CommonFood> search(@Param("keyword") String keyword, @Param("limit") Integer limit);

    /**
     * 按分类查询
     */
    @Select("SELECT * FROM common_food WHERE category = #{category} ORDER BY id")
    List<CommonFood> findByCategory(@Param("category") String category);

    /**
     * 获取所有分类
     */
    @Select("SELECT DISTINCT category FROM common_food WHERE category IS NOT NULL ORDER BY category")
    List<String> findAllCategories();
}