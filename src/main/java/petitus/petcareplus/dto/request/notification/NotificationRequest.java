package petitus.petcareplus.dto.request.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NonNull;
import petitus.petcareplus.utils.enums.Notifications;

import java.util.UUID;

@Data
public class NotificationRequest {

    @NonNull
    private UUID userIdReceive;
    @NonNull
    private Notifications type;
    private String imageUrl;
    @NotBlank
    private String message;
    private String title;
    @NonNull
    private UUID relatedId;

}
