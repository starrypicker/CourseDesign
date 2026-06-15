package com.sports.sales.controller;

import com.sports.sales.common.Result;
import com.sports.sales.entity.ChatMessage;
import com.sports.sales.service.ChatMessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    public ChatMessageController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    /**
     * 获取与指定用户的历史消息（最近50条）
     */
    @GetMapping("/history")
    public Result<List<ChatMessage>> getHistory(@RequestParam String targetCode,
                                                 HttpServletRequest request) {
        String myCode = getSenderCode(request);
        List<ChatMessage> history = chatMessageService.getHistory(myCode, targetCode, 50);
        return Result.success(history);
    }

    /**
     * 管理员获取所有聊过天的顾客列表
     */
    @GetMapping("/contacts")
    public Result<List<String>> getContacts(HttpServletRequest request) {
        checkAdmin(request);
        return Result.success(chatMessageService.getContacts());
    }

    /**
     * 标记某顾客的消息为已读
     */
    @PutMapping("/read/{senderCode}")
    public Result<Void> markAsRead(@PathVariable String senderCode, HttpServletRequest request) {
        checkAdmin(request);
        chatMessageService.markAsRead(senderCode);
        return Result.success();
    }

    /**
     * 获取未读消息数
     */
    @GetMapping("/unread-count")
    public Result<Map<String, Object>> getUnreadCount(HttpServletRequest request) {
        checkAdmin(request);
        int count = chatMessageService.countUnread();
        Map<String, Object> result = new HashMap<>();
        result.put("unreadCount", count);
        return Result.success(result);
    }

    private String getSenderCode(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if ("admin".equals(role)) {
            return "admin";
        }
        String customerCode = (String) request.getAttribute("customerCode");
        return customerCode != null ? customerCode : (String) request.getAttribute("username");
    }

    private void checkAdmin(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"admin".equals(role)) {
            throw new RuntimeException("无权限访问");
        }
    }
}
