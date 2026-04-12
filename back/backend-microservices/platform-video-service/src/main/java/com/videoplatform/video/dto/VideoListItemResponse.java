package com.videoplatform.video.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VideoListItemResponse {
    private Long id;
    private String title;
    private String thumbnail;
    private String author;
    private Long views;
    private String duration;
    private String createdAt;
    private Long likes;
}
