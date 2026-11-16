package vn.cozynest.listing.domain.service;

import vn.cozynest.common.image.ImageProcessingHelper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class AntiFraudRuleEngine {

    public record AntiFraudResult(boolean highRisk, List<String> reasons) {
        public static AntiFraudResult normal() {
            return new AntiFraudResult(false, List.of());
        }

        public static AntiFraudResult highRisk(List<String> reasons) {
            return new AntiFraudResult(true, reasons);
        }
    }

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:(?:\\+84|0)[1-9](?:[\\s.-]?\\d){8,9})"
    );

    private static final Pattern BYPASS_LINK_PATTERN = Pattern.compile(
            "(?i)(zalo\\.me|facebook\\.com|fb\\.com|t\\.me|telegram|bit\\.ly)"
    );

    private static final Pattern CONTACT_KEYWORD_PATTERN = Pattern.compile(
            "(?i)\\b(zalo|zl|sđt|sdt|call|liên hệ|lh)\\s*[:\\s-]?\\s*\\d{8,11}"
    );

    public static Optional<String> checkPriceAnomaly(BigDecimal roomPrice, List<BigDecimal> marketPrices) {
        if (roomPrice == null || marketPrices == null || marketPrices.size() < 3) {
            return Optional.empty();
        }

        List<BigDecimal> sorted = new ArrayList<>(marketPrices);
        Collections.sort(sorted);
        int size = sorted.size();
        BigDecimal median;
        if (size % 2 == 1) {
            median = sorted.get(size / 2);
        } else {
            median = sorted.get(size / 2 - 1).add(sorted.get(size / 2))
                    .divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP);
        }

        BigDecimal threshold = median.multiply(BigDecimal.valueOf(0.40));
        if (roomPrice.compareTo(threshold) < 0) {
            return Optional.of("Giá phòng (" + roomPrice + " đ) thấp bất thường (< 40% so với trung vị khu vực " + median + " đ)");
        }
        return Optional.empty();
    }

    public static Optional<String> checkContactBypass(String title, String description, String houseRules) {
        String combined = (title != null ? title : "") + " "
                + (description != null ? description : "") + " "
                + (houseRules != null ? houseRules : "");

        if (PHONE_PATTERN.matcher(combined).find()
                || BYPASS_LINK_PATTERN.matcher(combined).find()
                || CONTACT_KEYWORD_PATTERN.matcher(combined).find()) {
            return Optional.of("Tiêu đề hoặc mô tả chứa số điện thoại hoặc liên kết Zalo/mạng xã hội né sàn");
        }
        return Optional.empty();
    }

    public static Optional<String> checkDuplicatePhash(String candidatePhash, List<String> existingPhashes) {
        if (candidatePhash == null || existingPhashes == null || existingPhashes.isEmpty()) {
            return Optional.empty();
        }
        for (String existing : existingPhashes) {
            if (existing != null) {
                int dist = ImageProcessingHelper.hammingDistance(candidatePhash, existing);
                if (dist <= 10) {
                    return Optional.of("Ảnh phòng trùng lặp với tin đăng khác (khoảng cách pHash " + dist + " <= 10)");
                }
            }
        }
        return Optional.empty();
    }

    public static AntiFraudResult evaluate(
            BigDecimal price,
            List<BigDecimal> marketPrices,
            String title,
            String description,
            String houseRules,
            List<String> roomPhashes,
            List<String> otherPhashes
    ) {
        List<String> reasons = new ArrayList<>();

        checkPriceAnomaly(price, marketPrices).ifPresent(reasons::add);
        checkContactBypass(title, description, houseRules).ifPresent(reasons::add);

        if (roomPhashes != null && otherPhashes != null && !otherPhashes.isEmpty()) {
            for (String hash : roomPhashes) {
                checkDuplicatePhash(hash, otherPhashes).ifPresent(reasons::add);
            }
        }

        return reasons.isEmpty() ? AntiFraudResult.normal() : AntiFraudResult.highRisk(reasons);
    }
}
