package com.grid07.socialmedia.service;

import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final RedisService redisService;

    public NotificationService(RedisService redisService) {
        this.redisService = redisService;
    }

    // Called when a bot interacts with a user's post
    public void handleBotNotification(Long userId, String botName) {

        String cooldownKey = "notif_cooldown:user_" + userId;
        String pendingKey  = "user:" + userId + ":pending_notifs";
        String message     = botName + " replied to your post";

        // Check if user was notified in last 15 minutes
        if (redisService.exists(cooldownKey)) {

            // YES → add to pending queue
            redisService.pushToList(pendingKey, message);
            System.out.println("Notification queued for user: " + userId);

        } else {

            // NO → send immediately + set 15 min cooldown
            System.out.println("Push Notification Sent to User: " + userId
                               + " → " + message);
            redisService.setWithTTL(cooldownKey, "1", 15);
        }
    }
}