package com.sports.sales.service;

import com.sports.sales.entity.ChatMessage;

import java.util.List;

public interface ChatMessageService {

    ChatMessage save(ChatMessage message);

    List<ChatMessage> getHistory(String userCode1, String userCode2, int limit);

    List<String> getContacts();

    int markAsRead(String senderCode);

    int countUnread();
}
