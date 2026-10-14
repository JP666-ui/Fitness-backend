package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 常见食物热量表
 */
@Data
@TableName("common_food")
public class CommonFood {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 食物名称 */
    private String foodName;

    /** 每 100g 热量(kcal)，数据库列为 calorie_per_100g */
    @TableField("calorie_per_100g")
    private Integer caloriePer100g;

    /** 分类：主食/肉类/蔬菜/炒菜/水果/饮品/零食 */
    private String category;

    /** 蛋白质(g/100g) */
    private BigDecimal protein;

    /** 脂肪(g/100g) */
    private BigDecimal fat;

    /** 碳水化合物(g/100g) */
    private BigDecimal carbohydrate;

    /** 备注 */
    private String remark;
}