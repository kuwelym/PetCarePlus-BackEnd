package petitus.petcareplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;
import petitus.petcareplus.dto.request.auth.LoginRequest;
import petitus.petcareplus.dto.request.auth.RefreshTokenRequest;
import petitus.petcareplus.dto.request.auth.RegisterRequest;
import petitus.petcareplus.dto.request.auth.ResendEmailVerificationRequest;
import petitus.petcareplus.dto.response.SuccessResponse;
import petitus.petcareplus.dto.response.auth.TokenResponse;
import petitus.petcareplus.model.User;
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
        @Operation(tags = { "Authentication" }, summary = "Log in", description = "API để đăng nhập và lấy token")
        public ResponseEntity<TokenResponse> login(
                        @RequestBody @Valid final LoginRequest request) {
                return ResponseEntity.ok(authService.login(request.getEmail(), request.getPassword()));
        }

        @PostMapping("/register")
        @Operation(tags = {
                        "Authentication" }, summary = "Register Account", description = "API để đăng ký tài khoản mới")
        public ResponseEntity<SuccessResponse> register(
                        @RequestBody @Valid final RegisterRequest request) throws BindException {
                authService.register(request);

                return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.builder()
                                .message(messageSourceService.get("user_registered"))
                                .build());
        }

        @GetMapping("/refresh")
        @Operation(tags = {
                        "Authentication" }, summary = "Refresh Token", description = "API để lấy token mới từ refresh token")
        public ResponseEntity<TokenResponse> refresh(
                        @RequestBody @Valid final RefreshTokenRequest request) {
                return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
        }

        @GetMapping("/logout")
        @Operation(tags = { "Authentication" }, summary = "Log out", description = "API để đăng xuất người dùng")
        public ResponseEntity<SuccessResponse> logout(
                        @RequestHeader("Authorization") String authorization) {
                authService.logout(authorization);

                return ResponseEntity.ok(SuccessResponse.builder()
                                .message(messageSourceService.get("user_logged_out"))
                                .build());
        }

        @GetMapping("/email-verification/{tokenId}")
        @Operation(tags = {
                        "Authentication" }, summary = "Verify email", description = "API để xác thực email qua token")
        public ResponseEntity<SuccessResponse> verifyEmail(
                        @PathVariable final String tokenId) {
                userService.verifyEmail(tokenId);

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
                        "Authentication" }, summary = "Resend email verification", description = "API để gửi lại email xác thực")
        public ResponseEntity<SuccessResponse> resendEmailVerification(
                @RequestBody @Valid final ResendEmailVerificationRequest request
        ) {
                userService.resendEmailVerificationMail(request);
                return ResponseEntity.ok(SuccessResponse.builder()
                                .message(messageSourceService.get("email_verification_resent"))
                                .build());
        }

        @GetMapping("/verified")
        public String showVerifiedPage() {
                return "verified";
        }

        @Operation(tags = {
                        "Authentication" }, summary = "Get current user", description = "API để lấy thông tin user hiện tại", security = @SecurityRequirement(name = "bearerAuth"))
        @GetMapping("/me")
        public ResponseEntity<User> getCurrentUser(
                        @Parameter(name = "Authorization", description = "JWT token", required = true, in = ParameterIn.HEADER) @RequestHeader("Authorization") String token) {
                return ResponseEntity.ok(userService.getUser());
        }
}
