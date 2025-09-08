package com.papcha.telegramBot.config;

import com.papcha.telegramBot.bot.CaptchaBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class BotInitializer {

    private final CaptchaBot captchaBot;

    public BotInitializer(CaptchaBot captchaBot) {
        this.captchaBot = captchaBot;
    }

    @Bean
    public TelegramBotsApi telegramBotsApi() throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(captchaBot);
        System.out.println("✅ CaptchaBot зарегистрирован в TelegramBotsApi");
        return botsApi;
    }
}
