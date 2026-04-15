package com.videoplatform.social.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "xinghuo")
public class XingHuoConfig {
    private String appId;
    private String apiKey;
    private String apiSecret;

    private ChatConfig chat = new ChatConfig();
    private PPTConfig ppt = new PPTConfig();
    private ImageConfig image = new ImageConfig();

    @Data
    public static class ChatConfig {
        private String hostUrl;
        private String domain;
    }

    @Data
    public static class PPTConfig {
        private String hostUrl;
    }

    @Data
    public static class ImageConfig {
        private String hostUrl;
    }
}
