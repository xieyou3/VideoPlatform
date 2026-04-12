package com.videoplatform.video.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VideoInteractionResponse {
    private boolean active;
    private Long count;
}
