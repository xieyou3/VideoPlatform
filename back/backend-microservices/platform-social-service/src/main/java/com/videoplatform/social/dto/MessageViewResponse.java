package com.videoplatform.social.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageViewResponse {
    private Long id;
    private String senderId;
    private String text;
    private String time;
    private String status;
}
