package com.codeit.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
/**
 * 사용자별 SSE 연결을 관리하고 메시지를 전송합니다.
 */
public class SseEmitterService {

    @Value("${notification.timeout}")
    private Long timeout;

    // 사용자별 SseEmitter 저장소
    // Key: userId, Value: SseEmitter
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * SSE 연결 생성
     *
     * @return SseEmitter
     */
    public SseEmitter createEmitter(String userId) {
        SseEmitter emitter = new SseEmitter(timeout);

        if (emitters.containsKey(userId)) {
            SseEmitter oldEmitter = emitters.get(userId);
            oldEmitter.complete();
            log.info("기존 SSE 연결 종료 - 사용자: {}", userId);
        }
        // 새 연결 저장
        emitters.put(userId, emitter);

        // 연결 종료 시 Map에서도 Emitter 객체 제거
        emitter.onCompletion(() -> {
            emitters.remove(userId);
            log.info("SSE 연결 완료 - 사용자: {}, 남은 연결 수: {}", userId, emitters.size());
        });

        emitter.onTimeout(() -> {
            emitters.remove(userId);
            log.info("SSE 연결 타임아웃 - 사용자: {}, 남은 연결 수: {}", userId, emitters.size());
        });

        emitter.onError(e -> {
            emitters.remove(userId);
            log.info("SSE 연결 에러 - 사용자: {}, 에러: {}", userId, e.getMessage());
        });

        // 연결 확인용 초기 메시지 전달
        try {
            emitter.send(
              SseEmitter.event()
                      .name("connect")
                      .data("SSE 연결이 수립되었습니다.")
            );
        } catch (IOException e) {
            log.error("초기 메시지 전송 실패 - 사용자: {}", userId, e);
            emitters.remove(userId);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * 특정 사용자에게 알림 전송
     */
    public void sendToUser(String userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);

        if (emitter == null) {
            log.warn("SSE 연결을 찾을 수 없음 - 사용자: {}", userId);
            return;
        }

        try {
            emitter.send(
                    SseEmitter.event()
                            .name(eventName)
                            .data(data)
            );
            log.info("알림 전송 성공 - 사용자: {}, 이벤트: {}", userId, eventName);
        } catch (IOException e) {
            log.error("알림 전송 실패 - 사용자: {}, 이벤트: {}", userId, eventName, e);
            emitters.remove(userId);
            emitter.completeWithError(e);
        }
    }

    /**
     * 모든 사용자에게 브로드캐스트
     */
    public void broadcast(String eventName, Object data) {
        log.info("브로드캐스트 시작 - 이벤트: {}, 대상: {}명", eventName, emitters.size());

        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(
                        SseEmitter.event()
                                .name(eventName)
                                .data(data)
                );
                log.info("알림 전송 성공 - 사용자: {}, 이벤트: {}", userId, eventName);
            } catch (IOException e) {
                log.error("알림 전송 실패 - 사용자: {}, 이벤트: {}", userId, eventName, e);
                emitters.remove(userId);
                emitter.completeWithError(e);
            }
        });
    }

    /**
     * 특정 사용자의 연결 종료
     */
    public void closeEmitter(String userId) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            emitter.complete();
            emitters.remove(userId);
            log.info("SSE 연결 수동 종료 - 사용자: {}", userId);
        }
    }

    /**
     * 모든 연결 종료
     */
    public void closeAllEmitters() {
        log.info("모든 SSE 연결 종료 시작 - 총 {}개", emitters.size());
        emitters.forEach((userId, emitter) -> {
            emitter.complete();
        });
        emitters.clear();
        log.info("모든 SSE 연결 종료 완료");
    }

    /**
     * 현재 연결된 사용자 수
     */
    public int getConnectedUserCount() {
        return emitters.size();
    }

    /**
     * 특정 사용자가 연결되어 있는지 확인
     */
    public boolean isConnected(String userId) {
        return emitters.containsKey(userId);
    }
}
