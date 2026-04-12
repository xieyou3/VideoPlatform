package com.videoplatform.social.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videoplatform.social.domain.ChatMessage;
import com.videoplatform.social.domain.ChatSession;
import com.videoplatform.social.domain.ChatSessionMember;
import com.videoplatform.social.domain.FriendRelation;
import com.videoplatform.social.dto.ChatViewResponse;
import com.videoplatform.social.dto.FriendViewResponse;
import com.videoplatform.social.dto.MessageViewResponse;
import com.videoplatform.social.mapper.ChatMessageMapper;
import com.videoplatform.social.mapper.ChatSessionMapper;
import com.videoplatform.social.mapper.ChatSessionMemberMapper;
import com.videoplatform.social.mapper.FriendRelationMapper;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SocialService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatSessionMemberMapper memberMapper;
    private final ChatMessageMapper messageMapper;
    private final FriendRelationMapper friendRelationMapper;
    private final JdbcTemplate jdbcTemplate;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public List<ChatViewResponse> listChats(Long userId) {
        List<ChatSessionMember> memberships = memberMapper.selectList(new LambdaQueryWrapper<ChatSessionMember>().eq(ChatSessionMember::getUserId, userId));
        Map<Long, ChatSessionMember> memberMap = memberships.stream().collect(Collectors.toMap(ChatSessionMember::getSessionId, v -> v));
        List<Long> sessionIds = memberships.stream().map(ChatSessionMember::getSessionId).toList();
        if (sessionIds.isEmpty()) {
            return List.of();
        }
        List<ChatSession> sessions = chatSessionMapper.selectBatchIds(sessionIds);
        Map<Long, String> userMap = loadUserMap();
        return sessions.stream().map(session -> {
            List<ChatMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getSessionId, session.getId())
                    .orderByAsc(ChatMessage::getCreatedAt));
            Long otherUserId = messages.stream().map(ChatMessage::getSenderId).filter(id -> !id.equals(userId)).findFirst().orElse(userId);
            return ChatViewResponse.builder()
                    .id(session.getId())
                    .name(userMap.getOrDefault(otherUserId, "好友"))
                    .avatar("https://picsum.photos/seed/chat" + otherUserId + "/128")
                    .lastMessage(session.getLastMessage())
                    .time(session.getLastMessageAt() == null ? "" : session.getLastMessageAt().format(TIME_FORMATTER))
                    .unread(memberMap.get(session.getId()).getUnreadCount())
                    .type(session.getType().toLowerCase())
                    .messages(messages.stream().map(msg -> MessageViewResponse.builder()
                            .id(msg.getId())
                            .senderId(msg.getSenderId().equals(userId) ? "me" : String.valueOf(msg.getSenderId()))
                            .text(msg.getContent())
                            .time(msg.getCreatedAt().format(TIME_FORMATTER))
                            .status(msg.getSendStatus().toLowerCase())
                            .build()).toList())
                    .build();
        }).toList();
    }

    public List<FriendViewResponse> listFriends(Long userId) {
        Map<Long, String> userMap = loadUserMap();
        return friendRelationMapper.selectList(new LambdaQueryWrapper<FriendRelation>()
                        .eq(FriendRelation::getUserId, userId)
                        .eq(FriendRelation::getStatus, "ACCEPTED"))
                .stream()
                .map(friend -> FriendViewResponse.builder()
                        .id(friend.getFriendUserId())
                        .name(userMap.getOrDefault(friend.getFriendUserId(), "好友"))
                        .avatar("https://picsum.photos/seed/friend" + friend.getFriendUserId() + "/128")
                        .status(friend.getFriendUserId() % 2 == 0 ? "online" : "offline")
                        .build())
                .toList();
    }

    private Map<Long, String> loadUserMap() {
        return jdbcTemplate.query("select id, username from users", rs -> {
            Map<Long, String> result = new java.util.HashMap<>();
            while (rs.next()) {
                result.put(rs.getLong("id"), rs.getString("username"));
            }
            return result;
        });
    }
}
