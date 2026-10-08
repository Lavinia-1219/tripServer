package com.tripcompanion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@SpringBootApplication
@EnableScheduling
public class TripCompanionApplication {

    private static final Logger log = LoggerFactory.getLogger(TripCompanionApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(TripCompanionApplication.class, args);

        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(zone);
        log.info("运行环境时区: {} ({})，当前时间: {}，UTC 时间: {}",
                zone.getId(),
                now.getOffset(),
                LocalDateTime.now(),
                ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime());
    }
}
