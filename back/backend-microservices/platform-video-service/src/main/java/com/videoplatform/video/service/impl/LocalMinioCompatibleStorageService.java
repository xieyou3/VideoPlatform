package com.videoplatform.video.service.impl;

import com.videoplatform.video.service.StorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalMinioCompatibleStorageService implements StorageService {

    @Value("${storage.base-dir:uploads}")
    private String baseDir;

    @Override
    public String uploadChunk(String objectName, MultipartFile file) {
        try {
            Path target = Path.of(baseDir, objectName);
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new IllegalStateException("chunk upload failed", e);
        }
    }

    @Override
    public String mergeChunks(String objectPrefix, int totalChunks, String fileName) {
        try {
            Path mergeDir = Path.of(baseDir, "merged");
            Files.createDirectories(mergeDir);
            Path merged = mergeDir.resolve(fileName);
            if (Files.exists(merged)) {
                Files.delete(merged);
            }
            Files.createFile(merged);
            for (int i = 0; i < totalChunks; i++) {
                Path chunk = Path.of(baseDir, objectPrefix, "chunk-" + i);
                Files.write(merged, Files.readAllBytes(chunk), java.nio.file.StandardOpenOption.APPEND);
            }
            return "/minio/video/" + fileName;
        } catch (IOException e) {
            throw new IllegalStateException("chunk merge failed", e);
        }
    }
}
