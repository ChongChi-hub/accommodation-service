package vn.cozynest.identity.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.cozynest.identity.domain.port.AvatarStoragePort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Component
public class FileSystemAvatarStorageAdapter implements AvatarStoragePort {

    private static final Logger log = LoggerFactory.getLogger(FileSystemAvatarStorageAdapter.class);
    private final Path rootDir;

    public FileSystemAvatarStorageAdapter(@Value("${storage.media-dir:./storage/media}") String mediaDir) {
        this.rootDir = Paths.get(mediaDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootDir);
            log.info("Initialized avatar public media storage at: {}", this.rootDir);
        } catch (IOException e) {
            log.error("Failed to create avatar root storage directory: {}", this.rootDir, e);
            throw new RuntimeException("Could not initialize avatar storage directory", e);
        }
    }

    private Path resolveSafePath(String key) {
        String cleanKey = key.replace('\\', '/').replaceAll("^/+", "");
        Path resolved = rootDir.resolve(cleanKey).normalize();
        if (!resolved.startsWith(rootDir)) {
            throw new SecurityException("Attempted path traversal access: " + key);
        }
        return resolved;
    }

    @Override
    public String store(String key, byte[] content, String contentType) {
        try {
            Path targetPath = resolveSafePath(key);
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return key;
        } catch (IOException e) {
            log.error("Failed to store avatar key: {}", key, e);
            throw new RuntimeException("Error writing avatar file to disk", e);
        }
    }

    @Override
    public byte[] load(String key) {
        try {
            Path targetPath = resolveSafePath(key);
            if (!Files.exists(targetPath)) {
                return null;
            }
            return Files.readAllBytes(targetPath);
        } catch (IOException e) {
            log.error("Failed to read avatar key: {}", key, e);
            return null;
        }
    }

    @Override
    public void delete(String key) {
        try {
            Path targetPath = resolveSafePath(key);
            Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            log.warn("Failed to delete avatar key: {}", key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        Path targetPath = resolveSafePath(key);
        return Files.exists(targetPath);
    }
}

