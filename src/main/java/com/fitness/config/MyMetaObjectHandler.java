package com.fitness.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * MyBatis-Plus 字段自动填充处理器
 * <p>
 * 插入时自动填充 recordDate / createTime 等字段，避免每次手动赋值。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        Date now = new Date();
        LocalDateTime nowTime = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        // 通用时间字段（存在才填充）
        strictInsertFill(metaObject, "createTime", Date.class, now);
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, nowTime);
        strictInsertFill(metaObject, "recordTime", Date.class, now);
        strictInsertFill(metaObject, "exerciseTime", LocalDateTime.class, nowTime);
        strictInsertFill(metaObject, "recordDate", LocalDate.class, today);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, nowTime);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}