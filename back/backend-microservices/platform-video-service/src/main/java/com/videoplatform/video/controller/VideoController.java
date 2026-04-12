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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
                                                         @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.success(videoService.publishVideo(userId == null ? 1L : userId, request));
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
