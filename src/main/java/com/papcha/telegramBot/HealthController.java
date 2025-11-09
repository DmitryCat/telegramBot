package com.papcha.telegramBot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    @GetMapping("/ping")
    public String ping() {
        logger.info("Ping received from Render or UptimeRobot");
        return "pong";
    }
}
