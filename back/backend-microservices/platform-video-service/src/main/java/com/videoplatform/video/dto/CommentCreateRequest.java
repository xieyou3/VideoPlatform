package com.videoplatform.video.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentCreateRequest {
    @NotBlank
    private String content;
    private Long parentId;
}
