package com.service.auth.service;

import com.service.auth.dto.*;
import com.service.auth.entity.UserEntity;
import com.service.auth.entity.UserVerificationEntity;
import com.service.auth.repository.UserRepository;
import com.service.auth.repository.UserVerificationRepository;
import com.service.common.exception.AppException;
import com.service.security.JwtUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserVerificationRepository verificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    // In-memory OTP storage (can be swapped with Redis in production)
    private final Map<String, OtpInfo> otpStore = new ConcurrentHashMap<>();

    private record OtpInfo(String otp, long expiresAt) {}

    public AuthService(UserRepository userRepository,
                       UserVerificationRepository verificationRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.verificationRepository = verificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    public UserDto mapToUserDto(UserEntity user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .avatar(user.getAvatarUrl())
                .role(user.getRole())
                .isVerified(user.getIsVerified())
                .kycStatus(user.getKycStatus())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .build();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(HttpStatus.CONFLICT, "Email đã được sử dụng");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new AppException(HttpStatus.CONFLICT, "Số điện thoại đã được sử dụng");
        }

        String role = "landlord".equalsIgnoreCase(request.getRole()) ? "landlord" : "tenant";
        String userId = UUID.randomUUID().toString();

        UserEntity user = UserEntity.builder()
                .id(userId)
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .isVerified(false)
                .kycStatus("none")
                .build();

        user = userRepository.save(user);

        String token = jwtUtils.generateAccessToken(user.getId(), user.getEmail(), user.getRole(), user.getKycStatus());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .user(mapToUserDto(user))
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng");
        }

        String token = jwtUtils.generateAccessToken(user.getId(), user.getEmail(), user.getRole(), user.getKycStatus());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .user(mapToUserDto(user))
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }

    public UserDto getProfile(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
        return mapToUserDto(user);
    }

    @Transactional
    public void updateProfile(String userId, UpdateProfileRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            userRepository.findByPhone(request.getPhone()).ifPresent(existing -> {
                if (!existing.getId().equals(userId)) {
                    throw new AppException(HttpStatus.CONFLICT, "Số điện thoại đã được sử dụng bởi tài khoản khác");
                }
            });
            user.setPhone(request.getPhone());
        }

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail();
        if (userRepository.existsByEmail(email)) {
            String otp = String.format("%06d", (int) (Math.random() * 900000) + 100000);
            long expiresAt = System.currentTimeMillis() + 10 * 60 * 1000; // 10 mins
            otpStore.put(email, new OtpInfo(otp, expiresAt));
            System.out.println(">>> OTP for " + email + ": " + otp);
        }
    }

    public void verifyResetOtp(VerifyOtpRequest request) {
        OtpInfo info = otpStore.get(request.getEmail());
        if (info == null || !info.otp().equals(request.getOtp())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Mã OTP không hợp lệ");
        }
        if (System.currentTimeMillis() > info.expiresAt()) {
            otpStore.remove(request.getEmail());
            throw new AppException(HttpStatus.BAD_REQUEST, "Mã OTP đã hết hạn");
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        verifyResetOtp(new VerifyOtpRequest() {{
            setEmail(request.getEmail());
            setOtp(request.getOtp());
        }});

        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        otpStore.remove(request.getEmail());
    }

    public UserDto getUserById(String id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
        return mapToUserDto(user);
    }

    @Transactional
    public void submitKyc(String userId, KycSubmitRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        UserVerificationEntity verification = verificationRepository.findByUserId(userId)
                .orElse(UserVerificationEntity.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(userId)
                        .build());

        verification.setIdCardFront(request.getIdCardFront());
        verification.setIdCardBack(request.getIdCardBack());
        verification.setSelfieUrl(request.getSelfieUrl());
        verification.setStatus("pending");
        verification.setReviewedBy(null);
        verification.setReviewedAt(null);
        verificationRepository.save(verification);

        user.setKycStatus("pending");
        userRepository.save(user);
    }

    public List<UserVerificationEntity> listPendingKyc() {
        return verificationRepository.findByStatus("pending");
    }

    @Transactional
    public void reviewKyc(String kycId, String reviewerId, KycReviewRequest request) {
        UserVerificationEntity verification = verificationRepository.findById(kycId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ xác thực"));

        String status = "approved".equalsIgnoreCase(request.getStatus()) ? "approved" : "rejected";
        verification.setStatus(status);
        verification.setReviewedBy(reviewerId);
        verification.setReviewedAt(LocalDateTime.now());
        verificationRepository.save(verification);

        UserEntity user = userRepository.findById(verification.getUserId()).orElse(null);
        if (user != null) {
            user.setKycStatus(status);
            if ("approved".equals(status)) {
                user.setIsVerified(true);
            }
            userRepository.save(user);
        }
    }
}

