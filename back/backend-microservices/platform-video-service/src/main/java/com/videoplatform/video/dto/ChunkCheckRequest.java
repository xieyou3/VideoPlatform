package com.videoplatform.video.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChunkCheckRequest {
    @NotBlank
    private String fileHash;
    @NotBlank
    private String fileName;
    @NotNull
    @Min(0)
    private Integer chunkIndex;
    @NotNull
    @Min(1)
    private Integer totalChunks;
}
