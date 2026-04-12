package com.videoplatform.video.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("video_tag")
public class VideoTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long videoId;
    private Long tagId;
    private LocalDateTime createdAt;
}
