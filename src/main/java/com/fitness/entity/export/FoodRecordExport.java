package com.fitness.entity.export;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;

import java.util.Date;

/**
 * 饮食记录导出实体
 */
@Data
public class FoodRecordExport {
    
    @ExcelProperty("序号")
    private Integer index;
    
    @ExcelProperty("用户名")
    private String username;
    
    @ExcelProperty("食物名称")
    private String foodName;
    
    @ExcelProperty("重量(g)")
    private Integer weight;
    
    @ExcelProperty("热量(kcal)")
    private Integer calorie;
    
    @ExcelProperty("记录时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private Date recordTime;
    
    @ExcelProperty("备注")
    private String remark;
}
