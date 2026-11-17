package vn.cozynest.listing.infrastructure.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.Normalizer;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
public class CloudinaryStorageService {

    private final Cloudinary cloudinary;

    public CloudinaryStorageService(
            @Value("${cloudinary.cloud-name:dfs07oaet}") String cloudName,
            @Value("${cloudinary.api-key:632318816957616}") String apiKey,
            @Value("${cloudinary.api-secret:cqe9TWG7Qag1rOW8mdXPyf6lZ1g}") String apiSecret
    ) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
        log.info("Initialized CloudinaryStorageService for cloud: {}", cloudName);
    }

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "general";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutDiacritics = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalized).replaceAll("");
        withoutDiacritics = withoutDiacritics.replace('đ', 'd').replace('Đ', 'D');
        String slug = withoutDiacritics.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return slug.isBlank() ? "general" : slug;
    }

    public String uploadRoomImage(byte[] imageBytes, String landlordName, String publicId) {
        String landlordFolder = toSlug(landlordName);
        String targetFolder = "cozynest/rooms/" + landlordFolder;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(imageBytes, ObjectUtils.asMap(
                    "folder", targetFolder,
                    "public_id", publicId,
                    "overwrite", true,
                    "resource_type", "image"
            ));
            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("Uploaded room image to Cloudinary folder {}: {}", targetFolder, secureUrl);
            return secureUrl;
        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary: {}", e.getMessage(), e);
            return null;
        }
    }
}
