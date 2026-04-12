package com.videoplatform.auth.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.videoplatform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("users")
public class User extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String passwordHash;
    private String avatarUrl;
    private String bio;
    private String status;
    private Long fansCount;
    private Long followingCount;
    private Long likedCount;
    private LocalDateTime lastLoginAt;
}
