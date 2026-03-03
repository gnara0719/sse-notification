package com.codeit.notification.entity;

public enum NotificationType {

    SYSTEM("시스템"),


    COMMENT("댓글"),


    LIKE("좋아요"),


    FOLLOW("팔로우"),


    MESSAGE("메시지"),


    ANNOUNCEMENT("공지사항");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
