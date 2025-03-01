package petitus.petcareplus.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import petitus.petcareplus.service.MailSenderService;

@Component
@RequiredArgsConstructor
public class Listener {
    private final MailSenderService mailSenderService;

    @EventListener
    public void onUserEmailVerificationSendEvent(UserEmailVerificationSendEvent event) {
        mailSenderService.sendUserEmailVerification(event.getEmailVerificationToken());
    }
}
