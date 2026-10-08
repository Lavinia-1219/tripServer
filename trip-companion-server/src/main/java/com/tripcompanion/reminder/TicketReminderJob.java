package com.tripcompanion.reminder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TicketReminderJob {

    private static final Logger log = LoggerFactory.getLogger(TicketReminderJob.class);

    private final ReminderService reminderService;

    public TicketReminderJob(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @Scheduled(
            fixedDelayString = "${app.reminder.scan-interval-ms:300000}",
            initialDelayString = "${app.reminder.initial-delay-ms:10000}"
    )
    public void scan() {
        LocalDateTime now = LocalDateTime.now();

        try {
            ReminderDtos.ScanResult result = reminderService.scan(now);

            if (result.created() > 0) {
                log.info("提醒扫描完成：候选 {} 张，新增 {} 条，跳过 {} 条（{} ~ {}）",
                        result.scanned(), result.created(), result.skipped(),
                        result.now(), result.until());
            } else {
                log.debug("提醒扫描完成：没有新的提醒（候选 {} 张）", result.scanned());
            }

        } catch (Exception e) {
            log.error("提醒扫描失败，等下一轮重试", e);
        }
    }
}
