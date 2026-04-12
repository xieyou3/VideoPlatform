package com.videoplatform.video.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UploadCompleteRequest {
    @NotBlank
    private String fileHash;
}
