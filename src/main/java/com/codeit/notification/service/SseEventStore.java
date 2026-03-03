package com.codeit.notification.service;

import com.codeit.notification.model.SseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SseEventStore {

    // 알림에 붙일 고유 번호 (여러 스레드가 동시에 번호를 공유하도록, 같은 번호가 절대 뽑히지 않게끔)
    private final AtomicLong eventIdGenerator = new AtomicLong(0);

    // 사용자별로 개인 보관함을 제작해서 알림을 순차적으로 쌓아놓겠다.
    private final Map<String, List<SseEvent>> eventStore = new ConcurrentHashMap<>();

    // 이벤트 ID만으로 좀 더 빠르게 특정 이벤트를 찾기 위한 Map
    private final Map<String, SseEvent> eventIndex = new ConcurrentHashMap<>();

    private static final int MAX_EVENTS_PER_USER = 100;
    private static final int EVENT_RETENTION_MINUTES = 30;

    public String generateEventId() {
        return String.valueOf(eventIdGenerator.incrementAndGet());
    }

    /**
     * 이벤트 저장
     *
     * @return
     */
    public String saveEvent(String userId, String eventName, Object data) {
        String eventId = generateEventId();
        SseEvent event = SseEvent.create(eventId, userId, eventName, data);

        // Map에서 특정 키에 해당하는 값이 존재하는지 확인한 후 없으면 새로 만들어서 넣어주는 메서드
        eventStore.computeIfAbsent(userId,
                k -> Collections.synchronizedList(new ArrayList<>())).add(event);
        eventIndex.put(eventId, event);

        // 최대 개수 초과 시 오래된 이벤트부터 제거
        List<SseEvent> userEvents = eventStore.get(userId);
        if (userEvents.size() > MAX_EVENTS_PER_USER) {
            SseEvent removedEvent = userEvents.remove(0);
            eventIndex.remove(removedEvent.getEventId());
        }

        return eventId;
    }

    /**
     * 특정 이벤트 ID 이후의 모든 이벤트 조회
     *
     * @return
     */
    public List<SseEvent> getEventSince(String userId, String lastEventId) {
        List<SseEvent> userEvents = eventStore.get(userId);

        // 1. 보관함이 비었다면 빈 목록을 반환
        if (userEvents == null || userEvents.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 마지막으로 받은 ID를 모른다고 하면 다 반환
        if (lastEventId == null || lastEventId.isEmpty()) {
            return new ArrayList<>(userEvents);
        }

        // 3. 마지막으로 받은 번호 이후의 알림만 골라내기
        return userEvents.stream()
                .filter(event -> Long.parseLong(event.getEventId()) >= Long.parseLong(lastEventId))
                .collect(Collectors.toList());
    }

    /**
     * 주기적으로 오래된 이벤트 정리
     */
    @Scheduled(fixedRate = 600000) // 10분마다
    public void cleanupOldEvents() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(EVENT_RETENTION_MINUTES);

        for (Map.Entry<String, List<SseEvent>> entry : eventStore.entrySet()) {
            List<SseEvent> userEvents = entry.getValue();
            // 30분보다 더 전에 만들어진 알림들만 골라내기
            List<SseEvent> eventsToRemove = userEvents.stream()
                    .filter(event -> event.getCreatedAt().isBefore(cutoffTime))
                    .collect(Collectors.toList());

            // 오래된 알림을 보관함에서 갖다 버리기
            for (SseEvent event : eventsToRemove) {
                userEvents.remove(event);
                eventIndex.remove(event.getEventId());
            }

            if (userEvents.isEmpty()) {
                eventStore.remove(entry.getKey());
            }
        }
    }
}
