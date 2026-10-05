package com.example.taskmanager.service;

import com.example.taskmanager.model.Status;
import com.example.taskmanager.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ScheduledTaskChecker {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskChecker.class);

    private final TaskRepository taskRepository;

    public ScheduledTaskChecker(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    // Каждые 60 секунд: считаем сколько задач в каждом статусе
    @Scheduled(fixedRate = 60_000, initialDelay = 10_000)
    public void logTaskStatistics() {
        long newCount = taskRepository.findByStatus(Status.NEW).size();
        long inProgress = taskRepository.findByStatus(Status.IN_PROGRESS).size();
        long done = taskRepository.findByStatus(Status.DONE).size();

        log.info("[SCHEDULED] {} — статистика: NEW={}, IN_PROGRESS={}, DONE={}",
                LocalDateTime.now(), newCount, inProgress, done);
    }

    // Каждый день в 3 утра: напоминание о незавершённых задачах
    @Scheduled(cron = "0 0 3 * * *")
    public void dailyReport() {
        long pending = taskRepository.findByStatus(Status.NEW).size()
                + taskRepository.findByStatus(Status.IN_PROGRESS).size();
        if (pending > 0) {
            log.warn("[SCHEDULED] Ежедневный отчёт: {} незавершённых задач(и)", pending);
        } else {
            log.info("[SCHEDULED] Ежедневный отчёт: все задачи выполнены");
        }
    }
}