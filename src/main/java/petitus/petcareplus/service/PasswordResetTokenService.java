package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import petitus.petcareplus.exceptions.BadRequestException;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.PasswordResetToken;
import petitus.petcareplus.model.User;
import petitus.petcareplus.repository.PasswordResetTokenRepository;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static petitus.petcareplus.utils.Constants.PASSWORD_RESET_TOKEN_LENGTH;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private final MessageSourceService messageSourceService;

    @Value("${application.email.otp.expiration}")
    private Long expiresIn;

    public boolean isPasswordResetTokenExpired(PasswordResetToken token) {
        return token.getExpirationDate().before(Date.from(Instant.now()));
    }

    @Transactional
    public PasswordResetToken create(User user) {
        String newToken = RandomStringUtils.randomNumeric(PASSWORD_RESET_TOKEN_LENGTH);
        Date expirationDate = Date.from(Instant.now().plusMillis(expiresIn));
        Optional<PasswordResetToken> oldToken = passwordResetTokenRepository.findByUserId(user.getId());
        PasswordResetToken passwordResetToken;

        if (oldToken.isPresent()) {
            passwordResetToken = oldToken.get();
            passwordResetToken.setToken(newToken);
            passwordResetToken.setExpirationDate(expirationDate);
        } else {
            passwordResetToken = PasswordResetToken.builder()
                    .user(user)
                    .token(newToken)
                    .expirationDate(expirationDate)
                    .build();
        }

        return passwordResetTokenRepository.save(passwordResetToken);
    }

    public User getUserByToken(String token) {
        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("not_found_with_param",
                        new String[]{messageSourceService.get("token")})));

        if (isPasswordResetTokenExpired(passwordResetToken)) {
            throw new BadRequestException(messageSourceService.get("expired_with_param",
                    new String[]{messageSourceService.get("token")}));
        }

        return passwordResetToken.getUser();
    }

    @Transactional
    public void deleteByUserId(UUID userId) {
        passwordResetTokenRepository.deleteByUserId(userId);
    }
} 