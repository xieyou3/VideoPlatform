package com.videoplatform.social.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("friend_relation")
public class FriendRelation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long friendUserId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
