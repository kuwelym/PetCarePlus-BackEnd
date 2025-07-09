package petitus.petcareplus.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import petitus.petcareplus.dto.request.auth.RegisterRequest;
import petitus.petcareplus.dto.request.auth.ChangePasswordRequest;
import petitus.petcareplus.dto.request.auth.ForgotPasswordRequest;
import petitus.petcareplus.dto.request.auth.ResetPasswordRequest;
import petitus.petcareplus.dto.request.auth.VerifyPasswordRequest;
import petitus.petcareplus.dto.response.auth.TokenResponse;
import petitus.petcareplus.exceptions.RefreshTokenExpireException;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.JwtToken;
import petitus.petcareplus.model.User;
import petitus.petcareplus.repository.UserRepository;
import petitus.petcareplus.security.jwt.JwtTokenProvider;
import petitus.petcareplus.security.jwt.JwtUserDetails;
import petitus.petcareplus.utils.Constants;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import petitus.petcareplus.event.PasswordResetSendEvent;
import petitus.petcareplus.model.PasswordResetToken;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;

    private final UserService userService;

    private final RoleService roleService;

    private final ProfileService profileService;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final MessageSourceService messageSourceService;

    private final HttpServletRequest httpServletRequest;

    private final JwtTokenProvider jwtTokenProvider;

    private final JwtTokenService jwtTokenService;

    private final PasswordResetTokenService passwordResetTokenService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    private User createUser(RegisterRequest request) throws BindException {
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "request");
        userRepository.findByEmail(request.getEmail())
                .ifPresent(user -> bindingResult.addError(new FieldError(bindingResult.getObjectName(), "email",
                        messageSourceService.get("unique_email"))));

        if (bindingResult.hasErrors()) {
            throw new BindException(bindingResult);
        }

        return User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .lastName(request.getLastName())
                .build();
    }

    public TokenResponse login(String email, String password) {
        String badCredentialsMessage = messageSourceService.get("bad_credentials");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException(messageSourceService.get("user_not_found")));
        if (user.getEmailVerifiedAt() == null) {
            throw new BadCredentialsException(messageSourceService.get("email_not_verified"));
        }

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(email,
                password);
        try {
            Authentication authentication = authenticationManager.authenticate(authenticationToken);
            return generateTokens(((JwtUserDetails) authentication.getPrincipal()).getId());

        } catch (Exception e) {
            throw new BadCredentialsException(badCredentialsMessage);
        }
    }

    @Transactional
    public void register(RegisterRequest request) throws BindException {
        User user = createUser(request);
        user.setRole(roleService.findByName(Constants.RoleEnum.USER));

        User savedUser = userRepository.save(user);

        profileService.createDefaultProfile(savedUser);

        userService.emailVerificationEventPublisher(savedUser);

        generateTokens(savedUser.getId());
    }

    /**
     * Refreshes the access token, restrains the refresh token
     * 
     * @param refreshToken the refresh token
     */
    public TokenResponse refresh(final String refreshToken) {

        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new RefreshTokenExpireException(messageSourceService.get("refresh_token_expired"));
        }

        UUID userId = UUID.fromString(jwtTokenProvider.getUserIdFromToken(refreshToken));
        JwtToken oldToken = jwtTokenService.findByUserIdAndRefreshToken(userId, refreshToken);

        String newAccessToken = jwtTokenProvider.generateToken(userId.toString());

        oldToken.setToken(newAccessToken);
        jwtTokenService.save(oldToken);

        return TokenResponse.builder()
                .token(newAccessToken)
                .refreshToken(refreshToken)
                .expiresIn(
                        TokenResponse.TokenExpirationResponse.builder()
                                .token(jwtTokenProvider.getTokenExpiresIn())
                                .refreshToken(jwtTokenProvider.getRefreshTokenExpiresIn())
                                .build())
                .build();
    }

    public void logout(final String bearerToken) {
        String token = jwtTokenProvider.extractJwtFromBearerString(bearerToken);
        UUID userId = UUID.fromString(jwtTokenProvider.getUserIdFromToken(token));
        JwtToken jwtToken = jwtTokenService.findByUserIdAndToken(userId, token);

        jwtTokenService.delete(jwtToken);
    }

    public TokenResponse generateTokens(final UUID id) {
        String token = jwtTokenProvider.generateToken(id.toString());
        String refreshToken = jwtTokenProvider.generateRefreshToken(id.toString());

        jwtTokenService.save(JwtToken.builder()
                .userId(id)
                .token(token)
                .refreshToken(refreshToken)
                .ipAddress(httpServletRequest.getRemoteAddr())
                .tokenTTL(jwtTokenProvider.getRefreshTokenExpiresIn())
                .build());

        return TokenResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .expiresIn(
                        TokenResponse.TokenExpirationResponse.builder()
                                .token(jwtTokenProvider.getTokenExpiresIn())
                                .refreshToken(jwtTokenProvider.getRefreshTokenExpiresIn())
                                .build())
                .build();
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException(
                    messageSourceService.get("authentication_credentials_not_found"));
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof JwtUserDetails userDetails)) {
            throw new AuthenticationCredentialsNotFoundException(
                    messageSourceService.get("authentication_credentials_not_found"));
        }

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("user_not_found")));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException(messageSourceService.get("current_password_incorrect"));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("user_not_found")));

        PasswordResetToken token = passwordResetTokenService.create(user);
        eventPublisher.publishEvent(new PasswordResetSendEvent(this, token));
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        User user = passwordResetTokenService.getUserByToken(request.getToken());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Delete the used token
        passwordResetTokenService.deleteByUserId(user.getId());
    }

    /**
     * Verify current user's password
     *
     * @param request the password verification request
     */
    public void verifyPassword(VerifyPasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException(
                    messageSourceService.get("insufficient_authentication"));
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof JwtUserDetails userDetails)) {
            throw new AuthenticationCredentialsNotFoundException(
                    messageSourceService.get("insufficient_authentication"));
        }

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("user_not_found")));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException(messageSourceService.get("bad_credentials"));
        }

    }
}
