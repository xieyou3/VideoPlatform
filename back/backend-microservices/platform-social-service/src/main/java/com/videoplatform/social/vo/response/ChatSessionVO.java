package com.videoplatform.social.vo.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatSessionVO {
    private Long id;
    private Long partnerId;
    private String partnerName;
    private String partnerAvatar;
    private String lastMessageContent;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
}
