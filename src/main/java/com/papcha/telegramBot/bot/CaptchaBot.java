package com.papcha.telegramBot.bot;

import com.papcha.telegramBot.model.BotMessage;
import com.papcha.telegramBot.model.UserCaptcha;
import com.papcha.telegramBot.service.BotMessageService;
import com.papcha.telegramBot.service.CaptchaService;
import com.papcha.telegramBot.service.UserStateService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.groupadministration.BanChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.ChatPermissions;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Date;

@Component
@RequiredArgsConstructor
@EnableScheduling
public class CaptchaBot extends TelegramLongPollingBot {

    private final CaptchaService captchaService;
    private final UserStateService userStateService;
    private final BotMessageService botMessageService;

    @Override
    public String getBotUsername() {
        return "facecontro_bot";
    }

    @Override
    public String getBotToken() {
        return "-";
    }

    @PostConstruct
    public void init() {
        System.out.println("CaptchaBot создан!");
        try {
            var info = execute(new GetMe());
            System.out.println("Подключение к Telegram успешно: " + info);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        // ✅ Новый пользователь в группе
        if (update.hasMessage() && update.getMessage().getNewChatMembers() != null) {
            update.getMessage().getNewChatMembers().forEach(user -> {
                Long groupChatId = update.getMessage().getChatId();
                Long userId = user.getId();

                // Ограничим права (только читать)
                ChatPermissions noPermissions = ChatPermissions.builder()
                        .canSendMessages(false)
                        .build();

                RestrictChatMember restrict = RestrictChatMember.builder()
                        .chatId(groupChatId.toString())
                        .userId(userId)
                        .permissions(noPermissions)
                        .build();

                try {
                    execute(restrict);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

                // Генерация капчи
                String[] captcha = captchaService.generateCaptcha();
                String question = captcha[0];
                String answer = captcha[1];

                UserCaptcha userCaptcha = new UserCaptcha(
                        groupChatId,
                        userId,
                        userId,
                        answer,
                        System.currentTimeMillis() + 60000,
                        user.getUserName()
                );
                userStateService.addUser(userCaptcha);

                // Отправляем капчу в личку
                SendMessage msg = new SendMessage(userId.toString(),
                        "Привет, @" + user.getUserName() + "! Реши капчу: " + question + " (у тебя есть 60 секунд)");
                try {
                    execute(msg);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            });
        }

        // ✅ Сообщение в личке
        if (update.hasMessage() && update.getMessage().hasText()
                && update.getMessage().getChat().isUserChat()) {

            Long userId = update.getMessage().getFrom().getId();
            String text = update.getMessage().getText();

            if (userStateService.contains(userId)) {
                UserCaptcha uc = userStateService.getUser(userId);

                if (uc.getAnswer().equals(text.trim())) {
                    // Правильный ответ → снимаем ограничения в группе
                    ChatPermissions full = ChatPermissions.builder()
                            .canSendMessages(true)
                            .canSendMediaMessages(true)
                            .canSendPolls(true)
                            .canSendOtherMessages(true)
                            .build();

                    RestrictChatMember allow = RestrictChatMember.builder()
                            .chatId(uc.getGroupChatId().toString())
                            .userId(userId)
                            .permissions(full)
                            .build();

                    try {
                        execute(allow);

                        // Отправка сообщения в группу
                        SendMessage groupMsg = new SendMessage(uc.getGroupChatId().toString(),
                                "✅ @" + uc.getUserName() + " прошёл капчу! Добро пожаловать!");
                        org.telegram.telegrambots.meta.api.objects.Message sentMessage = execute(groupMsg);

                        // Сохраняем для удаления через 10 секунд
                        botMessageService.add(uc.getGroupChatId().toString(), sentMessage.getMessageId());

                        // Личное сообщение пользователю
                        execute(new SendMessage(userId.toString(), "Капча пройдена ✅"));

                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }

                    userStateService.removeUser(userId);
                } else {
                    try {
                        execute(new SendMessage(userId.toString(), "❌ Неправильно! Попробуй ещё раз."));
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    // Проверка истечения капчи
    @Scheduled(fixedRate = 10000)
    public void checkExpirations() {
        userStateService.getUserMap().values().forEach(userCaptcha -> {
            if (new Date().getTime() > userCaptcha.getExpireTime()) {
                // Время вышло → баним пользователя
                BanChatMember ban = new BanChatMember(
                        userCaptcha.getGroupChatId().toString(),
                        userCaptcha.getUserId()
                );
                try {
                    execute(ban);
                    execute(new SendMessage(userCaptcha.getGroupChatId().toString(),
                            "⏰ @" + userCaptcha.getUserName() + " не прошёл капчу и был удалён."));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                userStateService.removeUser(userCaptcha.getUserId());
            }
        });
    }
    @Scheduled(fixedRate = 30000) // каждые 10 секунд
    public void deleteBotMessages() {
        for (BotMessage m : botMessageService.getAll()) {
            try {
                execute(new DeleteMessage(m.getChatId(), m.getMessageId()));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            } finally {
                botMessageService.remove(m);
            }
        }
    }
}
