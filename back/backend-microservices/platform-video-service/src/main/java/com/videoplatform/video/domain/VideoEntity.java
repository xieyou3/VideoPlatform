package com.videoplatform.video.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.videoplatform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("video_entity")
public class VideoEntity extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fileHash;
    private String videoUrl;
    private Integer durationSeconds;
    private Long fileSize;
}
