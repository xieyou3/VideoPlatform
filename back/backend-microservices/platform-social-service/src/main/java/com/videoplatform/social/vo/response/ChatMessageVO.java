package com.videoplatform.social.vo.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessageVO {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String content;
    private Integer messageType;
    private Integer status;
    private Integer aiStatus;
    private String uuid;
    private LocalDateTime createdAt;
}
