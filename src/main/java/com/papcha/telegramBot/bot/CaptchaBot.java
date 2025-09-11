package com.papcha.telegramBot.bot;

import com.papcha.telegramBot.model.BotMessage;
import com.papcha.telegramBot.model.UserCaptcha;
import com.papcha.telegramBot.service.BotMessageService;
import com.papcha.telegramBot.service.CaptchaService;
import com.papcha.telegramBot.service.UserStateService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.List;

@Component
@RequiredArgsConstructor
@EnableScheduling
public class CaptchaBot extends TelegramLongPollingBot {

    private final CaptchaService captchaService;
    private final UserStateService userStateService;
    private final BotMessageService botMessageService;

    // Spring подхватит значение из application.properties или переменной окружения
    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
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
        // ✅ Новый участник зашёл в группу
        if (update.hasMessage() && update.getMessage().getNewChatMembers() != null) {
            update.getMessage().getNewChatMembers().forEach(user -> {
                Long groupChatId = update.getMessage().getChatId();
                Long userId = user.getId();

                // Ограничим права
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

                // Сохраняем пользователя
                UserCaptcha uc = new UserCaptcha(
                        groupChatId,
                        userId,
                        userId,
                        null,
                        System.currentTimeMillis() + 60000,
                        user.getUserName()
                );
                userStateService.addUser(uc);

                // Сообщение в группу (кладём в очередь для удаления)
                SendMessage groupMsg = new SendMessage(groupChatId.toString(),
                        "👋 @" + user.getUserName() +
                                ", чтобы писать в чат, открой ЛС с ботом @" + getBotUsername() +
                                " и нажми /start. У тебя есть 60 секунд ⏰");
                try {
                    org.telegram.telegrambots.meta.api.objects.Message sent = execute(groupMsg);
                    botMessageService.add(sent.getChatId().toString(), sent.getMessageId());
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            });
        }

        // ✅ ЛС с ботом
        if (update.hasMessage() && update.getMessage().hasText()
                && update.getMessage().getChat().isUserChat()) {

            Long userId = update.getMessage().getFrom().getId();
            String text = update.getMessage().getText();

            // Если юзер нажал /start → отправляем капчу
            if ("/start".equalsIgnoreCase(text.trim())) {
                if (userStateService.contains(userId)) {
                    String[] captcha = captchaService.generateCaptcha();
                    String question = captcha[0];
                    String answer = captcha[1];

                    UserCaptcha uc = userStateService.getUser(userId);
                    uc.setAnswer(answer);

                    try {
                        execute(new SendMessage(userId.toString(),
                                "Привет! Реши капчу: " + question + " (60 секунд)"));
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        execute(new SendMessage(userId.toString(),
                                "Ты не верифицируешься для чата сейчас 🙂"));
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
                return;
            }

            // ✅ Проверка ответа на капчу
            if (userStateService.contains(userId)) {
                UserCaptcha uc = userStateService.getUser(userId);

                if (uc.getAnswer() != null && uc.getAnswer().equals(text.trim())) {
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

                        // Сообщение в группу
                        org.telegram.telegrambots.meta.api.objects.Message sent =
                                execute(new SendMessage(uc.getGroupChatId().toString(),
                                        "✅ @" + uc.getUserName() + " прошёл капчу!"));
                        botMessageService.add(sent.getChatId().toString(), sent.getMessageId());

                        // Личное сообщение пользователю
                        execute(new SendMessage(userId.toString(),
                                "Капча пройдена ✅ Добро пожаловать в чат!"));

                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }

                    userStateService.removeUser(userId);
                } else if (uc.getAnswer() != null) {
                    try {
                        execute(new SendMessage(userId.toString(), "❌ Неправильно, попробуй ещё раз"));
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
                BanChatMember ban = new BanChatMember(
                        userCaptcha.getGroupChatId().toString(),
                        userCaptcha.getUserId()
                );
                try {
                    execute(ban);

                    org.telegram.telegrambots.meta.api.objects.Message sent =
                            execute(new SendMessage(userCaptcha.getGroupChatId().toString(),
                                    "⏰ @" + userCaptcha.getUserName() + " не прошёл капчу и был удалён."));
                    botMessageService.add(sent.getChatId().toString(), sent.getMessageId());

                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                userStateService.removeUser(userCaptcha.getUserId());
            }
        });
    }

    @Scheduled(fixedRate = 60000)
    public void deleteBotMessages() {
        System.out.println("🕒 deleteBotMessages запустился");
        while (!botMessageService.isEmpty()) {
            BotMessage m = botMessageService.poll();
            if (m == null) continue;

            try {
                execute(new DeleteMessage(m.getChatId(), m.getMessageId()));
                System.out.println("✅ Удалено сообщение: chatId=" + m.getChatId() + ", messageId=" + m.getMessageId());
            } catch (TelegramApiException e) {
                System.err.println("❌ Ошибка удаления: chatId=" + m.getChatId() + ", messageId=" + m.getMessageId());
                System.err.println("Причина: " + e.getMessage());
            }
        }
    }
}
