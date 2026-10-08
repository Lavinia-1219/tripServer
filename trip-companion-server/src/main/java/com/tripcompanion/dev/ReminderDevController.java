package com.tripcompanion.dev;

import com.tripcompanion.auth.CurrentUser;
import com.tripcompanion.common.ApiResponse;
import com.tripcompanion.reminder.ReminderDtos;
import com.tripcompanion.reminder.ReminderService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@RestController
@Profile("dev")
@RequestMapping("/api/dev/reminder")
public class ReminderDevController {

    private final ReminderService reminderService;

    public ReminderDevController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping("/scan")
    public ApiResponse<ReminderDtos.ScanResult> scan(@CurrentUser Long userId) {
        return ApiResponse.ok(reminderService.scan(LocalDateTime.now()));
    }

    @GetMapping("/time")
    public ApiResponse<ReminderDtos.TimeCheck> time(@CurrentUser Long userId) {
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(zone);

        long uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;

        return ApiResponse.ok(new ReminderDtos.TimeCheck(
                LocalDateTime.now(),
                zone.getId(),
                now.getOffset().toString(),
                String.valueOf(System.currentTimeMillis()),
                uptime
        ));
    }
}
