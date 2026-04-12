package com.videoplatform.social.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatViewResponse {
    private Long id;
    private String name;
    private String avatar;
    private String lastMessage;
    private String time;
    private Integer unread;
    private String type;
    private List<MessageViewResponse> messages;
}
