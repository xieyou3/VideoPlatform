package com.videoplatform.video.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.videoplatform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("video_upload_session")
public class VideoUploadSession extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fileHash;
    private String fileName;
    private Integer totalChunks;
    private Integer uploadedChunks;
    private Boolean merged;
    private String minioObjectName;
    private Long createdBy;
}
