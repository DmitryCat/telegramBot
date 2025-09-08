package com.papcha.telegramBot.service;
import com.papcha.telegramBot.model.BotMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BotMessageService {

    private final List<BotMessage> messages = new ArrayList<>();

    public void add(String chatId, int messageId) {
        messages.add(new BotMessage(chatId, messageId));
    }

    public List<BotMessage> getAll() {
        return new ArrayList<>(messages);
    }

    public void remove(BotMessage message) {
        messages.remove(message);
    }
}