package com.codeit.notification.service;

import com.codeit.notification.entity.Notification;
import com.codeit.notification.entity.NotificationType;
import com.codeit.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SseEmitterService sseEmitterService;

    @Transactional
    public void createAndSendNotification(String userId, NotificationType type, String title, String message) {

        // 알림 객체 생성 -> DB 저장
        Notification notification = Notification.create(userId, type, title, message);
        notification = notificationRepository.save(notification);

        sendNotificationToUser(userId, notification);
    }

    @Transactional
    public void createAndSendNotificationWithLink(String userId, NotificationType type, String title, String message, String link) {

        // 알림 객체 생성 -> DB 저장
        Notification notification = Notification.createWithLink(userId, type, title, message, link);
        notification = notificationRepository.save(notification);

        sendNotificationToUser(userId, notification);
    }

    /**
     * SSE로 알림 전송 (비동기)
     * @param userId
     * @param notification
     */
    @Async
    public void sendNotificationToUser(String userId, Notification notification) {
        if (sseEmitterService.isConnected(userId)) {
            sseEmitterService.sendToUser(userId, "notification", notification);
            log.info("실시간 알림 전송 성공 - 사용자: {}, 알림 ID: {}", userId, notification.getId());
        }else
            log.info("사용자가 연결되어 있지 않음 - 사용자: {}, 알림은 DB에 저장됨", userId);
    }

    /**
     * 특정 사용자의 모든 알림 조회
     */
    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 특정 사용자의 읽지 않은 알림 조회
     */
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(String userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    /**
     * 특정 사용자의 특정 타입 알림 조회
     */
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByType(String userId, NotificationType type) {
        return notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type);
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다: " + notificationId));

        notification.markAsRead();
        notificationRepository.save(notification);

        log.info("알림 읽음 처리 - ID: {}, 사용자: {}", notificationId, notification.getUserId());
    }

    /**
     * 모든 알림 읽음 처리
     */
    @Transactional
    public void markAllAsRead(String userId) {
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);

        unreadNotifications.forEach(Notification::markAsRead);
        notificationRepository.saveAll(unreadNotifications);

        log.info("모든 알림 읽음 처리 - 사용자: {}, 개수: {}", userId, unreadNotifications.size());
    }

    /**
     * 알림 삭제
     */
    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
        log.info("알림 삭제 - ID: {}", notificationId);
    }

    /**
     * 특정 기간 내 알림 조회
     */
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByDateRange(String userId, LocalDateTime startDate, LocalDateTime endDate) {
        return notificationRepository.findByUserIdAndDateRange(userId, startDate, endDate);
    }

    /**
     * 오래된 읽은 알림 정리 (배치 작업)
     */
    @Transactional
    public void cleanupOldNotifications(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        notificationRepository.deleteByReadTrueAndCreatedAtBefore(cutoffDate);
        log.info("오래된 알림 정리 완료 - 기준일: {}", cutoffDate);
    }
}
