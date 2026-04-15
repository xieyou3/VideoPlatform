package com.videoplatform.social.vo.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FriendRequestVO {
    private Long id;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private Long receiverId;
    private String status;
    private LocalDateTime createdAt;
}
