package com.papcha.telegramBot.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data                   // Генерирует геттеры/сеттеры
@AllArgsConstructor     // Генерирует конструктор со всеми полями
public class UserCaptcha {
    private Long groupChatId;     // ID группы
    private Long privateChatId;   // ID лички (userId)
    private Long userId;          // ID пользователя
    private String answer;        // правильный ответ
    private long expireTime;      // время истечения
    private String userName;      // username для сообщений
}
