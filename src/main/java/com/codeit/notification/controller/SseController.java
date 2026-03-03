package com.codeit.notification.controller;

import com.codeit.notification.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sse")
@Slf4j
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterService sseEmitterService;

    // SSE 연결 생성 요청 (Content Type을 text/event-stream으로 설정)
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@RequestParam String userId) {
        log.info("SSE Connecting to user {}", userId);
        return sseEmitterService.createEmitter(userId);
    }

    // SSE 연결 종료 요청 (Content Type을 text/event-stream으로 설정)
    @GetMapping(value = "/disconnect")
    public void disconnect(@RequestParam String userId) {
        log.info("SSE Disconnecting to user {}", userId);
        sseEmitterService.closeEmitter(userId);
    }

    @GetMapping("/connection-count")
    public int getConnectionCount() {
        return sseEmitterService.getConnectedUserCount();
    }

    @GetMapping("/is-connected")
    public boolean isConnected(@RequestParam String userId) {
        return sseEmitterService.isConnected(userId);
    }


}
