package com.papcha.telegramBot.service;

import com.papcha.telegramBot.model.UserCaptcha;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserStateService {

    // Храним пользователей, которые проходят капчу
    private final Map<Long, UserCaptcha> userMap = new ConcurrentHashMap<>();

    public void addUser(UserCaptcha userCaptcha) {
        userMap.put(userCaptcha.getUserId(), userCaptcha);
    }

    public boolean contains(Long userId) {
        return userMap.containsKey(userId);
    }

    public UserCaptcha getUser(Long userId) {
        return userMap.get(userId);
    }

    public void removeUser(Long userId) {
        userMap.remove(userId);
    }

    // Вот этого метода у тебя не хватало
    public Map<Long, UserCaptcha> getUserMap() {
        return userMap;
    }
}
