package vn.toam.identity.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import vn.toam.identity.application.dto.AvatarUploadResponse;
import vn.toam.identity.application.dto.UpdateUserProfileRequest;
import vn.toam.identity.application.dto.UserProfileResponse;
import vn.toam.identity.domain.exception.IdentityDomainException;
import vn.toam.identity.domain.model.User;
import vn.toam.identity.domain.model.UserProfile;
import vn.toam.identity.domain.model.UserRoleType;
import vn.toam.identity.domain.port.AvatarStoragePort;
import vn.toam.identity.domain.repository.UserProfileRepository;
import vn.toam.identity.domain.repository.UserRepository;
import vn.toam.identity.domain.repository.UserVerificationRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserVerificationRepository userVerificationRepository;

    @Mock
    private AvatarStoragePort avatarStorage;

    private UserProfileUseCase useCase;

    private User testUser;
    private UserProfile testProfile;

    @BeforeEach
    void setUp() {
        useCase = new UserProfileUseCase(
                userRepository,
                userProfileRepository,
                userVerificationRepository,
                avatarStorage,
                "/api/v1/media"
        );

        testUser = User.createNew("user@toam.vn", "0901234567", "hash", "Nguyen Van A", UserRoleType.LANDLORD);
        testProfile = UserProfile.createInitial(testUser.getId(), "Nguyen Van A", "0901234567");
    }

    @Test
    @DisplayName("Should update profile successfully with valid data")
    void shouldUpdateProfileSuccessfully() {
        UpdateUserProfileRequest req = new UpdateUserProfileRequest();
        req.setFullName("Nguyen Van B");
        req.setPhone("0909888999");
        req.setGender("MALE");
        req.setDateOfBirth(LocalDate.of(1995, 5, 20));
        req.setAddressDetail("123 Duong ABC");
        req.setVersion(0L);

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userProfileRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testProfile));
        when(userProfileRepository.existsByPhoneAndUserIdNot(req.getPhone(), testUser.getId())).thenReturn(false);
        when(userRepository.findByPhone(req.getPhone())).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        UserProfileResponse res = useCase.updateProfile(testUser.getId(), req);

        assertThat(res.getFullName()).isEqualTo("Nguyen Van B");
        assertThat(res.getPhone()).isEqualTo("0909888999");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject update if version is mismatched (HTTP 409 conflict)")
    void shouldRejectWhenVersionConflict() {
        UpdateUserProfileRequest req = new UpdateUserProfileRequest();
        req.setFullName("Nguyen Van B");
        req.setPhone("0909888999");
        req.setVersion(5L); // Mismatched version

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userProfileRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testProfile));

        IdentityDomainException ex = assertThrows(IdentityDomainException.class, () ->
                useCase.updateProfile(testUser.getId(), req)
        );

        assertThat(ex.getErrorCode()).isEqualTo("ERR_PROFILE_VERSION_CONFLICT");
    }

    @Test
    @DisplayName("Should reject update if phone number belongs to another user")
    void shouldRejectDuplicatePhone() {
        UpdateUserProfileRequest req = new UpdateUserProfileRequest();
        req.setFullName("Nguyen Van B");
        req.setPhone("0909888999");
        req.setVersion(0L);

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userProfileRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testProfile));
        when(userProfileRepository.existsByPhoneAndUserIdNot(req.getPhone(), testUser.getId())).thenReturn(true);

        IdentityDomainException ex = assertThrows(IdentityDomainException.class, () ->
                useCase.updateProfile(testUser.getId(), req)
        );

        assertThat(ex.getErrorCode()).isEqualTo("ERR_PROFILE_PHONE_DUPLICATE");
    }

    @Test
    @DisplayName("Should upload valid avatar image and generate webp sizes")
    void shouldUploadAvatarSuccessfully() throws IOException {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(100, 100, java.awt.image.BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", baos);
        byte[] pngBytes = baos.toByteArray();

        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", pngBytes);

        when(userProfileRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testProfile));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        AvatarUploadResponse res = useCase.uploadAvatar(testUser.getId(), file);

        assertThat(res.getAvatarKey()).isNotNull().startsWith("avatars/");
        assertThat(res.getAvatarUrl()).contains("/api/v1/media/avatars/");
        verify(avatarStorage, times(3)).store(anyString(), any(), eq("image/webp"));
    }
}

