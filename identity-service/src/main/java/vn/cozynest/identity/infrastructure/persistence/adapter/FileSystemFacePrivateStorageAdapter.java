package vn.cozynest.identity.infrastructure.persistence.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.cozynest.identity.domain.port.FacePrivateStoragePort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class FileSystemFacePrivateStorageAdapter implements FacePrivateStoragePort {

    private static final Logger log = LoggerFactory.getLogger(FileSystemFacePrivateStorageAdapter.class);
    private final Path storageDirectory;

    public FileSystemFacePrivateStorageAdapter(
            @Value("${face.storage.directory:./storage/face_private}") String dirPath) {
        this.storageDirectory = Paths.get(dirPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageDirectory);
            log.info("Initialized secure private face storage at: {}", this.storageDirectory);
        } catch (IOException e) {
            log.error("Failed to initialize face storage directory: {}", e.getMessage());
            throw new RuntimeException("Could not create face storage directory", e);
        }
    }

    @Override
    public void store(String storageKey, byte[] encryptedBytes) {
        try {
            Path target = resolvePath(storageKey);
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.write(target, encryptedBytes);
        } catch (IOException e) {
            log.error("Error storing encrypted face frame: {}", e.getMessage());
            throw new RuntimeException("Lỗi lưu trữ tệp khuôn mặt mã hóa", e);
        }
    }

    @Override
    public byte[] load(String storageKey) {
        try {
            Path target = resolvePath(storageKey);
            if (!Files.exists(target)) {
                return null;
            }
            return Files.readAllBytes(target);
        } catch (IOException e) {
            log.error("Error reading encrypted face frame: {}", e.getMessage());
            throw new RuntimeException("Lỗi đọc tệp khuôn mặt mã hóa", e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Path target = resolvePath(storageKey);
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Failed to delete encrypted face frame: {}", e.getMessage());
        }
    }

    private Path resolvePath(String storageKey) {
        String cleanKey = storageKey.replace("\\", "/").replaceAll("^/+", "");
        Path resolved = this.storageDirectory.resolve(cleanKey).normalize();
        if (!resolved.startsWith(this.storageDirectory)) {
            throw new SecurityException("Invalid storage key path traversal detected");
        }
        return resolved;
    }
}

