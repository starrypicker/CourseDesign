package com.sports.sales.service.impl;

import com.sports.sales.entity.ChatMessage;
import com.sports.sales.mapper.ChatMessageMapper;
import com.sports.sales.service.ChatMessageService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageMapper chatMessageMapper;

    public ChatMessageServiceImpl(ChatMessageMapper chatMessageMapper) {
        this.chatMessageMapper = chatMessageMapper;
    }

    @Override
    public ChatMessage save(ChatMessage message) {
        chatMessageMapper.insert(message);
        return message;
    }

    @Override
    public List<ChatMessage> getHistory(String userCode1, String userCode2, int limit) {
        return chatMessageMapper.selectHistory(userCode1, userCode2, limit);
    }

    @Override
    public List<String> getContacts() {
        return chatMessageMapper.selectContacts();
    }

    @Override
    public int markAsRead(String senderCode) {
        return chatMessageMapper.markAsRead(senderCode);
    }

    @Override
    public int countUnread() {
        return chatMessageMapper.countUnread();
    }
}
