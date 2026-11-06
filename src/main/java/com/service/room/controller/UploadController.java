package com.service.room.controller;

import com.service.common.dto.ApiResponse;
import com.service.room.service.UploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/upload")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/single")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadSingle(@RequestParam("file") MultipartFile file) {
        Map<String, Object> data = uploadService.storeFile(file);
        return ResponseEntity.ok(ApiResponse.success("Upload thành công", data));
    }

    @PostMapping("/multiple")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> uploadMultiple(@RequestParam("files") List<MultipartFile> files) {
        List<Map<String, Object>> data = uploadService.storeMultipleFiles(files);
        return ResponseEntity.ok(ApiResponse.success("Upload thành công", data));
    }
}

