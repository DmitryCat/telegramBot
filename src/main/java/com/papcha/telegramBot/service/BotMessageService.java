package com.papcha.telegramBot.service;

import com.papcha.telegramBot.model.BotMessage;
import org.springframework.stereotype.Service;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class BotMessageService {

    private final Queue<BotMessage> messages = new ConcurrentLinkedQueue<>();

    public void add(String chatId, int messageId) {
        messages.add(new BotMessage(chatId, messageId));
    }

    public BotMessage poll() {
        return messages.poll(); // достаёт и удаляет из очереди
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }
}
