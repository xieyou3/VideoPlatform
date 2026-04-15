package com.videoplatform.social.ws;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videoplatform.social.config.XingHuoConfig;
import com.videoplatform.social.domain.ChatMessage;
import com.videoplatform.social.domain.ChatSession;
import com.videoplatform.social.mapper.ChatMessageMapper;
import com.videoplatform.social.mapper.ChatSessionMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.videoplatform.social.constant.ChatConstant.*;

@Slf4j
@Component
public class ChatBigModelHandler extends BaseBigModelHandler {

    private final XingHuoConfig config;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatSessionMapper chatSessionMapper;
    
    private static final Map<String, UserSession> USER_SESSION_MAP = new ConcurrentHashMap<>();

    public ChatBigModelHandler(XingHuoConfig config, ChatMessageMapper chatMessageMapper, ChatSessionMapper chatSessionMapper) {
        this.config = config;
        this.chatMessageMapper = chatMessageMapper;
        this.chatSessionMapper = chatSessionMapper;
    }

    @Override
    public void send(String question, String userId, WebSocketSession clientSession) throws Exception {
        log.info("========== 开始处理AI请求 ==========");
        log.info("用户ID: {}", userId);
        log.info("问题内容: {}", question);
        log.info("客户端SessionId: {}", clientSession.getId());
        log.info("讯飞配置 - AppId: {}, HostUrl: {}, Domain: {}", 
            config.getAppId(), config.getChat().getHostUrl(), config.getChat().getDomain());

        UserSession userSession = USER_SESSION_MAP.computeIfAbsent(userId, k -> new UserSession());
        
        userSession.setUserId(userId);
        userSession.setClientSession(clientSession);
        userSession.setNewQuestion(question);
        userSession.setTotalAnswer("");
        userSession.setCurrentWebSocket(null);

        List<RoleContent> historyList = loadHistoryFromDB(userId);
        log.info("加载历史消息数量: {}", historyList.size());
        
        log.info("正在创建与大模型的WebSocket连接...");
        WebSocket webSocket = createWebSocketConnection(
                config.getChat().getHostUrl(),
                config.getApiKey(),
                config.getApiSecret()
        );
        
        userSession.setCurrentWebSocket(webSocket);
        log.info("与大模型的WebSocket连接创建成功");

        run(webSocket, userId, historyList);
        log.info("========== AI请求处理完成 ==========");
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        super.onOpen(webSocket, response);
        log.info("✅ 通用对话大模型连接成功, StatusCode: {}", response != null ? response.code() : "null");
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        try {
            log.info("========== 收到大模型响应 ==========");
            log.debug("原始响应: {}", text);
            
            JsonParse jsonParse = gson.fromJson(text, JsonParse.class);

            if (jsonParse.header.code != 0) {
                log.error("大模型错误，错误码：{}, 错误信息: {}", jsonParse.header.code, jsonParse.header.sid);
                
                UserSession userSession = getUserSessionByWebSocket(webSocket);
                if (userSession != null && userSession.getClientSession() != null && userSession.getClientSession().isOpen()) {
                    JSONObject errorMsg = new JSONObject();
                    errorMsg.put(MESSAGE_TYPE, MESSAGE_TYPE_BIGMODEL);
                    errorMsg.put(MESSAGE_CONTENT, "大模型服务错误，错误码：" + jsonParse.header.code);
                    errorMsg.put("status", 2);
                    errorMsg.put("error", true);
                    userSession.getClientSession().sendMessage(new org.springframework.web.socket.TextMessage(errorMsg.toString()));
                }
                
                webSocket.close(1000, "");
                return;
            }

            List<Text> textList = jsonParse.payload.choices.text;
            log.info("解析到 {} 个文本片段", textList.size());
            
            for (Text temp : textList) {
                UserSession userSession = getUserSessionByWebSocket(webSocket);
                if (userSession == null) {
                    log.error("未找到对应的用户会话，webSocket: {}", webSocket);
                    continue;
                }

                log.debug("找到用户会话 - userId: {}, sessionId: {}",
                    userSession.getUserId(), 
                    userSession.getClientSession() != null ? userSession.getClientSession().getId() : "null");
                
                userSession.getTotalAnswer().append(temp.content);

                JSONObject jsonMessage = new JSONObject();
                jsonMessage.put(MESSAGE_TYPE, MESSAGE_TYPE_BIGMODEL);
                jsonMessage.put(MESSAGE_STATUS, jsonParse.header.status);
                jsonMessage.put(MESSAGE_CONTENT, temp.content);

                log.info("准备发送AI回复片段 - userId: {}, content长度: {}, status: {}",
                    userSession.getUserId(), temp.content.length(), jsonParse.header.status);

                if (userSession.getClientSession() != null && userSession.getClientSession().isOpen()) {
                    try {
                        userSession.getClientSession().sendMessage(new org.springframework.web.socket.TextMessage(jsonMessage.toString()));
                        log.debug("成功发送AI回复片段到 sessionId: {}", userSession.getClientSession().getId());
                    } catch (Exception e) {
                        log.error("发送消息到客户端失败 - sessionId: {}", userSession.getClientSession().getId(), e);
                    }
                } else {
                    log.error("客户端会话不可用 - session={}, isOpen={}",
                        userSession.getClientSession(), 
                        userSession.getClientSession() != null ? userSession.getClientSession().isOpen() : "null");
                }
            }

            if (jsonParse.header.status == 2) {
                UserSession userSession = getUserSessionByWebSocket(webSocket);
                if (userSession != null) {
                    log.info("用户 {} 的大模型回复完成，总长度: {}",
                        userSession.getUserId(), userSession.getTotalAnswer().length());
                    
                    String completeAnswer = userSession.getTotalAnswer().toString();
                    
                    saveToHistory(userSession.getUserId(), userSession.getNewQuestion(), completeAnswer);
                    
                    userSession.reset();
                }
                
                log.info("回复完成，关闭与大模型的连接");
                webSocket.close(1000, "");
            }
        } catch (Exception e) {
            log.error("处理大模型消息失败", e);
        }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        super.onFailure(webSocket, t, response);
        log.error("大模型连接失败 - StatusCode: {}, Message: {}",
            response != null ? response.code() : "null", 
            t.getMessage());
        log.error("失败详情: ", t);
    }

