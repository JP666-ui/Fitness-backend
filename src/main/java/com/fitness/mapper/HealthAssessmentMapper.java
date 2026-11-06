package com.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.entity.HealthAssessment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 健康评测 Mapper
 */
@Mapper
public interface HealthAssessmentMapper extends BaseMapper<HealthAssessment> {
}
