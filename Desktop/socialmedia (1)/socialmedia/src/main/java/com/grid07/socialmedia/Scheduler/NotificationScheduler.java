package com.grid07.socialmedia.Scheduler;

import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.grid07.socialmedia.service.RedisService;

@Component
public class NotificationScheduler {

    private final RedisService redisService;
    private final RedisTemplate<String, String> redisTemplate;

    public NotificationScheduler(RedisService redisService,
                                 RedisTemplate<String, String> redisTemplate) {
        this.redisService = redisService;
        this.redisTemplate = redisTemplate;
    }

    // runs every 5 minutes
    @Scheduled(fixedRate = 300000)
    public void sweepPendingNotifications() {

        System.out.println("CRON: Sweeping pending notifications...");

        // find all keys matching "user:*:pending_notifs"
        Set<String> keys = redisTemplate.keys("user:*:pending_notifs");

        if (keys == null || keys.isEmpty()) {
            System.out.println("CRON: No pending notifications found.");
            return;
        }

        // loop through each user with pending notifications
        for (String key : keys) {

            // extract user id from key
            // key format: "user:5:pending_notifs"
            String userId = key.split(":")[1];

            // pop all notifications for this user
            List<String> notifications = redisService.popAllFromList(key);

            if (notifications == null || notifications.isEmpty()) continue;

            // get first bot name from first notification
            String firstNotif = notifications.get(0);
            int othersCount   = notifications.size() - 1;

            // log summarized notification
            if (othersCount > 0) {
                System.out.println("Summarized Push Notification for User "
                        + userId + ": "
                        + firstNotif + " and ["
                        + othersCount + "] others interacted with your posts.");
            } else {
                System.out.println("Summarized Push Notification for User "
                        + userId + ": " + firstNotif);
            }

            // clear the list
            redisService.delete(key);
        }
    }
}