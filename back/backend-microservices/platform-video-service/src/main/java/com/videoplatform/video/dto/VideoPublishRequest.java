package com.videoplatform.video.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

@Data
public class VideoPublishRequest {
    @NotBlank
    private String title;
    private String description;
    private List<String> tags;
    private String coverUrl;
    @NotBlank
    private String videoUrl;
    private Integer durationSeconds;
    private String category;
}
