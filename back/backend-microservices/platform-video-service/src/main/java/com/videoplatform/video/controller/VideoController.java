package com.videoplatform.video.controller;

import com.videoplatform.common.api.ApiResponse;
import com.videoplatform.common.api.PageResult;
import com.videoplatform.video.dto.CommentCreateRequest;
import com.videoplatform.video.dto.DanmakuCreateRequest;
import com.videoplatform.video.dto.VideoDetailResponse;
import com.videoplatform.video.dto.VideoInteractionResponse;
import com.videoplatform.video.dto.VideoListItemResponse;
import com.videoplatform.video.dto.VideoPublishRequest;
import com.videoplatform.video.service.VideoService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @GetMapping
    public PageResult<VideoListItemResponse> listVideos() {
        return videoService.listPublishedVideos();
    }

    @GetMapping("/{id}")
    public VideoDetailResponse getVideoDetail(@PathVariable Long id) {
        return videoService.getVideoDetail(id);
    }

    @GetMapping("/{id}/danmaku")
    public List<VideoDetailResponse.DanmakuItem> getDanmaku(@PathVariable Long id) {
        return videoService.listDanmaku(id);
    }

    @PostMapping
    public ApiResponse<VideoDetailResponse> publishVideo(@Valid @RequestBody VideoPublishRequest request,
                                                         @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                                         @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.info("收到发布视频请求: title={}, userId={}", request.getTitle(), userId);
        
        if (userId == null && authorization != null && authorization.startsWith("Bearer ")) {
            try {
                com.videoplatform.common.security.JwtTokenProvider jwtTokenProvider = 
                    new com.videoplatform.common.security.JwtTokenProvider(
                        "video-platform-demo-secret-video-platform-demo-secret", 1800, 604800);
                userId = jwtTokenProvider.getUserIdFromToken(authorization.substring(7));
                log.info("从 Token 解析出 userId: {}", userId);
            } catch (Exception e) {
                log.error("解析 Token 失败", e);
            }
        }
        
        if (userId == null) {
            log.warn("用户未登录，使用默认 userId=1");
            userId = 1L;
        }
        
        try {
            VideoDetailResponse response = videoService.publishVideo(userId, request);
            log.info("视频发布成功: videoId={}", response.getId());
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("视频发布失败", e);
            return ApiResponse.fail(500, "视频发布失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/comments")
    public ApiResponse<Void> createComment(@PathVariable Long id,
                                           @Valid @RequestBody CommentCreateRequest request,
                                           @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        videoService.createComment(id, userId == null ? 1L : userId, request);
        return ApiResponse.success("OK", null);
    }

    @PostMapping("/{id}/danmaku")
    public ApiResponse<Void> createDanmaku(@PathVariable Long id,
                                           @Valid @RequestBody DanmakuCreateRequest request,
                                           @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        videoService.createDanmaku(id, userId == null ? 1L : userId, request);
        return ApiResponse.success("OK", null);
    }

    @PostMapping("/{id}/like")
    public ApiResponse<VideoInteractionResponse> like(@PathVariable Long id,
                                                      @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.success(videoService.toggleInteraction(id, userId == null ? 1L : userId, "LIKE"));
    }

    @PostMapping("/{id}/favorite")
    public ApiResponse<VideoInteractionResponse> favorite(@PathVariable Long id,
                                                          @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.success(videoService.toggleInteraction(id, userId == null ? 1L : userId, "FAVORITE"));
    }
}
