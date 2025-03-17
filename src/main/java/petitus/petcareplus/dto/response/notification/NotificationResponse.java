package petitus.petcareplus.dto.response.notification;

import lombok.Builder;
import lombok.Data;
import petitus.petcareplus.utils.enums.Notifications;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class NotificationResponse {

    private UUID id;
    private UUID userIdSend;
    private UUID userIdReceive;
    private Notifications type;
    private String imageUrl;
    private String message;
    private String title;
    private UUID relatedId;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

}
