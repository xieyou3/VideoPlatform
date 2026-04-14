package com.videoplatform.video.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UploadCompleteRequest {
    @NotBlank
    private String fileHash;
    
    @NotBlank
    private String title;
    
    private String description;
    
    private List<String> tags;
    
    private String coverUrl;
    
    private String category;
    
    @NotNull
    private Integer durationSeconds;
    
    @NotNull
    private Long fileSize;
}
