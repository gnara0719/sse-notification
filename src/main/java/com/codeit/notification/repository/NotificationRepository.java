package com.codeit.notification.repository;

import com.codeit.notification.entity.Notification;
import com.codeit.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    /**
     * 특정 사용자의 모든 알림 조회 (최신순)
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * 특정 사용자의 읽지 않은 알림 조회
     */
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(String userId);

    /**
     * 특정 사용자의 특정 타입 알림 조회
     */
    List<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(String userId, NotificationType type);

    /**
     * 특정 사용자의 읽지 않은 알림 개수
     */
    long countByUserIdAndReadFalse(String userId);

    /**
     * 특정 기간 내 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.createdAt BETWEEN :startDate AND :endDate ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndDateRange(
            @Param("userId") String userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 오래된 읽은 알림 삭제 (배치 작업용)
     */
    void deleteByReadTrueAndCreatedAtBefore(LocalDateTime date);
}
