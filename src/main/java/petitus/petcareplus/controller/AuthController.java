package petitus.petcareplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;
import petitus.petcareplus.dto.request.auth.*;
import petitus.petcareplus.dto.response.user.UserResponse;
import petitus.petcareplus.dto.response.SuccessResponse;
import petitus.petcareplus.dto.response.auth.TokenResponse;
import petitus.petcareplus.service.AuthService;
import petitus.petcareplus.service.MessageSourceService;
import petitus.petcareplus.service.UserService;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Các API liên quan đến xác thực tài khoản")
public class AuthController {

    private final AuthService authService;
    private final MessageSourceService messageSourceService;
    private final UserService userService;

    @PostMapping("/login")
    @Operation(tags = {"Authentication"}, summary = "Log in", description = "API để đăng nhập và lấy token")
    public ResponseEntity<TokenResponse> login(
            @RequestBody @Valid final LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.getEmail(), request.getPassword()));
    }

    @PostMapping("/register")
    @Operation(tags = {
            "Authentication"}, summary = "Register Account", description = "API để đăng ký tài khoản mới")
    public ResponseEntity<SuccessResponse> register(
            @RequestBody @Valid final RegisterRequest request) throws BindException {
        authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.builder()
                .message(messageSourceService.get("user_registered"))
                .build());
    }

    @GetMapping("/refresh")
    @Operation(tags = {
            "Authentication"}, summary = "Refresh Token", description = "API để lấy token mới từ refresh token")
    public ResponseEntity<TokenResponse> refresh(
            @RequestBody @Valid final RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    @GetMapping("/logout")
    @Operation(tags = {"Authentication"}, summary = "Log out", description = "API để đăng xuất người dùng")
    public ResponseEntity<SuccessResponse> logout(
            @RequestHeader("Authorization") String authorization) {
        authService.logout(authorization);

        return ResponseEntity.ok(SuccessResponse.builder()
                .message(messageSourceService.get("user_logged_out"))
                .build());
    }

    @GetMapping("/email-verification/{token}")
    @Operation(tags = {
            "Authentication"}, summary = "Verify email", description = "API để xác thực email qua token")
    public ResponseEntity<SuccessResponse> verifyEmail(
            @PathVariable final String token) {
        userService.verifyEmail(token);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .header("Location", "/auth/verified")
                .body(
                        SuccessResponse.builder()
                                .message(messageSourceService.get("email_verified"))
                                .build());
    }

    @PostMapping("/resend-email-verification")
    @Operation(tags = {
            "Authentication"}, summary = "Resend email verification", description = "API để gửi lại email xác thực (có giới hạn tần suất)")
    public ResponseEntity<SuccessResponse> resendEmailVerification(
            @RequestBody @Valid final ResendEmailVerificationRequest request
    ) {
        userService.resendEmailVerificationMail(request);
        return ResponseEntity.ok(SuccessResponse.builder()
                .message(messageSourceService.get("email_verification_resent"))
                .build());
    }

    @GetMapping("/cancel-registration/{token}")
    @Operation(tags = {"Authentication"}, summary = "Cancel Registration",
            description = "API để hủy đăng ký nếu email bị sử dụng mà không có sự đồng ý.")
    public ResponseEntity<SuccessResponse> cancelRegistration(
            @PathVariable String token) {

        boolean isCanceled = userService.cancelUnverifiedRegistration(token);

        if (isCanceled) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "/auth/registration-cancelled")
                    .body(SuccessResponse.builder()
                            .message(messageSourceService.get("registration_cancelled"))
                            .build());
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("Location", "/auth/registration-cancel-failed")
                    .body(SuccessResponse.builder()
                            .message(messageSourceService.get("registration_cancel_failed"))
                            .build());
        }
    }

    @GetMapping("/registration-cancelled")
    public String showRegistrationCancelledPage() {
        return "registration-cancelled";
    }

    @GetMapping("/registration-cancel-failed")
    public String showRegistrationCancelFailedPage() {
        return "registration-cancel-failed";
    }

    @GetMapping("/verified")
    public String showVerifiedPage() {
        return "verified";
    }

    @Operation(tags = {
            "Authentication"}, summary = "Get current user", description = "API để lấy thông tin user hiện tại", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(UserResponse.convert(userService.getUser()));
    }

    @PostMapping("/change-password")
    @Operation(tags = {"Authentication"},
            summary = "Change Password",
            description = "API to change user password",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<SuccessResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(SuccessResponse.builder()
                .message(messageSourceService.get("password_changed"))
                .build());
    }

    @PostMapping("/forgot-password")
    @Operation(tags = {"Authentication"}, summary = "Forgot Password", description = "API to request password reset")
    public ResponseEntity<SuccessResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(SuccessResponse.builder()
                .message(messageSourceService.get("password_reset_email_sent"))
                .build());
    }

    @PostMapping("/reset-password")
    @Operation(tags = {"Authentication"}, summary = "Reset Password", description = "API to reset password using token")
    public ResponseEntity<SuccessResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(SuccessResponse.builder()
                .message(messageSourceService.get("password_reset_success"))
                .build());
    }

    @PostMapping("/verify-password")
    @Operation(tags = {"Authentication"}, 
            summary = "Verify Password", 
            description = "API to verify current user's password before upgrading to service provider",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<SuccessResponse> verifyPassword(@Valid @RequestBody VerifyPasswordRequest request) {
        authService.verifyPassword(request);
        return ResponseEntity.ok(SuccessResponse.builder()
                .message(messageSourceService.get("password_verified"))
                .build());
    }
}
