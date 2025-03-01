package petitus.petcareplus.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;
import petitus.petcareplus.dto.request.auth.LoginRequest;
import petitus.petcareplus.dto.request.auth.RefreshTokenRequest;
import petitus.petcareplus.dto.request.auth.RegisterRequest;
import petitus.petcareplus.dto.response.SuccessResponse;
import petitus.petcareplus.dto.response.auth.TokenResponse;
import petitus.petcareplus.service.AuthService;
import petitus.petcareplus.service.MessageSourceService;
import petitus.petcareplus.service.UserService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final MessageSourceService messageSourceService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @RequestBody @Valid final LoginRequest request
    ) {
        return ResponseEntity.ok(authService.login(request.getEmail(), request.getPassword()));
    }

    @PostMapping("/register")
    public ResponseEntity<SuccessResponse> register(
            @RequestBody @Valid final RegisterRequest request
    ) throws BindException {
        authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.builder()
                .message(messageSourceService.get("user_registered"))
                .build());
    }

    @GetMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @RequestBody @Valid final RefreshTokenRequest request
    ) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    @GetMapping("/logout")
    public ResponseEntity<SuccessResponse> logout(
            @RequestHeader("Authorization") String authorization
    ) {
        authService.logout(authorization);

        return ResponseEntity.ok(SuccessResponse.builder()
                .message(messageSourceService.get("user_logged_out"))
                .build());
    }

    @GetMapping("/email-verification/{token}")
    public ResponseEntity<SuccessResponse> verifyEmail(
            @PathVariable final String token
    ) {
        userService.verifyEmail(token);

        return ResponseEntity.ok(SuccessResponse.builder()
                .message(messageSourceService.get("email_verified"))
                .build());
    }
}
