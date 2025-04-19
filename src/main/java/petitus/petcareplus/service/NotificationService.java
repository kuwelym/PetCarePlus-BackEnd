package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import petitus.petcareplus.dto.request.notification.NotificationRequest;
import petitus.petcareplus.dto.response.notification.NotificationResponse;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.Notification;
import petitus.petcareplus.model.User;
import petitus.petcareplus.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserService userService;

    @Transactional
    public NotificationResponse pushNotification(NotificationRequest request) {
        User user = userService.getUser();
        return pushNotificationInternal(request, user.getId());
    }

    @Transactional
    public NotificationResponse pushNotification(NotificationRequest request, UUID senderId) {
        return pushNotificationInternal(request, senderId);
    }

    private NotificationResponse pushNotificationInternal(NotificationRequest request, UUID senderId) {
        Notification notification = Notification.builder()
                .userIdSend(senderId)
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

    public List<NotificationResponse> getAllNotifications() {
        User currentUser = userService.getUser();

        return notificationRepository.findByUserIdReceive(currentUser.getId()).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public NotificationResponse getNotificationById(UUID notificationId) {
        User currentUser = userService.getUser();

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getUserIdReceive().equals(currentUser.getId())) {
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
    public void deleteNotification(UUID notificationId) {
        User currentUser = userService.getUser();

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getUserIdReceive().equals(currentUser.getId())) {
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
