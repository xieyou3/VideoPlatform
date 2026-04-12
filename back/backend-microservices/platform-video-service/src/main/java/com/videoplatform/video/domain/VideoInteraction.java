package com.videoplatform.video.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("video_interaction")
public class VideoInteraction {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long videoId;
    private String interactionType;
    private LocalDateTime createdAt;
}
