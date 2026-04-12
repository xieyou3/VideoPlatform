package com.videoplatform.social.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("chat_session_member")
public class ChatSessionMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long userId;
    private Integer unreadCount;
    private LocalDateTime joinedAt;
}
