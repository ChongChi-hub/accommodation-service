package vn.cozynest.identity.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KycCryptoServiceTest {

    private KycCryptoService cryptoService;

    @BeforeEach
    void setUp() {
        cryptoService = new KycCryptoService(
                "CozyNestKycSecretKey2026DevEnvironment32B",
                "CozyNestKycPepperSecret2026DevEnvKey!"
        );
    }

    @Test
    @DisplayName("Should successfully encrypt and decrypt 12-digit citizen ID number")
    void shouldEncryptAndDecryptCitizenId() {
        String originalId = "000123456789";
        String encrypted = cryptoService.encryptIdNumber(originalId);

        assertThat(encrypted).isNotNull().isNotEqualTo(originalId);

        String decrypted = cryptoService.decryptIdNumber(encrypted);
        assertThat(decrypted).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should compute consistent 64-char lowercase HMAC-SHA256 hash")
    void shouldComputeConsistentHmacHash() {
        String originalId = "000123456789";
        String hash1 = cryptoService.hashIdNumber(originalId);
        String hash2 = cryptoService.hashIdNumber(originalId);

        assertThat(hash1).isNotNull().hasSize(64).isEqualTo(hash2);
    }

    @Test
    @DisplayName("Should correctly extract last 4 digits and mask")
    void shouldExtractLast4AndMask() {
        String idNumber = "000123456789";
        String last4 = cryptoService.extractLast4(idNumber);
        assertThat(last4).isEqualTo("6789");

        String masked = cryptoService.maskIdNumber(last4);
        assertThat(masked).isEqualTo("********6789");
    }
}
