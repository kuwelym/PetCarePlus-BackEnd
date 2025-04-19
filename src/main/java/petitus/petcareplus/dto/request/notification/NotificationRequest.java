package petitus.petcareplus.dto.request.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import petitus.petcareplus.utils.enums.Notifications;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
