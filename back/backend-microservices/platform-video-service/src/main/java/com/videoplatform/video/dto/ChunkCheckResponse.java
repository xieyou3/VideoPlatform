package com.videoplatform.video.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkCheckResponse {
    private Boolean exists;
    private String videoUrl;
    private Integer durationSeconds;
    private Long fileSize;
    private Integer uploadedChunks;
    private Integer totalChunks;
}
