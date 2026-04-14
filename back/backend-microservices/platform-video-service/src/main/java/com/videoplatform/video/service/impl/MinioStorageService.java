package com.videoplatform.video.service.impl;

import com.videoplatform.video.service.StorageService;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name:video-platform}")
    private String bucketName;

    @Value("${minio.chunk-dir:video_chunks}")
    private String chunkDir;

    @Value("${minio.video-dir:videos}")
    private String videoDir;

    @Value("${minio.image-dir:video_img}")
    private String imageDir;

    @PostConstruct
    public void initBucket() {
        try {
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            if (!bucketExists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
                log.info("创建 MinIO bucket: {}", bucketName);
            } else {
                log.info("MinIO bucket 已存在: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("初始化 MinIO bucket 失败", e);
            throw new IllegalStateException("MinIO bucket 初始化失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadChunk(String objectName, MultipartFile file) {
        try {
            String objectPath = chunkDir + "/" + objectName;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("分片上传成功: {}", objectPath);
            return objectPath;
        } catch (Exception e) {
            log.error("分片上传失败: {}", objectName, e);
            throw new IllegalStateException("分片上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String mergeChunks(String fileHash, int totalChunks, String fileName) {
        try {
            String videoObjectName = videoDir + "/" + fileHash + getFileExtension(fileName);

            List<ComposeSource> sources = new ArrayList<>();
            for (int i = 0; i < totalChunks; i++) {
                String chunkPath = chunkDir + "/" + fileHash + "/" + i;
                sources.add(ComposeSource.builder()
                        .bucket(bucketName)
                        .object(chunkPath)
                        .build());
            }

            minioClient.composeObject(
                    ComposeObjectArgs.builder()
                            .bucket(bucketName)
                            .object(videoObjectName)
                            .sources(sources)
                            .build()
            );

            log.info("分片合并成功: {}", videoObjectName);

            deleteChunks(fileHash, totalChunks);

            return videoObjectName;
        } catch (Exception e) {
            log.error("分片合并失败: {}", fileHash, e);
            throw new IllegalStateException("分片合并失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadImage(String objectName, MultipartFile file) {
        try {
            String objectPath = imageDir + "/" + objectName;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("图片上传成功: {}", objectPath);

            return getPresignedUrl(objectPath);
        } catch (Exception e) {
            log.error("图片上传失败: {}", objectName, e);
            throw new IllegalStateException("图片上传失败: " + e.getMessage(), e);
        }
    }

    public String getVideoUrl(String objectName) {
        try {
            return getPresignedUrl(objectName);
        } catch (Exception e) {
            log.error("获取视频URL失败: {}", objectName, e);
            throw new IllegalStateException("获取视频URL失败: " + e.getMessage(), e);
        }
    }

    private String getPresignedUrl(String objectName) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(7 * 24 * 60 * 60)
                        .build()
        );
    }

    private void deleteChunks(String fileHash, int totalChunks) {
        for (int i = 0; i < totalChunks; i++) {
            try {
                String chunkPath = chunkDir + "/" + fileHash + "/" + i;
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(chunkPath)
                                .build()
                );
            } catch (Exception e) {
                log.warn("删除分片失败: {}/{}", fileHash, i, e);
            }
        }

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(chunkDir + "/" + fileHash)
                            .build()
            );
        } catch (Exception e) {
            log.warn("删除分片目录失败: {}", fileHash, e);
        }
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");
        return dotIndex > 0 ? fileName.substring(dotIndex) : ".mp4";
    }
}
