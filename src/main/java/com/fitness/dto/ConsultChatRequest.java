package com.fitness.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * AI 咨询对话请求
 */
@Data
public class ConsultChatRequest {

    /** 用户消息 */
    private String message;

    /** 历史对话：[{role: user|assistant, content: xxx}] */
    private List<Map<String, String>> history;

    /** 可选：随消息一起发送的图片（data:image/jpeg;base64,...） */
    private String imageBase64;
}