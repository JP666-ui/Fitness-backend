package com.fitness.entity.export;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 运动记录导出实体
 */
@Data
public class ExerciseRecordExport {
    
    @ExcelProperty("序号")
    private Integer index;
    
    @ExcelProperty("用户名")
    private String username;
    
    @ExcelProperty("运动类型")
    private String exerciseName;
    
    @ExcelProperty("时长(分钟)")
    private Integer duration;
    
    @ExcelProperty("消耗热量(kcal)")
    private Integer calorie;
    
    @ExcelProperty("距离(km)")
    private Double distance;
    
    @ExcelProperty("运动时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime exerciseTime;
    
    @ExcelProperty("备注")
    private String remark;
}
