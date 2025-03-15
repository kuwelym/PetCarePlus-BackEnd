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

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;

    private final UserService userService;

    private final RoleService roleService;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final MessageSourceService messageSourceService;

    private final HttpServletRequest httpServletRequest;

    private final JwtTokenProvider jwtTokenProvider;

    private final JwtTokenService jwtTokenService;

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
                .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("user_not_found")));
        if (user.getEmailVerifiedAt() == null) {
            throw new ResourceNotFoundException(messageSourceService.get("email_not_verified"));
        }

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(email, password);
        try {
            Authentication authentication = authenticationManager.authenticate(authenticationToken);
            return generateTokens(((JwtUserDetails) authentication.getPrincipal()).getId());

        } catch (Exception e) {
            throw new AuthenticationCredentialsNotFoundException(badCredentialsMessage);
        }
    }

    public void register(RegisterRequest request) throws BindException {
        User user = createUser(request);
        user.setRole(roleService.findByName(Constants.RoleEnum.USER));

        userRepository.save(user);

        userService.emailVerificationEventPublisher(user);

        generateTokens(user.getId());
    }

    /**
     * Refreshes the access token, restrains the refresh token
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
                                .build()
                )
                .build();
    }

    public void logout(final String bearerToken) {
        String token = jwtTokenProvider.extractJwtFromBearerString(bearerToken);
        UUID userId = UUID.fromString(jwtTokenProvider.getUserIdFromToken(token));
        JwtToken jwtToken = jwtTokenService.findByUserIdAndToken(userId, token);

        jwtTokenService.delete(jwtToken);
    }

    public TokenResponse generateTokens(final UUID id){
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
                                .build()
                )
                .build();
    }
}
