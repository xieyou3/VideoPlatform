package com.videoplatform.video.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VideoDetailResponse {
    private Long id;
    private String title;
    private String description;
    private String author;
    private String authorAvatar;
    private String videoUrl;
    private String thumbnail;
    private Integer durationSeconds;
    private Long views;
    private Long likes;
    private Long favorites;
    private Long comments;
    private String createdAt;
    private List<String> tags;

    @Data
    @Builder
    public static class DanmakuItem {
        private Long id;
        private String text;
        private Integer time;
        private String color;
        private Integer top;
    }
}
