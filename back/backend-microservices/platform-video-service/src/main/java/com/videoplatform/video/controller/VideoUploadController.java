package com.videoplatform.video.controller;

import com.videoplatform.common.api.ApiResponse;
import com.videoplatform.common.security.JwtTokenProvider;
import com.videoplatform.video.dto.ChunkCheckRequest;
import com.videoplatform.video.dto.ChunkCheckResponse;
import com.videoplatform.video.dto.ChunkUploadResponse;
import com.videoplatform.video.dto.CoverUploadResponse;
import com.videoplatform.video.dto.UploadCompleteRequest;
import com.videoplatform.video.service.VideoUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/videos/upload")
@RequiredArgsConstructor
public class VideoUploadController {

    private final VideoUploadService videoUploadService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/check-md5")
    public ApiResponse<ChunkCheckResponse> checkMd5(@RequestParam String fileHash) {
        return ApiResponse.success(videoUploadService.checkMd5(fileHash));
    }

    @PostMapping("/chunk")
    public ApiResponse<ChunkUploadResponse> uploadChunk(
            @ModelAttribute ChunkCheckRequest request,
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        if (userId == null && authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            userId = jwtTokenProvider.getUserIdFromToken(token);
        }
        
        if (userId == null) {
            return ApiResponse.fail(401, "用户未登录");
        }
        
        return ApiResponse.success(videoUploadService.uploadChunk(userId, request, file));
    }

    @PostMapping("/cover")
    public ApiResponse<CoverUploadResponse> uploadCover(
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        if (userId == null && authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            userId = jwtTokenProvider.getUserIdFromToken(token);
        }
        
        if (userId == null) {
            return ApiResponse.fail(401, "用户未登录");
        }
        
        if (file.isEmpty()) {
            return ApiResponse.fail(400, "文件不能为空");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ApiResponse.fail(400, "只支持图片文件");
        }
        
        if (file.getSize() > 10 * 1024 * 1024) {
            return ApiResponse.fail(400, "图片大小不能超过 10MB");
        }
        
        return ApiResponse.success(videoUploadService.uploadCover(file));
    }
}
