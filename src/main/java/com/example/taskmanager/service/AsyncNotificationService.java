package com.example.taskmanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AsyncNotificationService.class);

    @Async
    public void sendTaskCreatedNotification(Long taskId, String title, String ownerEmail) {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("[ASYNC] Уведомление отправлено: задача #{} '{}' создана пользователем {}",
                taskId, title, ownerEmail);
    }

    @Async
    public void sendTaskDeletedNotification(Long taskId) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("[ASYNC] Уведомление отправлено: задача #{} удалена", taskId);
    }
}