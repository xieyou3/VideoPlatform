package com.videoplatform.video.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DanmakuCreateRequest {
    @NotBlank
    private String text;
    @NotNull
    private Integer time;
    private String color;
}
