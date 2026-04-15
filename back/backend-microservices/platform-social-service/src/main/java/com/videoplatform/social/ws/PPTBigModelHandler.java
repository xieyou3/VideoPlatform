package com.videoplatform.social.ws;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videoplatform.social.config.XingHuoConfig;
import com.videoplatform.social.domain.ChatMessage;
import com.videoplatform.social.domain.ChatSession;
import com.videoplatform.social.mapper.ChatMessageMapper;
import com.videoplatform.social.mapper.ChatSessionMapper;
import com.videoplatform.social.util.PPTAuthAlgorithm;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class PPTBigModelHandler extends BaseBigModelHandler {

    private final XingHuoConfig config;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatSessionMapper chatSessionMapper;

    public PPTBigModelHandler(XingHuoConfig config, ChatMessageMapper chatMessageMapper, ChatSessionMapper chatSessionMapper) {
        this.config = config;
        this.chatMessageMapper = chatMessageMapper;
        this.chatSessionMapper = chatSessionMapper;
    }

    @Override
    public void send(String question, String userId, WebSocketSession clientSession) throws Exception {
        this.userId = userId;
        this.clientSession = clientSession;

        log.info("用户 {} 请求生成 PPT: {}", userId, question);
        log.info("AppId: {}", config.getAppId());

        try {
            String apiUrl = "https://zwapi.xfyun.cn/api/ppt/v2/create";
            long timestamp = System.currentTimeMillis() / 1000;
            String signature = PPTAuthAlgorithm.getSignature(config.getAppId(), config.getApiSecret(), timestamp);

            if (signature == null) {
                log.error("PPT 签名生成失败");
                sendErrorMessage("PPT 签名生成失败");
                return;
            }

            log.info("PPT API 地址: {}", apiUrl);
            log.info("时间戳: {}, 签名: {}", timestamp, signature);

            RequestBody requestBody = buildPPTMultipartRequest(question);
            log.info("PPT 请求参数 - query: {}", question);

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("appId", config.getAppId())
                    .addHeader("timestamp", String.valueOf(timestamp))
                    .addHeader("signature", signature)
                    .post(requestBody)
                    .build();

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            log.info("开始调用 PPT API...");
            callPPTAPI(client, request, question);
        } catch (Exception e) {
            log.error("构建 PPT 请求失败", e);
            sendErrorMessage("PPT 生成请求失败：" + e.getMessage());
        }
    }

    private RequestBody buildPPTMultipartRequest(String description) {
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("query", description)
                .addFormDataPart("language", "cn")
                .addFormDataPart("isFigure", "true")
                .addFormDataPart("aiImage", "normal")
                .addFormDataPart("search", "false")
                .addFormDataPart("isCardNote", "false");
        
        log.debug("构建的 PPT multipart 请求");
        
        return builder.build();
    }

    private void callPPTAPI(OkHttpClient client, Request request, String description) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("PPT 生成请求失败 - 网络错误", e);
                sendErrorMessage("PPT 生成服务连接失败：" + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    log.info("收到 PPT API 响应 - StatusCode: {}", response.code());

                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "无响应体";
                        log.error("PPT API 返回错误 - StatusCode: {}, Body: {}", response.code(), errorBody);
                        sendErrorMessage("PPT 生成服务错误：" + response.code());
                        return;
                    }

                    if (response.body() == null) {
                        log.error("PPT API 响应体为空");
                        sendErrorMessage("PPT 生成服务返回空响应");
                        return;
                    }

                    String result = response.body().string();
                    log.info("PPT 生成原始响应: {}", result);

                    JSONObject jsonResponse = JSONObject.parseObject(result);
                    
                    Boolean flag = jsonResponse.getBoolean("flag");
                    Integer code = jsonResponse.getInteger("code");
                    
                    if (flag != null && !flag) {
                        String desc = jsonResponse.getString("desc");
                        log.error("PPT 生成业务错误 - Code: {}, Desc: {}", code, desc);
                        sendErrorMessage("PPT 生成失败：" + desc);
                        return;
                    }

                    JSONObject data = jsonResponse.getJSONObject("data");
                    if (data == null) {
                        log.error("响应中未找到 data 字段");
                        sendErrorMessage("PPT 生成响应格式错误");
                        return;
                    }

                    String sid = data.getString("sid");
                    String coverImgSrc = data.getString("coverImgSrc");
                    String title = data.getString("title");
                    
                    if (sid == null || sid.isEmpty()) {
                        log.error("响应中未找到 sid 字段");
                        sendErrorMessage("PPT 生成成功但未返回任务ID");
                        return;
                    }

                    log.info("PPT 生成任务已提交 - SID: {}, 标题: {}, 封面: {}", sid, title, coverImgSrc);

                    savePPTMessage(userId, sid, title, coverImgSrc);

                    JSONObject jsonMessage = new JSONObject();
                    jsonMessage.put("type", "ppt");
                    jsonMessage.put("pptSid", sid);
                    jsonMessage.put("title", title);
                    jsonMessage.put("coverImg", coverImgSrc);
                    jsonMessage.put("status", 2);

                    if (clientSession != null && clientSession.isOpen()) {
                        clientSession.sendMessage(new TextMessage(jsonMessage.toString()));
                        log.info("已将 PPT 任务信息发送到前端");
                    } else {
                        log.error("客户端 WebSocket 会话不可用");
                    }
                } catch (Exception e) {
                    log.error("处理 PPT 生成响应失败", e);
                    sendErrorMessage("处理 PPT 生成响应失败：" + e.getMessage());
                }
            }
        });
    }

    private void savePPTMessage(String userId, String sid, String title, String coverImg) {
        try {
            ChatMessage aiMessage = new ChatMessage();
            aiMessage.setSenderId(0L);
            aiMessage.setReceiverId(Long.parseLong(userId));
            
            JSONObject pptData = new JSONObject();
            pptData.put("sid", sid);
            pptData.put("title", title);
            pptData.put("coverImg", coverImg);
            aiMessage.setContent(pptData.toJSONString());
            
            aiMessage.setMessageType(3);
            aiMessage.setStatus(1);
            aiMessage.setCreatedAt(LocalDateTime.now());
            
            chatMessageMapper.insert(aiMessage);

            ChatSession session = chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>()
                    .eq(ChatSession::getUserId, Long.parseLong(userId))
                    .eq(ChatSession::getPartnerId, 0L)
            );
            
            if (session != null) {
                String previewText = "[PPT] " + title;
                if (previewText.length() > 2000) {
                    previewText = previewText.substring(0, 2000);
                }
                
                session.setLastMessageContent(previewText);
                session.setLastMessageTime(LocalDateTime.now());
                chatSessionMapper.updateById(session);
            }

            log.info("已保存 PPT 消息到数据库 - userId: {}, sid: {}", userId, sid);
        } catch (Exception e) {
            log.error("保存 PPT 消息失败 - userId: {}", userId, e);
        }
    }

    private void sendErrorMessage(String errorMessage) {
        try {
            if (clientSession != null && clientSession.isOpen()) {
                JSONObject errorMsg = new JSONObject();
                errorMsg.put("type", "ppt");
                errorMsg.put("error", true);
                errorMsg.put("message", errorMessage);
                errorMsg.put("status", 2);
                clientSession.sendMessage(new TextMessage(errorMsg.toString()));
                log.info("已发送错误消息到前端: {}", errorMessage);
            } else {
                log.error("无法发送错误消息 - 客户端会话不可用");
            }
        } catch (Exception e) {
            log.error("发送错误消息失败", e);
        }
    }

    @Override
    public void onMessage(okhttp3.WebSocket webSocket, String text) {
    }

    @Override
    public void onFailure(okhttp3.WebSocket webSocket, Throwable t, okhttp3.Response response) {
    }
}