    public void run(WebSocket webSocket, String userId, List<RoleContent> historyList) {
        try {
            JSONObject requestJson = new JSONObject();

            JSONObject header = new JSONObject();
            header.put(MODEL_APP_ID, config.getAppId());
            header.put(SESSION_UID, UUID.randomUUID().toString().substring(0, 10));

            JSONObject parameter = new JSONObject();
            JSONObject chat = new JSONObject();
            chat.put(MODEL_DOMAIN, config.getChat().getDomain());
            chat.put(MODEL_TEMPERATURE, 0.8);
            chat.put(MODEL_MAX_TOKENS, 4096);
            parameter.put(MODEL_PARAMETER, chat);

            JSONObject payload = new JSONObject();
            JSONObject message = new JSONObject();
            JSONArray text = new JSONArray();

            if (!historyList.isEmpty()) {
                for (RoleContent tempRoleContent : historyList) {
                    text.add(JSON.toJSON(tempRoleContent));
                }
            }

            UserSession userSession = USER_SESSION_MAP.get(userId);
            if (userSession != null) {
                RoleContent roleContent = new RoleContent();
                roleContent.role = USER_ROLE;
                roleContent.content = userSession.getNewQuestion();
                text.add(JSON.toJSON(roleContent));
            }

            message.put(MESSAGE_TEXT, text);
            payload.put(PAYLOAD_MESSAGE, message);

            requestJson.put(REQUEST_HEADER, header);
            requestJson.put(REQUEST_PARAMTER, parameter);
            requestJson.put(REQUEST_PAYLOAD, payload);

            log.info("发送请求到大模型 - userId: {}, 请求内容: {}", userId, requestJson.toJSONString());
            webSocket.send(requestJson.toString());
            
            log.info("请求已发送，等待大模型响应...（不要主动关闭连接）");
            
        } catch (Exception e) {
            log.error("发送消息到大模型失败 - userId: {}", userId, e);
            
            UserSession userSession = USER_SESSION_MAP.get(userId);
            if (userSession != null && userSession.getClientSession() != null && userSession.getClientSession().isOpen()) {
                try {
                    JSONObject errorMsg = new JSONObject();
                    errorMsg.put(MESSAGE_TYPE, MESSAGE_TYPE_BIGMODEL);
                    errorMsg.put(MESSAGE_CONTENT, "发送请求失败：" + e.getMessage());
                    errorMsg.put("status", 2);
                    errorMsg.put("error", true);
                    userSession.getClientSession().sendMessage(new org.springframework.web.socket.TextMessage(errorMsg.toString()));
                } catch (Exception ex) {
                    log.error("发送错误消息到客户端失败", ex);
                }
            }
        }
    }

