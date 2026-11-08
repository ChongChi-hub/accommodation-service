package vn.cozynest.identity.infrastructure.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultApplicationArguments;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class IdentitySeedAssetLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should populate demo avatars and synthetic KYC documents with watermarks")
    void shouldPopulateDemoAssets() {
        Path mediaPath = tempDir.resolve("media");
        Path kycPath = tempDir.resolve("kyc_private");

        FileSystemAvatarStorageAdapter avatarAdapter = new FileSystemAvatarStorageAdapter(mediaPath.toString());
        FileSystemKycStorageAdapter kycAdapter = new FileSystemKycStorageAdapter(kycPath.toString());

        IdentitySeedAssetLoader loader = new IdentitySeedAssetLoader(avatarAdapter, kycAdapter);
        loader.run(new DefaultApplicationArguments(new String[0]));

        assertThat(avatarAdapter.exists("avatars/u-landlord-01.webp")).isTrue();
        assertThat(avatarAdapter.exists("avatars/u-landlord-01_thumb.webp")).isTrue();
        assertThat(avatarAdapter.load("avatars/u-landlord-01.webp")).isNotEmpty();

        assertThat(kycAdapter.exists("kyc/private/landlord-01_front.webp")).isTrue();
        assertThat(kycAdapter.exists("kyc/private/landlord-01_back.webp")).isTrue();
        assertThat(kycAdapter.exists("kyc/private/landlord-02_front.webp")).isTrue();
        assertThat(kycAdapter.load("kyc/private/landlord-01_front.webp")).isNotEmpty();
    }
}

