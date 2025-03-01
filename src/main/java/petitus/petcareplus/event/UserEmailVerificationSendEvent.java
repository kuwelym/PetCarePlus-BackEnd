package petitus.petcareplus.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import petitus.petcareplus.model.EmailVerificationToken;

@Getter
public class UserEmailVerificationSendEvent extends ApplicationEvent {
    private final EmailVerificationToken emailVerificationToken;

    public UserEmailVerificationSendEvent(Object source, EmailVerificationToken emailVerificationToken) {
        super(source);
        this.emailVerificationToken = emailVerificationToken;
    }
}