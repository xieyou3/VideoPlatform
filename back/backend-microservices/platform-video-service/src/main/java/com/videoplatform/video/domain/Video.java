package com.videoplatform.video.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.videoplatform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("video")
public class Video extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long authorId;
    private String title;
    private String description;
    private String coverUrl;
    private String videoUrl;
    private Integer durationSeconds;
    private String category;
    private String status;
    private String reviewStatus;
    private String visibility;
    private Long viewCount;
    private Long likeCount;
    private Long favoriteCount;
    private Long commentCount;
    private Long danmakuCount;
}
