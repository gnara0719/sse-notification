package com.codeit.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "notifications")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 알림을 받을 사용자 ID
     */
    @Column(nullable = false)
    private String userId;

    /**
     * 알림 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    /**
     * 알림 제목
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 알림 내용
     */
    @Column(nullable = false, length = 1000)
    private String message;

    /**
     * 관련 링크
     */
    @Column(length = 500)
    private String link;

    /**
     * 읽음 여부
     */
    @Column(nullable = false)
    private boolean read = false;

    /**
     * 생성 시간
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 읽은 시간
     */
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * 알림을 읽음 처리
     */
    public void markAsRead() {
        this.read = true;
        this.readAt = LocalDateTime.now();
    }

    /**
     * 알림 생성 팩토리 메서드
     */
    public static Notification create(String userId, NotificationType type, String title, String message) {
        return Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 링크가 있는 알림 생성 팩토리 메서드
     */
    public static Notification createWithLink(String userId, NotificationType type, String title, String message, String link) {
        return Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .link(link)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

}
