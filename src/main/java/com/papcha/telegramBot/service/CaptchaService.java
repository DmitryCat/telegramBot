package com.papcha.telegramBot.service;

import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class CaptchaService {

    private final Random random = new Random();

    public String[] generateCaptcha() {
        int a = random.nextInt(10) + 1;
        int b = random.nextInt(10) + 1;
        int result = a + b;

        String question = "Сколько будет " + a + " + " + b + "?";
        String answer = String.valueOf(result);

        return new String[]{question, answer};
    }
}
