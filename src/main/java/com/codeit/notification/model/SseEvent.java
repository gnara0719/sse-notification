package com.codeit.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseEvent {

    private String eventId;
    private String userId;
    private String eventName;
    private Object data;
    private LocalDateTime createdAt;

    public static SseEvent create(String eventId, String userId, String eventName, Object data) {
        return SseEvent.builder()
                .eventId(eventId)
                .userId(userId)
                .eventName(eventName)
                .data(data)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
