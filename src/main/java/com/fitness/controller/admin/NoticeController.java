package com.fitness.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fitness.common.Result;
import com.fitness.common.annotation.Idempotent;
import com.fitness.entity.NoticeMessage;
import com.fitness.entity.User;
import com.fitness.mapper.NoticeMessageMapper;
import com.fitness.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息提醒 Controller
 * <p>
 * 两类角色共用一个 Controller，靠路径前缀区分权限：
 * <ul>
 *     <li>/admin/notice/**  —— 仅管理员（JwtInterceptor 统一拦截）：发送、查看发送记录</li>
 *     <li>/notice/**        —— 所有登录用户：收件箱、未读数、标记已读</li>
 * </ul>
 */
@RestController
public class NoticeController {

    @Autowired
    private NoticeMessageMapper noticeMessageMapper;

    @Autowired
    private UserService userService;

    // ==================== 管理员：发送 ====================

    /**
     * 发送消息（指定用户或全体广播）
     * POST /admin/notice/send
     * <p>
     * 入参：{ "receiverId": 2, "title": "...", "content": "...", "type": 2 }
     * receiverId 为 null 或 0 表示全体广播。
     */
    @Idempotent(seconds = 3, message = "消息发送过于频繁")
    @PostMapping("/admin/notice/send")
    public Result<NoticeMessage> send(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long senderId = (Long) request.getAttribute("userId");
        User sender = userService.getById(senderId);
        if (sender == null) {
            return Result.fail(401, "发送人不存在");
        }

        String title = str(body.get("title"));
        String content = str(body.get("content"));
        if (title == null || title.isEmpty() || content == null || content.isEmpty()) {
            return Result.fail(400, "标题和内容不能为空");
        }
        if (title.length() > 100) {
            return Result.fail(400, "标题过长（最多 100 字）");
        }
        if (content.length() > 500) {
            return Result.fail(400, "内容过长（最多 500 字）");
        }

        // 定向发送时校验接收人存在
        Long receiverId = null;
        Object rid = body.get("receiverId");
        if (rid instanceof Number && ((Number) rid).longValue() > 0) {
            receiverId = ((Number) rid).longValue();
            User receiver = userService.getById(receiverId);
            if (receiver == null) {
                return Result.fail(404, "接收用户不存在");
            }
        }

        NoticeMessage msg = new NoticeMessage();
        msg.setSenderId(senderId);
        msg.setSenderName(sender.getNickname() != null ? sender.getNickname() : sender.getUsername());
        msg.setReceiverId(receiverId);
        msg.setTitle(title);
        msg.setContent(content);
        Object type = body.get("type");
        msg.setType(type instanceof Number ? ((Number) type).intValue() : 1);
        msg.setIsRead(0);

        noticeMessageMapper.insert(msg);
        return Result.success(msg);
    }

    /**
     * 管理员查看自己发出的消息记录
     * GET /admin/notice/sent
     */
    @GetMapping("/admin/notice/sent")
    public Result<List<NoticeMessage>> sent(HttpServletRequest request,
                                            @RequestParam(defaultValue = "50") Integer limit) {
        Long senderId = (Long) request.getAttribute("userId");
        if (limit == null || limit <= 0) limit = 50;
        if (limit > 200) limit = 200;

        List<NoticeMessage> list = noticeMessageMapper.selectList(
                new QueryWrapper<NoticeMessage>()
                        .eq("sender_id", senderId)
                        .orderByDesc("create_time")
                        .last("LIMIT " + limit));
        return Result.success(list);
    }

    /**
     * 管理员查看某用户的消息（含定向 + 该用户可见的广播）
     * GET /admin/notice/user/{userId}
     */
    @GetMapping("/admin/notice/user/{userId}")
    public Result<List<NoticeMessage>> byUser(@PathVariable Long userId,
                                              @RequestParam(defaultValue = "50") Integer limit) {
        if (limit == null || limit <= 0) limit = 50;
        if (limit > 200) limit = 200;

        List<NoticeMessage> list = noticeMessageMapper.selectList(
                new QueryWrapper<NoticeMessage>()
                        .and(w -> w.eq("receiver_id", userId).or().isNull("receiver_id"))
                        .orderByDesc("create_time")
                        .last("LIMIT " + limit));
        return Result.success(list);
    }

    // ==================== 所有用户：收件箱 ====================

    /**
     * 我的收件箱
     * GET /notice/inbox
     */
    @GetMapping("/notice/inbox")
    public Result<List<NoticeMessage>> inbox(HttpServletRequest request,
                                             @RequestParam(defaultValue = "50") Integer limit) {
        Long userId = (Long) request.getAttribute("userId");
        if (limit == null || limit <= 0) limit = 50;
        if (limit > 200) limit = 200;

        List<NoticeMessage> list = noticeMessageMapper.findInbox(userId, limit);
        return Result.success(list);
    }

    /**
     * 我的未读数（前端铃铛轮询用）
     * GET /notice/unread
     */
    @GetMapping("/notice/unread")
    public Result<Map<String, Object>> unread(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        int count = noticeMessageMapper.countUnread(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("unread", count);
        return Result.success(data);
    }

    /**
     * 标记单条已读
     * PUT /notice/{id}/read
     */
    @PutMapping("/notice/{id}/read")
    public Result<Void> markRead(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        NoticeMessage msg = noticeMessageMapper.selectById(id);
        if (msg == null) {
            return Result.fail(404, "消息不存在");
        }
        // 只能标记发给自己的或广播
        if (msg.getReceiverId() != null && !msg.getReceiverId().equals(userId)) {
            return Result.fail(403, "无权操作该消息");
        }
        msg.setIsRead(1);
        noticeMessageMapper.updateById(msg);
        return Result.success(null);
    }

    /**
     * 全部标记已读
     * PUT /notice/read-all
     */
    @PutMapping("/notice/read-all")
    public Result<Map<String, Object>> markAllRead(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        List<NoticeMessage> unreadList = noticeMessageMapper.selectList(
                new QueryWrapper<NoticeMessage>()
                        .eq("is_read", 0)
                        .and(w -> w.eq("receiver_id", userId)
                                .or(ww -> ww.isNull("receiver_id").ne("sender_id", userId))));
        for (NoticeMessage m : unreadList) {
            m.setIsRead(1);
            noticeMessageMapper.updateById(m);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("marked", unreadList.size());
        return Result.success(data);
    }

    private String str(Object o) {
        return o == null ? null : o.toString().trim();
    }
}
