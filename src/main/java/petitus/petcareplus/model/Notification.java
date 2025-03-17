package petitus.petcareplus.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import petitus.petcareplus.utils.enums.Notifications;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name ="Notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Notification {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(nullable = false)
    private UUID userIdSend;

    @Column(nullable = false)
    private UUID userIdReceive;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Notifications type;

    private String imageUrl;

    private String title;

    private String message;

    @Column(nullable = false)
    private UUID relatedId;

    private Boolean isRead;

    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        isRead = false;
    }

}
