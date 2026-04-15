package com.videoplatform.social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videoplatform.social.domain.ChatSession;
import com.videoplatform.social.mapper.ChatSessionMapper;
import com.videoplatform.social.vo.response.ChatSessionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl {

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    public List<ChatSessionVO> getSessions(Long userId) {
        ChatSession aiSession = chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
                        .eq(ChatSession::getPartnerId, 0L)
        );

        if (aiSession == null) {
            aiSession = new ChatSession();
            aiSession.setUserId(userId);
            aiSession.setPartnerId(0L);
            aiSession.setLastMessageContent("你好！我是AI助手，有什么可以帮助你的吗？");
            aiSession.setLastMessageTime(LocalDateTime.now());
            aiSession.setUnreadCount(0);
            chatSessionMapper.insert(aiSession);
        }

        List<ChatSession> sessions = chatSessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
                        .orderByDesc(ChatSession::getUpdatedAt)
        );

        return sessions.stream().map(session -> {
            ChatSessionVO vo = new ChatSessionVO();
            BeanUtils.copyProperties(session, vo);
            return vo;
        }).collect(Collectors.toList());
    }
}
