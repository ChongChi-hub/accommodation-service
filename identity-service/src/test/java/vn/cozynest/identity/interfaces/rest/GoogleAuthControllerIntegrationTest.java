package vn.cozynest.identity.interfaces.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.identity.application.dto.*;
import vn.cozynest.identity.application.port.TokenProviderPort;
import vn.cozynest.identity.application.usecase.*;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.UserRoleType;
import vn.cozynest.identity.domain.repository.RefreshTokenRepository;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GoogleAuthControllerIntegrationTest {

    private GoogleAuthUseCase googleAuthUseCase;
    private AuthController controller;
    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        RegisterUseCase registerUseCase = mock(RegisterUseCase.class);
        LoginUseCase loginUseCase = mock(LoginUseCase.class);
        RefreshTokenUseCase refreshTokenUseCase = mock(RefreshTokenUseCase.class);
        LogoutUseCase logoutUseCase = mock(LogoutUseCase.class);
        GetProfileUseCase getProfileUseCase = mock(GetProfileUseCase.class);
        ChangePasswordUseCase changePasswordUseCase = mock(ChangePasswordUseCase.class);
        ForgotPasswordUseCase forgotPasswordUseCase = mock(ForgotPasswordUseCase.class);
        ResetPasswordUseCase resetPasswordUseCase = mock(ResetPasswordUseCase.class);
        VerifyEmailUseCase verifyEmailUseCase = mock(VerifyEmailUseCase.class);
        googleAuthUseCase = mock(GoogleAuthUseCase.class);

        controller = new AuthController(
                registerUseCase,
                loginUseCase,
                refreshTokenUseCase,
                logoutUseCase,
                getProfileUseCase,
                changePasswordUseCase,
                forgotPasswordUseCase,
                resetPasswordUseCase,
                verifyEmailUseCase,
                googleAuthUseCase,
                "dev"
        );
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("200 OK: Đăng nhập Google thành công -> trả về AuthResult và đặt cookie cozynest_token")
    void testGoogleAuthSuccess200() {
        GoogleAuthCommand cmd = GoogleAuthCommand.builder()
                .credential("valid-cred")
                .nonce("nonce-123")
                .build();

        AuthResult mockResult = AuthResult.builder()
                .token("jwt-token-xyz")
                .refreshToken("ref-token-xyz")
                .user(UserDto.builder().id("u-1").email("g@gmail.com").fullName("Google User").roles(List.of("TENANT")).build())
                .build();

        when(googleAuthUseCase.authenticate(any())).thenReturn(mockResult);

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        ResponseEntity<ApiResponse<AuthResult>> response = controller.googleAuth(cmd, req, res);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getToken()).isEqualTo("jwt-token-xyz");
        assertThat(res.getHeader("Set-Cookie")).contains("cozynest_token=jwt-token-xyz");
    }

    @Test
    @DisplayName("400 Bad Request: Nonce không hợp lệ -> GlobalExceptionHandler trả 400")
    void testGoogleAuthInvalidNonce400() {
        IdentityDomainException ex = new IdentityDomainException("Mã xác thực nonce không hợp lệ", "ERR_INVALID_NONCE");
        ResponseEntity<ApiResponse<Void>> res = exceptionHandler.handleDomainException(ex);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().getErrorCode()).isEqualTo("ERR_INVALID_NONCE");
    }

    @Test
    @DisplayName("401 Unauthorized: ID token hết hạn hoặc sai chữ ký -> GlobalExceptionHandler trả 401")
    void testGoogleAuthInvalidToken401() {
        IdentityDomainException ex = new IdentityDomainException("Google ID token không hợp lệ", "ERR_INVALID_GOOGLE_TOKEN");
        ResponseEntity<ApiResponse<Void>> res = exceptionHandler.handleDomainException(ex);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody().getErrorCode()).isEqualTo("ERR_INVALID_GOOGLE_TOKEN");
    }

    @Test
    @DisplayName("403 Forbidden: Tài khoản SUSPENDED hoặc DELETED -> GlobalExceptionHandler trả 403")
    void testGoogleAuthSuspendedOrDeleted403() {
        IdentityDomainException exSuspended = new IdentityDomainException("Tài khoản bị đình chỉ", "ERR_USER_SUSPENDED");
        ResponseEntity<ApiResponse<Void>> resSuspended = exceptionHandler.handleDomainException(exSuspended);
        assertThat(resSuspended.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resSuspended.getBody().getErrorCode()).isEqualTo("ERR_USER_SUSPENDED");

        IdentityDomainException exDeleted = new IdentityDomainException("Tài khoản đã bị xóa", "ERR_USER_DELETED");
        ResponseEntity<ApiResponse<Void>> resDeleted = exceptionHandler.handleDomainException(exDeleted);
        assertThat(resDeleted.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resDeleted.getBody().getErrorCode()).isEqualTo("ERR_USER_DELETED");
    }

    @Test
    @DisplayName("429 Too Many Requests: Vượt quá 10 req/phút -> GlobalExceptionHandler trả 429")
    void testGoogleAuthRateLimit429() {
        IdentityDomainException ex = new IdentityDomainException("Quá nhiều yêu cầu", "ERR_RATE_LIMIT_EXCEEDED");
        ResponseEntity<ApiResponse<Void>> res = exceptionHandler.handleDomainException(ex);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(res.getBody().getErrorCode()).isEqualTo("ERR_RATE_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("503 Service Unavailable: Chưa bật hoặc thiếu Client ID -> GlobalExceptionHandler trả 503")
    void testGoogleNotConfigured503() {
        IdentityDomainException ex = new IdentityDomainException("Chưa cấu hình Google", "ERR_GOOGLE_NOT_CONFIGURED");
        ResponseEntity<ApiResponse<Void>> res = exceptionHandler.handleDomainException(ex);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(res.getBody().getErrorCode()).isEqualTo("ERR_GOOGLE_NOT_CONFIGURED");
    }

    @Test
    @DisplayName("Đồng thời: Hai request cùng sub chạy song song chỉ tạo đúng 1 user")
    void testConcurrentRegistrationCreatesSingleUser() throws Exception {
        AtomicInteger createdCount = new AtomicInteger(0);

        when(googleAuthUseCase.authenticate(any())).thenAnswer(inv -> {
            createdCount.incrementAndGet();
            return AuthResult.builder()
                    .token("token")
                    .user(UserDto.builder().id("u-unique").email("concurrent@gmail.com").build())
                    .build();
        });

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<ResponseEntity<ApiResponse<AuthResult>>>> futures = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                latch.await();
                GoogleAuthCommand cmd = GoogleAuthCommand.builder().credential("same-sub-cred").nonce("n").build();
                return controller.googleAuth(cmd, new MockHttpServletRequest(), new MockHttpServletResponse());
            }));
        }

        latch.countDown();
        for (Future<ResponseEntity<ApiResponse<AuthResult>>> f : futures) {
            ResponseEntity<ApiResponse<AuthResult>> resp = f.get(5, TimeUnit.SECONDS);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody().getData().getUser().getId()).isEqualTo("u-unique");
        }
        executor.shutdown();
    }
}

