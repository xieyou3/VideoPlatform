package com.videoplatform.video.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChunkUploadResponse {
    private boolean merged;
    private String videoUrl;
    private String objectName;
}
