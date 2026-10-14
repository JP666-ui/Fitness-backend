package com.fitness.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fitness.entity.FoodRecord;
import com.fitness.mapper.FoodRecordMapper;
import com.fitness.service.FoodRecordService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 饮食记录 Service实现类
 */
@Service
public class FoodRecordServiceImpl extends ServiceImpl<FoodRecordMapper, FoodRecord>
        implements FoodRecordService {

    @Override
    public Integer getTodayCalorie(Long userId) {
        if (userId == null) {
            return 0;
        }

        // FoodRecord.recordTime 为 java.util.Date 类型
        Date startOfDay = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endOfDay = Date.from(LocalDate.now().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());

        return lambdaQuery()
                .eq(FoodRecord::getUserId, userId)
                .ge(FoodRecord::getRecordTime, startOfDay)
                .le(FoodRecord::getRecordTime, endOfDay)
                .list()
                .stream()
                .map(FoodRecord::getCalorie)
                .filter(c -> c != null)
                .mapToInt(Integer::intValue)
                .sum();
    }
}