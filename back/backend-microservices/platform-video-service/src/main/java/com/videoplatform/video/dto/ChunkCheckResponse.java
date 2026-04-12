package com.videoplatform.video.dto;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChunkCheckResponse {
    private boolean uploaded;
    private Set<Integer> uploadedChunks;
}
