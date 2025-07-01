package petitus.petcareplus.dto.response.notification;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import petitus.petcareplus.utils.enums.Notifications;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminNotificationResponse {
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
