package com.videoplatform.social.ws;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videoplatform.social.domain.ChatMessage;
import com.videoplatform.social.mapper.ChatMessageMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@RequiredArgsConstructor
public class PrivateChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatMessageMapper chatMessageMapper;
    private final Map<Long, WebSocketSession> onlineUsers = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = getUserId(session);
        if (userId != null) {
            onlineUsers.put(userId, session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String[] parts = message.getPayload().split("\\|", 3);
        if (parts.length < 3) {
            return;
        }
        Long senderId = Long.valueOf(parts[0]);
        Long receiverId = Long.valueOf(parts[1]);
        String content = parts[2];

        ChatMessage entity = new ChatMessage();
        entity.setSessionId(findSessionId(senderId, receiverId));
        entity.setSenderId(senderId);
        entity.setReceiverId(receiverId);
        entity.setContent(content);
        entity.setMessageType("TEXT");
        entity.setSendStatus("SENT");
        entity.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(entity);

        WebSocketSession receiver = onlineUsers.get(receiverId);
        if (receiver != null && receiver.isOpen()) {
            receiver.sendMessage(new TextMessage(content));
            entity.setSendStatus("DELIVERED");
            chatMessageMapper.updateById(entity);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserId(session);
        if (userId != null) {
            onlineUsers.remove(userId);
        }
    }

    private Long getUserId(WebSocketSession session) {
        String userId = session.getHandshakeHeaders().getFirst("X-User-Id");
        return userId == null ? null : Long.valueOf(userId);
    }

    private Long findSessionId(Long senderId, Long receiverId) {
        ChatMessage last = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                .and(wrapper -> wrapper.eq(ChatMessage::getSenderId, senderId).eq(ChatMessage::getReceiverId, receiverId)
                        .or().eq(ChatMessage::getSenderId, receiverId).eq(ChatMessage::getReceiverId, senderId))
                .last("limit 1"));
        return last == null ? 1L : last.getSessionId();
    }
}
