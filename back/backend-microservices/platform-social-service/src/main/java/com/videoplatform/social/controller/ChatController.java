package com.videoplatform.social.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videoplatform.common.api.ApiResponse;
import com.videoplatform.social.domain.ChatMessage;
import com.videoplatform.social.domain.ChatSession;
import com.videoplatform.social.mapper.ChatMessageMapper;
import com.videoplatform.social.mapper.ChatSessionMapper;
import com.videoplatform.social.vo.response.ChatMessageVO;
import com.videoplatform.social.vo.response.ChatSessionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;

    @GetMapping("/sessions/{userId}")
    public ApiResponse<List<ChatSessionVO>> getSessions(@PathVariable Long userId) {
        log.info("获取用户 {} 的会话列表", userId);

        ChatSession aiSession = chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
                        .eq(ChatSession::getPartnerId, 0L)
        );

        if (aiSession == null) {
            log.info("为用户 {} 创建 AI 助手会话", userId);
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

        List<ChatSessionVO> result = sessions.stream().map(session -> {
            ChatSessionVO vo = new ChatSessionVO();
            BeanUtils.copyProperties(session, vo);
            
            if (session.getPartnerId() == 0) {
                vo.setPartnerName("AI 助手");
                vo.setPartnerAvatar("");
            }
            
            return vo;
        }).collect(Collectors.toList());

        return ApiResponse.success(result);
    }

    @GetMapping("/history/{userId}/{partnerId}")
    public ApiResponse<List<ChatMessageVO>> getHistory(
            @PathVariable Long userId,
            @PathVariable Long partnerId) {
        log.info("获取用户 {} 和 {} 的历史消息", userId, partnerId);

        List<ChatMessage> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .and(wrapper -> wrapper
                                .and(w -> w.eq(ChatMessage::getSenderId, userId)
                                        .eq(ChatMessage::getReceiverId, partnerId))
                                .or(w -> w.eq(ChatMessage::getSenderId, partnerId)
                                        .eq(ChatMessage::getReceiverId, userId))
                        )
                        .orderByAsc(ChatMessage::getCreatedAt)
        );

        List<ChatMessageVO> result = messages.stream().map(message -> {
            ChatMessageVO vo = new ChatMessageVO();
            BeanUtils.copyProperties(message, vo);
            return vo;
        }).collect(Collectors.toList());

        return ApiResponse.success(result);
    }

    @PostMapping("/session/create")
    public ApiResponse<Long> createSession(@RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        Long receiverId = request.get("receiverId");

        log.info("创建会话: userId={}, receiverId={}", userId, receiverId);

        ChatSession existingSession = chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
                        .eq(ChatSession::getPartnerId, receiverId)
        );

        if (existingSession != null) {
            return ApiResponse.success(existingSession.getId());
        }

        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setPartnerId(receiverId);
        session.setUnreadCount(0);
        session.setLastMessageTime(LocalDateTime.now());

        chatSessionMapper.insert(session);
        return ApiResponse.success(session.getId());
    }

    @PostMapping("/read")
    public ApiResponse<Void> markAsRead(@RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        Long partnerId = request.get("partnerId");

        log.info("标记已读: userId={}, partnerId={}", userId, partnerId);

        chatMessageMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ChatMessage>()
                        .eq(ChatMessage::getSenderId, partnerId)
                        .eq(ChatMessage::getReceiverId, userId)
                        .eq(ChatMessage::getStatus, 0)
                        .set(ChatMessage::getStatus, 1)
        );

        ChatSession session = chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
                        .eq(ChatSession::getPartnerId, partnerId)
        );

        if (session != null) {
            session.setUnreadCount(0);
            chatSessionMapper.updateById(session);
        }

        return ApiResponse.success(null);
    }
}
