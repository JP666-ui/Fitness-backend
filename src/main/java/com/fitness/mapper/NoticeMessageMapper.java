package com.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.entity.NoticeMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 消息提醒 Mapper
 */
@Mapper
public interface NoticeMessageMapper extends BaseMapper<NoticeMessage> {

    /**
     * 收件箱：定向发给我的 + 全体广播（receiver_id IS NULL），排除我发出的广播
     */
    @Select("SELECT * FROM notice_message "
            + "WHERE receiver_id = #{userId} "
            + "   OR (receiver_id IS NULL AND sender_id != #{userId}) "
            + "ORDER BY create_time DESC LIMIT #{limit}")
    List<NoticeMessage> findInbox(@Param("userId") Long userId, @Param("limit") Integer limit);

    /**
     * 未读数（同上口径）
     */
    @Select("SELECT COUNT(*) FROM notice_message "
            + "WHERE is_read = 0 "
            + "  AND (receiver_id = #{userId} OR (receiver_id IS NULL AND sender_id != #{userId}))")
    int countUnread(@Param("userId") Long userId);
}
