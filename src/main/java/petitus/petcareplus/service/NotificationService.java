package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import petitus.petcareplus.dto.request.notification.NotificationRequest;
import petitus.petcareplus.dto.response.notification.NotificationResponse;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.Notification;
import petitus.petcareplus.repository.NotificationRepository;
import petitus.petcareplus.security.jwt.JwtTokenProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final JwtTokenProvider jwtTokenProvider;

    private UUID extractUserId(String bearerToken) {
        String token = jwtTokenProvider.extractJwtFromBearerString(bearerToken);
        return UUID.fromString(jwtTokenProvider.getUserIdFromToken(token));
    }

    @Transactional
    public NotificationResponse pushNotification(String bearerToken, NotificationRequest request) {
        UUID userIdSend = extractUserId(bearerToken);

        Notification notification = Notification.builder()
                .userIdSend(userIdSend)
                .userIdReceive(request.getUserIdReceive())
                .type(request.getType())
                .imageUrl(request.getImageUrl())
                .title(request.getTitle())
                .message(request.getMessage())
                .relatedId(request.getRelatedId())
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
        return convertToResponse(notification);
    }

    public List<NotificationResponse> getAllNotifications(String bearerToken) {
        UUID userIdReceive = extractUserId(bearerToken);

        return notificationRepository.findByUserIdReceive(userIdReceive).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public NotificationResponse getNotificationById(String bearerToken, UUID notificationId) {
        UUID userIdReceive = extractUserId(bearerToken);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getUserIdReceive().equals(userIdReceive)) {
            throw new ResourceNotFoundException("Notification does not belong to user");
        }
        return convertToResponse(notification);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void deleteNotification(String bearerToken, UUID notificationId) {

        UUID userIdReceive = extractUserId(bearerToken);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getUserIdReceive().equals(userIdReceive)) {
            throw new ResourceNotFoundException("Notification does not belong to user");
        }

        notification.setDeletedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    private NotificationResponse convertToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userIdSend(notification.getUserIdSend())
                .userIdReceive(notification.getUserIdReceive())
                .type(notification.getType())
                .imageUrl(notification.getImageUrl())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .relatedId(notification.getRelatedId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .deletedAt(notification.getDeletedAt())
                .build();
    }
}
