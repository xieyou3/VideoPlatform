package com.videoplatform.video.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUploadResponse {
    private Boolean merged;
    private String videoUrl;
    private Integer uploadedChunks;
    private Integer totalChunks;
}
