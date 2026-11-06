package com.service.room.service;

import com.service.common.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
public class UploadService {

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "video/mp4", "video/quicktime",
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    public Map<String, Object> storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Không có file nào được upload");
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_TYPES.contains(mimeType.toLowerCase())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Loại file không được hỗ trợ");
        }

        try {
            Path targetDir = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }

            String uniqueName = System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Path targetFile = targetDir.resolve(uniqueName);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            String fileType = mimeType.startsWith("image/") ? "image" :
                              mimeType.startsWith("video/") ? "video" : "document";

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("url", "/uploads/" + uniqueName);
            data.put("originalFilename", originalName);
            data.put("fileSize", file.getSize());
            data.put("mimeType", mimeType);
            data.put("fileType", fileType);

            return data;
        } catch (IOException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi lưu file: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> storeMultipleFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Không có file nào được upload");
        }
        return files.stream().map(this::storeFile).toList();
    }
}

