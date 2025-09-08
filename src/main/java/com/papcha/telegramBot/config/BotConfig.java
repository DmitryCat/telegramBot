package com.papcha.telegramBot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "telegram.bot")
public class BotConfig {

    private String username;
    private String token;

    public String getBotUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getBotToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
