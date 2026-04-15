package com.videoplatform.social.ws;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.videoplatform.social.domain.ChatMessage;
import com.videoplatform.social.domain.ChatSession;
import com.videoplatform.social.mapper.ChatMessageMapper;
import com.videoplatform.social.mapper.ChatSessionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.videoplatform.social.constant.ChatConstant.*;

@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatMessageMapper chatMessageMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatBigModelHandler chatBigModelHandler;
    private final PPTBigModelHandler pptBigModelHandler;
    private final ImageBigModelHandler imageBigModelHandler;

    public static final Map<String, WebSocketSession> SESSION_MAP = new ConcurrentHashMap<>();
    public static final Map<String, String> USERID_TO_SESSIONID_MAP = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(
            ChatMessageMapper chatMessageMapper,
            ChatSessionMapper chatSessionMapper,
            ChatBigModelHandler chatBigModelHandler,
            PPTBigModelHandler pptBigModelHandler,
            ImageBigModelHandler imageBigModelHandler) {
        this.chatMessageMapper = chatMessageMapper;
        this.chatSessionMapper = chatSessionMapper;
        this.chatBigModelHandler = chatBigModelHandler;
        this.pptBigModelHandler = pptBigModelHandler;
        this.imageBigModelHandler = imageBigModelHandler;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        SESSION_MAP.put(session.getId(), session);

        JsonObject json = new JsonObject();
        json.addProperty(MESSAGE_TYPE, MESSAGE_TYPE_SESSIONID);
        json.addProperty(MESSAGE_TYPE_SESSIONID, session.getId());
        session.sendMessage(new TextMessage(json.toString()));

        log.info("WebSocket 连接建立: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("收到 WebSocket 原始消息: {}", message.getPayload());
        
        JsonObject json = JsonParser.parseString(message.getPayload()).getAsJsonObject();
        String type = json.get(MESSAGE_TYPE).getAsString();

        log.info("处理消息类型: {}, sessionId: {}", type, session.getId());

        switch (type) {
            case MESSAGE_TYPE_INIT:
                handleInit(session, json);
                break;
            case MESSAGE_TYPE_BIGMODEL:
                handleBigModel(session, json);
                break;
            case MESSAGE_TYPE_MESSAGE:
                handleMessage(session, json);
                break;
            case "ppt":
                handlePPT(session, json);
                break;
            case "image":
                handleImage(session, json);
                break;
            case MESSAGE_TYPE_REMOVE_SESSION:
                handleRemoveSession(json);
                break;
            default:
                log.warn("未知的消息类型: {}", type);
        }
    }

    private void handleInit(WebSocketSession session, JsonObject json) {
        String frontendSessionId = json.get(MESSAGE_TYPE_SESSIONID).getAsString();
        String userId = json.get(USER_IDENTITY).getAsString();

        USERID_TO_SESSIONID_MAP.put(userId, session.getId());

        log.info("用户 {} 初始化会话，前端sessionId: {}, 后端sessionId: {}", userId, frontendSessionId, session.getId());
    }

    private void handleBigModel(WebSocketSession session, JsonObject json) throws Exception {
        String question = json.get(MESSAGE_TYPE_BIGMODEL_QUESTION).getAsString();
        String userId = json.get(USER_IDENTITY).getAsString();

        log.info("========== 处理AI请求 ==========");
        log.info("用户ID: {}", userId);
        log.info("问题: {}", question);
        log.info("当前sessionId: {}", session.getId());
        log.info("会话是否打开: {}", session.isOpen());
        
        try {
            ChatMessage userMessage = new ChatMessage();
            userMessage.setSenderId(Long.parseLong(userId));
            userMessage.setReceiverId(0L);
            userMessage.setContent(question);
            userMessage.setMessageType(0);
            userMessage.setStatus(1);
            userMessage.setCreatedAt(LocalDateTime.now());
            chatMessageMapper.insert(userMessage);
            
            updateSession(userId, "0", question);
            
            log.info("开始调用chatBigModelHandler.send()");
            chatBigModelHandler.send(question, userId, session);
            log.info("========== AI请求处理完成 ==========");
        } catch (Exception e) {
            log.error("处理AI请求失败 - userId: {}, question: {}", userId, question, e);
            
            JsonObject errorMsg = new JsonObject();
            errorMsg.addProperty(MESSAGE_TYPE, MESSAGE_TYPE_BIGMODEL);
            errorMsg.addProperty(MESSAGE_CONTENT, "抱歉，AI服务出现错误：" + e.getMessage());
            errorMsg.addProperty("status", 2);
            errorMsg.addProperty("error", true);
            
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(errorMsg.toString()));
            }
        }
    }

    private void handlePPT(WebSocketSession session, JsonObject json) throws Exception {
        String description = json.get("description").getAsString();
        String userId = json.get(USER_IDENTITY).getAsString();

        log.info("用户 {} 请求生成 PPT: {}", userId, description);
        pptBigModelHandler.send(description, userId, session);
    }

    private void handleImage(WebSocketSession session, JsonObject json) throws Exception {
        String description = json.get("description").getAsString();
        String userId = json.get(USER_IDENTITY).getAsString();

        log.info("用户 {} 请求生成图片: {}", userId, description);
        imageBigModelHandler.send(description, userId, session);
    }

    private void handleMessage(WebSocketSession session, JsonObject json) throws IOException {
        String receiverId = json.get(RECEIVER_IDENTITY).getAsString();
        String content = json.get(MESSAGE_CONTENT).getAsString();
        String senderId = json.get(USER_IDENTITY).getAsString();

        log.info("用户 {} 发送消息给用户 {}: {}", senderId, receiverId, content);

        String receiverSessionId = USERID_TO_SESSIONID_MAP.get(receiverId);

        if (receiverSessionId != null && SESSION_MAP.containsKey(receiverSessionId)) {
            WebSocketSession receiverSession = SESSION_MAP.get(receiverSessionId);

            JsonObject jsonMessage = new JsonObject();
            jsonMessage.addProperty(MESSAGE_TYPE, MESSAGE_TYPE_MESSAGE);
            jsonMessage.addProperty(MESSAGE_CONTENT, content);
            jsonMessage.addProperty("senderId", senderId);

            if (receiverSession.isOpen()) {
                receiverSession.sendMessage(new TextMessage(jsonMessage.toString()));
            }
        }

        saveMessage(senderId, receiverId, content);
        updateSession(senderId, receiverId, content);
    }

    private void handleRemoveSession(JsonObject json) {
        String userId = json.get(USER_IDENTITY).getAsString();
        chatBigModelHandler.removeSession(userId);
        log.info("清除用户 {} 的AI会话历史", userId);
    }

    private void saveMessage(String senderId, String receiverId, String content) {
        ChatMessage message = new ChatMessage();
        message.setSenderId(Long.parseLong(senderId));
        message.setReceiverId(Long.parseLong(receiverId));
        message.setContent(content);
        message.setMessageType(0);
        message.setStatus(MESSAGE_STATUS_UNREAD);
        message.setCreatedAt(LocalDateTime.now());

        chatMessageMapper.insert(message);
    }

    private void updateSession(String senderId, String receiverId, String content) {
        ChatSession session = chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, Long.parseLong(receiverId))
                        .eq(ChatSession::getPartnerId, Long.parseLong(senderId))
        );

        if (session != null) {
            session.setLastMessageContent(content);
            session.setLastMessageTime(LocalDateTime.now());
            session.setUnreadCount(session.getUnreadCount() + 1);
            chatSessionMapper.updateById(session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SESSION_MAP.values().removeIf(s -> s.getId().equals(session.getId()));
        
        USERID_TO_SESSIONID_MAP.entrySet().removeIf(entry -> entry.getValue().equals(session.getId()));

        log.info("WebSocket 连接关闭: {}, 状态: {}", session.getId(), status);
        log.info("当前活跃连接数: {}", SESSION_MAP.size());
        log.info("当前用户映射数: {}", USERID_TO_SESSIONID_MAP.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket 传输错误: {}", session.getId(), exception);
    }
}
