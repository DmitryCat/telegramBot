package com.papcha.telegramBot.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BotMessage {
    private String chatId;
    private int messageId;
}

