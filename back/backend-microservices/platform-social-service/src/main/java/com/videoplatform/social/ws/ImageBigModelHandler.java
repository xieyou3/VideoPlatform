package com.videoplatform.social.ws;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videoplatform.social.config.XingHuoConfig;
import com.videoplatform.social.domain.ChatMessage;
import com.videoplatform.social.domain.ChatSession;
import com.videoplatform.social.mapper.ChatMessageMapper;
import com.videoplatform.social.mapper.ChatSessionMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ImageBigModelHandler extends BaseBigModelHandler {

    private final XingHuoConfig config;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatSessionMapper chatSessionMapper;

    public ImageBigModelHandler(XingHuoConfig config, ChatMessageMapper chatMessageMapper, ChatSessionMapper chatSessionMapper) {
        this.config = config;
        this.chatMessageMapper = chatMessageMapper;
        this.chatSessionMapper = chatSessionMapper;
    }

    @Override
    public void send(String question, String userId, WebSocketSession clientSession) throws Exception {
        this.userId = userId;
        this.clientSession = clientSession;

        log.info("========== 开始处理图片生成请求 ==========");
        log.info("用户ID: {}", userId);
        log.info("描述: {}", question);
        log.info("图片生成API地址: {}", config.getImage().getHostUrl());
        log.info("AppId: {}", config.getAppId());

        generateImage(question);
        
        log.info("========== 图片生成请求已提交 ==========");
    }

    private void generateImage(String description) {
        try {
            JSONObject requestBody = buildImageRequest(description);

            RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), 
                requestBody.toJSONString()
            );

            String signedUrl = buildSignedUrl(config.getImage().getHostUrl());
            
            log.info("发送HTTP请求到讯飞图片生成API...");
            log.info("签名后的URL: {}", signedUrl);
            log.debug("请求体: {}", requestBody.toJSONString());

            Request request = new Request.Builder()
                    .url(signedUrl)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    log.error("❌ 图片生成请求失败 - 网络错误", e);
                    sendErrorMessage("图片生成请求失败：" + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        log.info("收到讯飞API响应 - StatusCode: {}", response.code());
                        
                        if (!response.isSuccessful()) {
                            String errorBody = response.body() != null ? response.body().string() : "无响应体";
                            log.error("图片生成API返回错误 - StatusCode: {}, Body: {}", response.code(), errorBody);
                            sendErrorMessage("图片生成服务错误：" + response.code());
                            return;
                        }

                        if (response.body() == null) {
                            log.error("图片生成API响应体为空");
                            sendErrorMessage("图片生成服务返回空响应");
                            return;
                        }

                        String result = response.body().string();
                        
                        JSONObject jsonResponse = JSONObject.parseObject(result);
                        JSONObject header = jsonResponse.getJSONObject("header");
                        Integer code = header != null ? header.getInteger("code") : null;
                        
                        if (code != null && code == 0) {
                            log.info("图片生成API响应成功（响应数据过大，省略完整日志）");
                        } else {
                            log.info("图片生成API原始响应: {}", result);
                        }

                        if (header == null) {
                            log.error("响应中未找到 header 字段");
                            sendErrorMessage("图片生成响应格式错误");
                            return;
                        }

                        if (code != null && code != 0) {
                            String message = header.getString("message");
                            log.error("图片生成业务错误 - Code: {}, Message: {}", code, message);
                            sendErrorMessage("图片生成失败：" + message);
                            return;
                        }

                        JSONObject payload = jsonResponse.getJSONObject("payload");
                        if (payload == null) {
                            log.error("响应中未找到 payload 字段");
                            sendErrorMessage("图片生成响应格式错误");
                            return;
                        }

                        JSONObject choices = payload.getJSONObject("choices");
                        if (choices == null) {
                            log.error("响应中未找到 choices 字段");
                            sendErrorMessage("图片生成响应格式错误");
                            return;
                        }

                        com.alibaba.fastjson.JSONArray textArray = choices.getJSONArray("text");
                        if (textArray == null || textArray.isEmpty()) {
                            log.error("响应中未找到 text 数组或数组为空");
                            sendErrorMessage("图片生成响应格式错误");
                            return;
                        }

                        JSONObject firstText = textArray.getJSONObject(0);
                        if (firstText == null) {
                            log.error("text 数组第一个元素为空");
                            sendErrorMessage("图片生成响应格式错误");
                            return;
                        }

                        String imageUrl = firstText.getString("content");
                        if (imageUrl == null || imageUrl.isEmpty()) {
                            log.error("响应中未找到 content 字段");
                            sendErrorMessage("图片生成成功但未返回图片URL");
                            return;
                        }

                        if (imageUrl.startsWith("data:image")) {
                            log.info("图片生成成功 - Base64格式，长度: {} 字符", imageUrl.length());
                        } else {
                            log.info("图片生成成功 - URL: {}", imageUrl);
                        }

                        saveImageMessage(userId, imageUrl);

                        JSONObject jsonMessage = new JSONObject();
                        jsonMessage.put("type", "image");
                        jsonMessage.put("imageUrl", imageUrl);
                        jsonMessage.put("status", 2);

                        if (clientSession != null && clientSession.isOpen()) {
                            clientSession.sendMessage(new TextMessage(jsonMessage.toString()));
                            log.info("已将图片URL发送到前端");
                        } else {
                            log.error("客户端WebSocket会话不可用 - session={}, isOpen={}",
                                clientSession, 
                                clientSession != null ? clientSession.isOpen() : "null");
                        }
                    } catch (Exception e) {
                        log.error("处理图片生成响应失败", e);
                        sendErrorMessage("处理图片生成响应失败：" + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            log.error("图片生成异常", e);
            sendErrorMessage("图片生成异常：" + e.getMessage());
        }
    }

    private void saveImageMessage(String userId, String imageUrl) {
        try {
            ChatMessage aiMessage = new ChatMessage();
            aiMessage.setSenderId(0L);
            aiMessage.setReceiverId(Long.parseLong(userId));
            aiMessage.setContent(imageUrl);
            aiMessage.setMessageType(2);
            aiMessage.setStatus(1);
            aiMessage.setCreatedAt(LocalDateTime.now());
            
            chatMessageMapper.insert(aiMessage);

            ChatSession session = chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>()
                    .eq(ChatSession::getUserId, Long.parseLong(userId))
                    .eq(ChatSession::getPartnerId, 0L)
            );
            
            if (session != null) {
                String previewText = imageUrl.startsWith("data:image") 
                    ? "[图片]" 
                    : "[图片] " + imageUrl;
                
                if (previewText.length() > 2000) {
                    previewText = previewText.substring(0, 2000);
                }
                
                session.setLastMessageContent(previewText);
                session.setLastMessageTime(LocalDateTime.now());
                chatSessionMapper.updateById(session);
            }

            log.info("已保存图片消息到数据库 - userId: {}, messageType: 2", userId);
        } catch (Exception e) {
            log.error("保存图片消息失败 - userId: {}", userId, e);
        }
    }

    private JSONObject buildImageRequest(String description) {
        JSONObject requestJson = new JSONObject();

        JSONObject header = new JSONObject();
        header.put("app_id", config.getAppId());
        requestJson.put("header", header);

        JSONObject parameter = new JSONObject();
        JSONObject chat = new JSONObject();
        chat.put("domain", "general");
        chat.put("width", 512);
        chat.put("height", 512);
        parameter.put("chat", chat);
        requestJson.put("parameter", parameter);

        JSONObject payload = new JSONObject();
        JSONObject message = new JSONObject();
        
        com.alibaba.fastjson.JSONArray textArray = new com.alibaba.fastjson.JSONArray();
        JSONObject textItem = new JSONObject();
        textItem.put("role", "user");
        textItem.put("content", description);
        textArray.add(textItem);
        
        message.put("text", textArray);
        payload.put("message", message);
        requestJson.put("payload", payload);

        log.debug("构建的请求体: {}", requestJson.toJSONString());
        
        return requestJson;
    }

    private String buildSignedUrl(String hostUrl) throws Exception {
        URL url = new URL(hostUrl.replaceFirst("^https://", "http://"));
        String host = url.getHost();
        String path = url.getPath();
        String date = toGMTString(new Date());

        String signatureOrigin = "host: " + host + "\n" +
                "date: " + date + "\n" +
                "POST " + path + " HTTP/1.1";

        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(config.getApiSecret().getBytes(StandardCharsets.UTF_8), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(hexDigits);

        String authorizationOrigin = "api_key=\"" + config.getApiKey() + "\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"" + signature + "\"";
        String authorization = Base64.getEncoder().encodeToString(authorizationOrigin.getBytes(StandardCharsets.UTF_8));

        return hostUrl + "?authorization=" + authorization + "&date=" + date + "&host=" + host;
    }

    private String toGMTString(Date date) {
        SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        df.setTimeZone(new SimpleTimeZone(0, "GMT"));
        return df.format(date);
    }

    private void sendErrorMessage(String errorMessage) {
        try {
            if (clientSession != null && clientSession.isOpen()) {
                JSONObject errorMsg = new JSONObject();
                errorMsg.put("type", "image");
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
