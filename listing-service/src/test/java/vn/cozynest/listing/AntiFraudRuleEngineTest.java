package vn.cozynest.listing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.cozynest.listing.domain.service.AntiFraudRuleEngine;
import vn.cozynest.listing.domain.service.AntiFraudRuleEngine.AntiFraudResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AntiFraudRuleEngineTest {

    @Test
    @DisplayName("Room price below 40% of ward median is flagged as an anomaly")
    void testPriceAnomalyBelow40PercentMedianFlagged() {
        List<BigDecimal> marketPrices = List.of(
                BigDecimal.valueOf(5_000_000),
                BigDecimal.valueOf(6_000_000),
                BigDecimal.valueOf(7_000_000)
        ); // median = 6,000,000; 40% = 2,400,000

        BigDecimal cheapPrice = BigDecimal.valueOf(2_000_000);
        Optional<String> reason = AntiFraudRuleEngine.checkPriceAnomaly(cheapPrice, marketPrices);

        assertThat(reason).isPresent();
        assertThat(reason.get()).contains("< 40% so với trung vị khu vực");
    }

    @Test
    @DisplayName("Room price equal to or above 40% of ward median is NOT flagged")
    void testPriceNormalNotFlagged() {
        List<BigDecimal> marketPrices = List.of(
                BigDecimal.valueOf(5_000_000),
                BigDecimal.valueOf(6_000_000),
                BigDecimal.valueOf(7_000_000)
        );

        BigDecimal fairPrice = BigDecimal.valueOf(3_000_000);
        Optional<String> reason = AntiFraudRuleEngine.checkPriceAnomaly(fairPrice, marketPrices);

        assertThat(reason).isEmpty();
    }

    @Test
    @DisplayName("Title or description containing Vietnamese phone numbers is detected as contact bypass")
    void testContactBypassPhoneNumberDetected() {
        Optional<String> r1 = AntiFraudRuleEngine.checkContactBypass(
                "Phòng trọ giá tốt liên hệ 0987654321",
                "Mô tả bình thường",
                "Không hút thuốc"
        );
        assertThat(r1).isPresent();
        assertThat(r1.get()).contains("số điện thoại hoặc liên kết Zalo");

        Optional<String> r2 = AntiFraudRuleEngine.checkContactBypass(
                "Phòng trọ Cầu Giấy",
                "Liên hệ chính chủ qua số +84912345678",
                ""
        );
        assertThat(r2).isPresent();
    }

    @Test
    @DisplayName("Description containing Zalo link or off-platform keyword is detected as contact bypass")
    void testContactBypassZaloLinkDetected() {
        Optional<String> reason = AntiFraudRuleEngine.checkContactBypass(
                "Phòng trọ view đẹp",
                "Vui lòng add zalo.me/0901234567 để trao đổi trực tiếp",
                ""
        );
        assertThat(reason).isPresent();
    }

    @Test
    @DisplayName("Duplicate pHash with Hamming distance <= 10 is detected as stolen image")
    void testDuplicatePhashDetected() {
        String hash1 = "ffff0000ffff0000";
        // Distance 1 (only last hex char diff: 0 vs 1)
        String hash2 = "ffff0000ffff0001";

        Optional<String> reason = AntiFraudRuleEngine.checkDuplicatePhash(hash1, List.of(hash2));
        assertThat(reason).isPresent();
        assertThat(reason.get()).contains("trùng lặp với tin đăng khác");
    }

    @Test
    @DisplayName("Completely distinct pHash with Hamming distance > 10 is NOT flagged")
    void testDistinctPhashNotFlagged() {
        String hash1 = "0000000000000000";
        String hash2 = "ffffffffffffffff";

        Optional<String> reason = AntiFraudRuleEngine.checkDuplicatePhash(hash1, List.of(hash2));
        assertThat(reason).isEmpty();
    }

    @Test
    @DisplayName("Overall evaluate flags HIGH_RISK when price anomaly and contact bypass are present")
    void testEvaluateCombinesAnomalies() {
        List<BigDecimal> marketPrices = List.of(
                BigDecimal.valueOf(5_000_000),
                BigDecimal.valueOf(6_000_000),
                BigDecimal.valueOf(7_000_000)
        );

        AntiFraudResult result = AntiFraudRuleEngine.evaluate(
                BigDecimal.valueOf(1_500_000),
                marketPrices,
                "Phòng Cầu Giấy gọi 0901234567",
                "Mô tả phòng",
                "Giờ giấc tự do",
                List.of(),
                List.of()
        );

        assertThat(result.highRisk()).isTrue();
        assertThat(result.reasons()).hasSize(2);
    }
}
