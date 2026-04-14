package com.videoplatform.video.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String uploadChunk(String objectName, MultipartFile file);
    String mergeChunks(String objectPrefix, int totalChunks, String fileName);
    String uploadImage(String objectName, MultipartFile file);
}
