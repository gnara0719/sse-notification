package com.codeit.notification.controller;

import com.codeit.notification.entity.Notification;
import com.codeit.notification.entity.NotificationType;
import com.codeit.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 알림 생성 (테스트용)
     */
    @PostMapping
    public ResponseEntity<Notification> createNotification(@RequestBody NotificationRequest request) {
        log.info("알림 생성 요청 - 사용자: {}, 타입: {}", request.userId(), request.type());

        Notification notification = notificationService.createAndSendNotification(
                request.userId(),
                request.type(),
                request.title(),
                request.message()
        );

        return ResponseEntity.ok(notification);
    }

    /**
     * 사용자 알림 조회
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getUserNotifications(@PathVariable String userId) {
        List<Notification> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 읽지 않은 알림 개수
     */
    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable String userId) {
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * 알림 읽음 처리
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

    /**
     * 알림 생성 요청 DTO
     */
    public record NotificationRequest(
            String userId,
            NotificationType type,
            String title,
            String message
    ) {}

    public record AnnouncementRequest(String title, String message) {}
}
