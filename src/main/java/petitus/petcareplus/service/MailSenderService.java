package petitus.petcareplus.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.EmailVerificationToken;
import petitus.petcareplus.model.PasswordResetToken;
import petitus.petcareplus.model.User;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailSenderService {

    @Value("${spring.application.name}")
    private String appName;

    @Value("${spring.mail.username}")
    private String senderAddress;

    private final MessageSourceService messageSourceService;

    private final CipherService cipherService;

    private final JavaMailSender mailSender;

    private final SpringTemplateEngine templateEngine;

    @Async
    public void sendUserEmailVerification(EmailVerificationToken token) {
        User user = token.getUser();
        if (user == null) {
            throw new ResourceNotFoundException(messageSourceService.get("user_token_null"));
        }
        String verificationLink = "http://localhost:8080/auth/email-verification/" + cipherService.encryptForURL(String.valueOf(token.getId()));
        String cancelRegistrationLink = "http://localhost:8080/auth/cancel-registration/" + cipherService.encryptForURL(user.getEmail());

        Context ctx = createContext();
        ctx.setVariable("name", user.getName());
        ctx.setVariable("fullName", user.getFullName());
        ctx.setVariable("verificationLink", verificationLink);
        ctx.setVariable("cancelRegistrationLink", cancelRegistrationLink);

        String subject = messageSourceService.get("email_verification_sent");

        try {

            send(user.getEmail(),subject,
                    templateEngine.process("mail/user-email-verification", ctx));
        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage(), e);
        }
    }
    
    @Async
    public void sendPasswordResetEmail(PasswordResetToken token) {
        User user = token.getUser();
        if (user == null) {
            throw new ResourceNotFoundException(messageSourceService.get("user_token_null"));
        }
        
        Context ctx = createContext();
        ctx.setVariable("name", user.getName());
        ctx.setVariable("fullName", user.getFullName());
        ctx.setVariable("token", token.getToken());

        String subject = messageSourceService.get("password_reset_email_sent");

        try {
            send(user.getEmail(), subject,
                    templateEngine.process("mail/password-reset", ctx));
        } catch (Exception e) {
            log.error("Failed to send password reset email: {}", e.getMessage(), e);
        }
    }

    private Context createContext() {
        final Context ctx = new Context(LocaleContextHolder.getLocale());
        ctx.setVariable("SENDER_ADDRESS", senderAddress);
        ctx.setVariable("APP_NAME", appName);

        return ctx;
    }

    private void send(
                      String to,
                      String subject,
                      String text) throws MessagingException, MailException {

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text, true);
        mailSender.send(mimeMessage);
    }
}