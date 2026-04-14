package com.videoplatform.video.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videoplatform.video.domain.VideoEntity;
import com.videoplatform.video.dto.ChunkCheckRequest;
import com.videoplatform.video.dto.ChunkCheckResponse;
import com.videoplatform.video.dto.ChunkUploadResponse;
import com.videoplatform.video.dto.CoverUploadResponse;
import com.videoplatform.video.dto.UploadCompleteRequest;
import com.videoplatform.video.mapper.VideoEntityMapper;
import com.videoplatform.video.service.impl.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoUploadService {

    private final VideoEntityMapper videoEntityMapper;
    private final StorageService storageService;

    private final ConcurrentHashMap<String, Set<Integer>> uploadProgressMap = new ConcurrentHashMap<>();

    public ChunkCheckResponse checkMd5(String fileHash) {
        VideoEntity entity = videoEntityMapper.selectOne(new LambdaQueryWrapper<VideoEntity>()
            .eq(VideoEntity::getFileHash, fileHash));
        
        if (entity != null) {
            String videoUrl = ((MinioStorageService) storageService).getVideoUrl(entity.getVideoUrl());
            return ChunkCheckResponse.builder()
                .exists(true)
                .videoUrl(videoUrl)
                .durationSeconds(entity.getDurationSeconds())
                .fileSize(entity.getFileSize())
                .build();
        }
        
        return ChunkCheckResponse.builder()
            .exists(false)
            .build();
    }

    @Transactional
    public ChunkUploadResponse uploadChunk(Long userId, ChunkCheckRequest request, MultipartFile file) {
        String fileHash = request.getFileHash();
        Integer chunkIndex = request.getChunkIndex();
        Integer totalChunks = request.getTotalChunks();
        
        uploadProgressMap.computeIfAbsent(fileHash, k -> new CopyOnWriteArraySet<>());
        
        Set<Integer> uploadedChunks = uploadProgressMap.get(fileHash);
        
        if (!uploadedChunks.contains(chunkIndex)) {
            String objectName = fileHash + "/" + chunkIndex;
            storageService.uploadChunk(objectName, file);
            
            uploadedChunks.add(chunkIndex);
            log.info("用户 {} 上传分片 {}/{} 成功", userId, chunkIndex + 1, totalChunks);
        }
        
        boolean allUploaded = uploadedChunks.size() == totalChunks;
        String videoUrl = null;
        
        if (allUploaded) {
            videoUrl = mergeAndSaveEntity(fileHash, totalChunks, request.getFileName());
            uploadProgressMap.remove(fileHash);
            log.info("文件 {} 所有分片上传完成并已合并", fileHash);
        }
        
        return ChunkUploadResponse.builder()
            .merged(allUploaded)
            .videoUrl(videoUrl)
            .uploadedChunks(uploadedChunks.size())
            .totalChunks(totalChunks)
            .build();
    }

    public CoverUploadResponse uploadCover(MultipartFile file) {
        try {
            String fileHash = calculateFileHash(file);
            String fileName = file.getOriginalFilename();
            String extension = fileName != null && fileName.contains(".") 
                ? fileName.substring(fileName.lastIndexOf(".")) 
                : ".jpg";
            
            String objectName = "IMG_" + fileHash + extension;
            
            String coverUrl = storageService.uploadImage(objectName, file);
            
            log.info("封面图上传成功: {}", coverUrl);
            
            return CoverUploadResponse.builder()
                .coverUrl(coverUrl)
                .build();
        } catch (Exception e) {
            log.error("封面图上传失败", e);
            throw new IllegalStateException("封面图上传失败: " + e.getMessage(), e);
        }
    }

    private String calculateFileHash(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] fileBytes = file.getBytes();
            byte[] hashBytes = md.digest(fileBytes);
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("计算文件哈希失败", e);
        }
    }

    private String mergeAndSaveEntity(String fileHash, int totalChunks, String fileName) {
        String videoObjectName = storageService.mergeChunks(fileHash, totalChunks, fileName);
        
        VideoEntity entity = videoEntityMapper.selectOne(new LambdaQueryWrapper<VideoEntity>()
            .eq(VideoEntity::getFileHash, fileHash));
        
        if (entity == null) {
            entity = new VideoEntity();
            entity.setFileHash(fileHash);
            entity.setVideoUrl(videoObjectName);
            entity.setFileSize(0L);
            entity.setDurationSeconds(0);
            videoEntityMapper.insert(entity);
            log.info("创建视频实体记录: {}", fileHash);
        }
        
        return ((MinioStorageService) storageService).getVideoUrl(videoObjectName);
    }
}
