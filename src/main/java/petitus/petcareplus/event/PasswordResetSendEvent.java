package petitus.petcareplus.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import petitus.petcareplus.model.PasswordResetToken;

@Getter
public class PasswordResetSendEvent extends ApplicationEvent {
    private final PasswordResetToken passwordResetToken;

    public PasswordResetSendEvent(Object source, PasswordResetToken passwordResetToken) {
        super(source);
        this.passwordResetToken = passwordResetToken;
    }
} 