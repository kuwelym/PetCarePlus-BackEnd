package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import petitus.petcareplus.exceptions.BadRequestException;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.EmailVerificationToken;
import petitus.petcareplus.model.User;
import petitus.petcareplus.repository.EmailVerificationTokenRepository;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static petitus.petcareplus.utils.Constants.EMAIL_VERIFICATION_TOKEN_LENGTH;

@Service
@RequiredArgsConstructor
public class EmailVerificationTokenService {
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;

    private final MessageSourceService messageSourceService;

    @Value("${application.email.otp.expiration}")
    private Long expiresIn;

    public boolean isEmailVerificationTokenExpired(EmailVerificationToken token) {
        return token.getExpirationDate().before(Date.from(Instant.now()));
    }

    /**
     * Create email verification token from user.
     */
    public EmailVerificationToken create(User user) {
        String newToken = RandomStringUtils.randomNumeric(EMAIL_VERIFICATION_TOKEN_LENGTH);
        Date expirationDate = Date.from(Instant.now().plusSeconds(expiresIn));
        Optional<EmailVerificationToken> oldToken = emailVerificationTokenRepository.findByUserId(user.getId());
        EmailVerificationToken emailVerificationToken;

        if (oldToken.isPresent()) {
            emailVerificationToken = oldToken.get();
            emailVerificationToken.setToken(newToken);
            emailVerificationToken.setExpirationDate(expirationDate);
        } else {
            emailVerificationToken = EmailVerificationToken.builder()
                    .user(user)
                    .token(newToken)
                    .expirationDate(Date.from(Instant.now().plusSeconds(expiresIn)))
                    .build();
        }

        return emailVerificationTokenRepository.save(emailVerificationToken);
    }

    public User getUserByToken(String token) {
        EmailVerificationToken emailVerificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("not_found_with_param",
                        new String[]{messageSourceService.get("token")})));

        if (isEmailVerificationTokenExpired(emailVerificationToken)) {
            throw new BadRequestException(messageSourceService.get("expired_with_param",
                    new String[]{messageSourceService.get("token")}));
        }

        return emailVerificationToken.getUser();
    }

    public void deleteByUserId(UUID userId) {
        emailVerificationTokenRepository.deleteByUserId(userId);
    }
}