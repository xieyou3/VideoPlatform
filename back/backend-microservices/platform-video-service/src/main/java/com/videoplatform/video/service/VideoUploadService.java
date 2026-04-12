package com.videoplatform.video.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videoplatform.video.domain.VideoUploadChunk;
import com.videoplatform.video.domain.VideoUploadSession;
import com.videoplatform.video.dto.ChunkCheckRequest;
import com.videoplatform.video.dto.ChunkCheckResponse;
import com.videoplatform.video.dto.ChunkUploadResponse;
import com.videoplatform.video.dto.UploadCompleteRequest;
import com.videoplatform.video.mapper.VideoUploadChunkMapper;
import com.videoplatform.video.mapper.VideoUploadSessionMapper;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class VideoUploadService {

    private final VideoUploadSessionMapper uploadSessionMapper;
    private final VideoUploadChunkMapper uploadChunkMapper;
    private final StorageService storageService;

    public ChunkCheckResponse checkChunk(Long userId, ChunkCheckRequest request) {
        VideoUploadSession session = createOrGetSession(userId, request);
        Set<Integer> uploadedChunks = uploadChunkMapper.selectList(new LambdaQueryWrapper<VideoUploadChunk>()
                        .eq(VideoUploadChunk::getUploadSessionId, session.getId()))
                .stream()
                .map(VideoUploadChunk::getChunkIndex)
                .collect(Collectors.toSet());
        return ChunkCheckResponse.builder()
                .uploaded(uploadedChunks.contains(request.getChunkIndex()))
                .uploadedChunks(uploadedChunks)
                .build();
    }

    @Transactional
    public ChunkUploadResponse uploadChunk(Long userId, ChunkCheckRequest request, MultipartFile file) {
        VideoUploadSession session = createOrGetSession(userId, request);
        boolean existed = uploadChunkMapper.selectCount(new LambdaQueryWrapper<VideoUploadChunk>()
                .eq(VideoUploadChunk::getUploadSessionId, session.getId())
                .eq(VideoUploadChunk::getChunkIndex, request.getChunkIndex())) > 0;
        if (!existed) {
            storageService.uploadChunk(session.getFileHash() + "/chunk-" + request.getChunkIndex(), file);
            VideoUploadChunk chunk = new VideoUploadChunk();
            chunk.setUploadSessionId(session.getId());
            chunk.setChunkIndex(request.getChunkIndex());
            chunk.setChunkSize(file.getSize());
            chunk.setUploaded(true);
            uploadChunkMapper.insert(chunk);
            session.setUploadedChunks(session.getUploadedChunks() + 1);
            uploadSessionMapper.updateById(session);
        }
        boolean merged = session.getUploadedChunks() >= session.getTotalChunks();
        String videoUrl = null;
        if (merged && !Boolean.TRUE.equals(session.getMerged())) {
            videoUrl = mergeSession(session);
        }
        return ChunkUploadResponse.builder()
                .merged(merged)
                .videoUrl(videoUrl)
                .objectName(session.getMinioObjectName())
                .build();
    }

    @Transactional
    public ChunkUploadResponse complete(Long userId, UploadCompleteRequest request) {
        VideoUploadSession session = uploadSessionMapper.selectOne(new LambdaQueryWrapper<VideoUploadSession>()
                .eq(VideoUploadSession::getFileHash, request.getFileHash())
                .eq(VideoUploadSession::getCreatedBy, userId));
        if (session == null) {
            throw new IllegalArgumentException("upload session not found");
        }
        String videoUrl = Boolean.TRUE.equals(session.getMerged()) ? session.getMinioObjectName() : mergeSession(session);
        return ChunkUploadResponse.builder()
                .merged(true)
                .videoUrl(videoUrl)
                .objectName(session.getMinioObjectName())
                .build();
    }

    private VideoUploadSession createOrGetSession(Long userId, ChunkCheckRequest request) {
        VideoUploadSession session = uploadSessionMapper.selectOne(new LambdaQueryWrapper<VideoUploadSession>()
                .eq(VideoUploadSession::getFileHash, request.getFileHash())
                .eq(VideoUploadSession::getCreatedBy, userId));
        if (session != null) {
            return session;
        }
        session = new VideoUploadSession();
        session.setFileHash(request.getFileHash());
        session.setFileName(request.getFileName());
        session.setTotalChunks(request.getTotalChunks());
        session.setUploadedChunks(0);
        session.setMerged(false);
        session.setCreatedBy(userId);
        uploadSessionMapper.insert(session);
        return session;
    }

    private String mergeSession(VideoUploadSession session) {
        String videoUrl = storageService.mergeChunks(session.getFileHash(), session.getTotalChunks(), session.getFileName());
        session.setMerged(true);
        session.setMinioObjectName(videoUrl);
        uploadSessionMapper.updateById(session);
        return videoUrl;
    }
}
