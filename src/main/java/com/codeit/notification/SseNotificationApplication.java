package com.codeit.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SseNotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(SseNotificationApplication.class, args);
    }

}
