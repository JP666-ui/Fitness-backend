package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员消息提醒
 */
@Data
@TableName("notice_message")
public class NoticeMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发送人（管理员）ID */
    private Long senderId;

    /** 发送人昵称（冗余存储，列表展示不再关联查询） */
    private String senderName;

    /** 接收人ID；null 表示全体广播 */
    private Long receiverId;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 类型：1-通知 2-提醒 3-警告 */
    private Integer type;

    /** 0-未读 1-已读 */
    private Integer isRead;

    private LocalDateTime createTime;
}
