package com.videoplatform.video.controller;

import com.videoplatform.common.api.ApiResponse;
import com.videoplatform.video.dto.ChunkCheckRequest;
import com.videoplatform.video.dto.ChunkCheckResponse;
import com.videoplatform.video.dto.ChunkUploadResponse;
import com.videoplatform.video.dto.UploadCompleteRequest;
import com.videoplatform.video.service.VideoUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/videos/upload")
@RequiredArgsConstructor
public class VideoUploadController {

    private final VideoUploadService videoUploadService;

    @PostMapping("/chunk/check")
    public ApiResponse<ChunkCheckResponse> checkChunk(@Valid @RequestBody ChunkCheckRequest request,
                                                      @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.success(videoUploadService.checkChunk(userId == null ? 1L : userId, request));
    }

    @PostMapping("/chunk")
    public ApiResponse<ChunkUploadResponse> uploadChunk(@ModelAttribute ChunkCheckRequest request,
                                                        @RequestPart("file") MultipartFile file,
                                                        @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.success(videoUploadService.uploadChunk(userId == null ? 1L : userId, request, file));
    }

    @PostMapping("/complete")
    public ApiResponse<ChunkUploadResponse> complete(@Valid @RequestBody UploadCompleteRequest request,
                                                     @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.success(videoUploadService.complete(userId == null ? 1L : userId, request));
    }
}
