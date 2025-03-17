package petitus.petcareplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import petitus.petcareplus.dto.request.notification.NotificationRequest;
import petitus.petcareplus.dto.response.notification.NotificationResponse;
import petitus.petcareplus.service.NotificationService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Các API quản lý thông báo")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @Operation(summary = "Push Notification", description = "Gửi thông báo mới", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<NotificationResponse> pushNotification(@RequestHeader(value = "Authorization") String authorization,
                                                                 @RequestBody @Valid NotificationRequest notificationRequest) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.pushNotification(authorization, notificationRequest));
    }

    @GetMapping
    @Operation(summary = "Get all notifications", description = "Lấy danh sách thông báo", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<NotificationResponse>> getAllNotifications(@RequestHeader(value = "Authorization") String authorization)  {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(notificationService.getAllNotifications(authorization));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a notifications", description = "Lấy chi tiết thông báo", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<NotificationResponse> getNotification(@RequestHeader(value = "Authorization") String authorization,
                                                                @PathVariable @Valid UUID id)  {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(notificationService.getNotificationById(authorization, id));
    }

    @PutMapping("/read/{id}")
    @Operation(summary = "Mark as read", description = "Đánh dấu đã đọc")
    public ResponseEntity<NotificationResponse> markasread(@PathVariable @Valid UUID id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete notification", description = "Xóa thông báo")
    public ResponseEntity<NotificationResponse> deleteNotification(@RequestHeader("Authorization") String authorization,
                                                                   @PathVariable @Valid UUID id) {
        notificationService.deleteNotification(authorization, id);
        return ResponseEntity.ok().build();
    }



}