    public Boolean removeSession(String userId) {
        USER_SESSION_MAP.remove(userId);
        
        chatMessageMapper.delete(
            new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSenderId, 0L)
                .eq(ChatMessage::getReceiverId, Long.parseLong(userId))
        );
        
        log.info("清除用户 {} 的AI会话历史", userId);
        return true;
    }

    private UserSession getUserSessionByWebSocket(WebSocket webSocket) {
        for (UserSession session : USER_SESSION_MAP.values()) {
            if (session.getCurrentWebSocket() == webSocket) {
                return session;
            }
        }
        return null;
    }

    private List<RoleContent> loadHistoryFromDB(String userId) {
        List<ChatMessage> messages = chatMessageMapper.selectList(
            new LambdaQueryWrapper<ChatMessage>()
                .and(wrapper -> wrapper
                    .and(w -> w.eq(ChatMessage::getSenderId, Long.parseLong(userId))
                            .eq(ChatMessage::getReceiverId, 0L))
                    .or(w -> w.eq(ChatMessage::getSenderId, 0L)
                            .eq(ChatMessage::getReceiverId, Long.parseLong(userId)))
                )
                .orderByAsc(ChatMessage::getCreatedAt)
                .last("LIMIT 20")
        );

        List<RoleContent> historyList = new ArrayList<>();
        for (ChatMessage msg : messages) {
            RoleContent roleContent = new RoleContent();
            if (msg.getSenderId().equals(Long.parseLong(userId))) {
                roleContent.role = USER_ROLE;
            } else {
                roleContent.role = ASSISTANT_ROLE;
            }
            roleContent.content = msg.getContent();
            historyList.add(roleContent);
        }

        return historyList;
    }

    private void saveToHistory(String userId, String question, String answer) {
        ChatMessage aiMessage = new ChatMessage();
        aiMessage.setSenderId(0L);
        aiMessage.setReceiverId(Long.parseLong(userId));
        aiMessage.setContent(answer);
        aiMessage.setMessageType(0);
        aiMessage.setStatus(1);
        aiMessage.setAiStatus(2);
        aiMessage.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(aiMessage);

        ChatSession session = chatSessionMapper.selectOne(
            new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, Long.parseLong(userId))
                .eq(ChatSession::getPartnerId, 0L)
        );
        
        if (session != null) {
            session.setLastMessageContent(answer.length() > 2000 ? answer.substring(0, 2000) : answer);
            session.setLastMessageTime(LocalDateTime.now());
            chatSessionMapper.updateById(session);
        }

        log.info("保存用户 {} 的AI回答到数据库", userId);
    }

    static class UserSession {
        private String userId;
        private WebSocketSession clientSession;
        private WebSocket currentWebSocket;
        private String newQuestion;
        private StringBuilder totalAnswer;

        public UserSession() {
            this.totalAnswer = new StringBuilder();
        }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public WebSocketSession getClientSession() { return clientSession; }
        public void setClientSession(WebSocketSession clientSession) { this.clientSession = clientSession; }
        
        public WebSocket getCurrentWebSocket() { return currentWebSocket; }
        public void setCurrentWebSocket(WebSocket currentWebSocket) { this.currentWebSocket = currentWebSocket; }
        
        public String getNewQuestion() { return newQuestion; }
        public void setNewQuestion(String newQuestion) { this.newQuestion = newQuestion; }
        
        public StringBuilder getTotalAnswer() { return totalAnswer; }
        public void setTotalAnswer(String totalAnswer) { this.totalAnswer = new StringBuilder(totalAnswer); }
        
        public void reset() {
            this.newQuestion = "";
            this.totalAnswer = new StringBuilder();
            this.currentWebSocket = null;
        }
    }

    static class JsonParse {
        Header header;
        Payload payload;
    }

    static class Header {
        int code;
        int status;
        String sid;
    }

    static class Payload {
        Choices choices;
    }

    static class Choices {
        List<Text> text;
    }

    static class Text {
        String role;
        String content;
    }

    static class RoleContent {
        String role;
        String content;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
