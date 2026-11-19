package vn.cozynest.identity.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.cozynest.identity.application.dto.LandlordApplicationSummaryDto;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.*;
import vn.cozynest.identity.domain.repository.LandlordProfileRepository;
import vn.cozynest.identity.domain.repository.UserRepository;
import vn.cozynest.identity.domain.repository.UserVerificationRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LandlordApplicationUseCaseTest {

    private LandlordProfileRepository landlordProfileRepository;
    private UserRepository userRepository;
    private UserVerificationRepository userVerificationRepository;
    private LandlordApplicationUseCase useCase;

    @BeforeEach
    void setUp() {
        landlordProfileRepository = mock(LandlordProfileRepository.class);
        userRepository = mock(UserRepository.class);
        userVerificationRepository = mock(UserVerificationRepository.class);

        useCase = new LandlordApplicationUseCase(
                landlordProfileRepository,
                userRepository,
                userVerificationRepository
        );
    }

    private User createVerifiedUser(String userId) {
        Set<UserRoleType> roles = new HashSet<>();
        roles.add(UserRoleType.TENANT);
        return new User(
                userId, "test@toam.vn", "0901234567", "hash", "Nguyen Van A",
                null, null, null, UserStatus.ACTIVE, Instant.now(), null,
                roles, Instant.now(), Instant.now()
        );
    }

    private UserVerification createApprovedVerification(String userId) {
        return new UserVerification(
                "v-" + userId, userId, "Nguyen Van A", LocalDate.of(1990, 1, 1),
                "enc", "hash", "1234", LocalDate.of(2020, 1, 1), "CA HN",
                "front", "back", null, KycStatus.APPROVED, Instant.now(), "v1.0",
                null, null, null, null, null, 1, 0L, Instant.now(), Instant.now()
        );
    }

    @Test
    @DisplayName("Should upgrade tenant to landlord when profile was in AWAITING_KYC")
    void shouldUpgradeTenantToLandlordWhenProfileWasInAwaitingKyc() {
        String userId = "u-tenant-01";
        User user = createVerifiedUser(userId);
        UserVerification verification = createApprovedVerification(userId);
        LandlordProfile profile = LandlordProfile.createNew(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userVerificationRepository.findByUserId(userId)).thenReturn(Optional.of(verification));
        when(landlordProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(landlordProfileRepository.save(any(LandlordProfile.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        LandlordApplicationSummaryDto result = useCase.upgradeToLandlord(userId);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(LandlordApprovalStatus.APPROVED.name());
        assertThat(user.getRoles()).contains(UserRoleType.TENANT, UserRoleType.LANDLORD);
        assertThat(profile.getApprovalStatus()).isEqualTo(LandlordApprovalStatus.APPROVED);
        assertThat(profile.getReviewedBy()).isEqualTo("AUTO_UPGRADE");
        verify(landlordProfileRepository).save(profile);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Should upgrade tenant to landlord when no prior profile exists")
    void shouldUpgradeTenantToLandlordWhenNoPriorProfileExists() {
        String userId = "u-tenant-02";
        User user = createVerifiedUser(userId);
        UserVerification verification = createApprovedVerification(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userVerificationRepository.findByUserId(userId)).thenReturn(Optional.of(verification));
        when(landlordProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(landlordProfileRepository.save(any(LandlordProfile.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        LandlordApplicationSummaryDto result = useCase.upgradeToLandlord(userId);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(LandlordApprovalStatus.APPROVED.name());
        assertThat(user.getRoles()).contains(UserRoleType.LANDLORD);
        verify(landlordProfileRepository).save(any(LandlordProfile.class));
    }

    @Test
    @DisplayName("Should upgrade tenant to landlord when profile was REJECTED previously")
    void shouldUpgradeTenantToLandlordWhenProfileWasRejected() {
        String userId = "u-tenant-03";
        User user = createVerifiedUser(userId);
        UserVerification verification = createApprovedVerification(userId);
        LandlordProfile profile = LandlordProfile.createNew(userId);
        profile.submitKyc();
        profile.reject("admin-01", "DOC_BLURRY", "Note");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userVerificationRepository.findByUserId(userId)).thenReturn(Optional.of(verification));
        when(landlordProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(landlordProfileRepository.save(any(LandlordProfile.class))).thenAnswer(i -> i.getArgument(0));

        LandlordApplicationSummaryDto result = useCase.upgradeToLandlord(userId);

        assertThat(result.getStatus()).isEqualTo(LandlordApprovalStatus.APPROVED.name());
        assertThat(profile.getApprovalStatus()).isEqualTo(LandlordApprovalStatus.APPROVED);
    }

    @Test
    @DisplayName("Should throw ERR_EMAIL_NOT_VERIFIED when user email is not verified")
    void shouldThrowWhenEmailNotVerified() {
        String userId = "u-unverified-email";
        Set<UserRoleType> roles = new HashSet<>();
        roles.add(UserRoleType.TENANT);
        User user = new User(
                userId, "unverified@toam.vn", "0900000000", "hash", "Unverified",
                null, null, null, UserStatus.ACTIVE, null, null,
                roles, Instant.now(), Instant.now()
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> useCase.upgradeToLandlord(userId))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_EMAIL_NOT_VERIFIED");
    }

    @Test
    @DisplayName("Should throw ERR_KYC_NOT_VERIFIED when user KYC is not approved")
    void shouldThrowWhenKycNotVerified() {
        String userId = "u-unverified-kyc";
        User user = createVerifiedUser(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userVerificationRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.upgradeToLandlord(userId))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_KYC_NOT_VERIFIED");
    }
}
