package com.videoplatform.social.ws;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.springframework.web.socket.WebSocketSession;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.SimpleTimeZone;

@Slf4j
public abstract class BaseBigModelHandler extends WebSocketListener {

    protected static final Gson gson = new Gson();
    protected String userId;
    protected WebSocketSession clientSession;

    protected WebSocket createWebSocketConnection(String hostUrl, String apiKey, String apiSecret) throws Exception {
        String httpUrl = hostUrl.replaceFirst("^wss://", "https://").replaceFirst("^ws://", "http://");
        URL url = new URL(httpUrl);
        String host = url.getHost();
        String path = url.getPath();
        String date = toGMTString(new Date());

        String signatureOrigin = "host: " + host + "\n" +
                "date: " + date + "\n" +
                "GET " + path + " HTTP/1.1";

        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(hexDigits);

        String authorizationOrigin = "api_key=\"" + apiKey + "\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"" + signature + "\"";
        String authorization = Base64.getEncoder().encodeToString(authorizationOrigin.getBytes(StandardCharsets.UTF_8));

        HttpUrl httpUrlObj = Objects.requireNonNull(HttpUrl.parse(httpUrl)).newBuilder()
                .addQueryParameter("authorization", authorization)
                .addQueryParameter("date", date)
                .addQueryParameter("host", host)
                .build();

        OkHttpClient client = new OkHttpClient.Builder().build();
        Request request = new Request.Builder().url(httpUrlObj).build();

        return client.newWebSocket(request, this);
    }

    private String toGMTString(Date date) {
        SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        df.setTimeZone(new SimpleTimeZone(0, "GMT"));
        return df.format(date);
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        log.info("WebSocket 连接已建立");
    }

    @Override
    public abstract void onMessage(WebSocket webSocket, String text);

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        log.info("WebSocket 正在关闭: code={}, reason={}", code, reason);
        webSocket.close(code, reason);
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        log.info("WebSocket 连接已关闭: code={}, reason={}", code, reason);
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        log.error("WebSocket 连接失败: {}", t.getMessage());
    }

    public abstract void send(String question, String userId, WebSocketSession clientSession) throws Exception;
}
