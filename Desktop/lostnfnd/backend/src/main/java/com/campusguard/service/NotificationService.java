package com.campusguard.service;

import com.campusguard.domain.AppNotification;
import com.campusguard.domain.AppUser;
import com.campusguard.domain.Enums;
import com.campusguard.dto.Dtos;
import com.campusguard.repo.NotificationRepository;
import com.campusguard.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final MailService mail;

    public NotificationService(NotificationRepository notifications, MailService mail) {
        this.notifications = notifications;
        this.mail = mail;
    }

    @Transactional
    public void notify(AppUser user, Enums.NotificationType type, String title, String body, Long ref) {
        AppNotification n = new AppNotification();
        n.setUser(user);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setReferenceId(ref);
        notifications.save(n);
        mail.send(user.getEmail(), "CampusGuard — Smart Lost and Found: " + title, body);
    }

    public List<Dtos.NotificationView> list(AppUser user) {
        return notifications.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(n -> new Dtos.NotificationView(n.getId(), n.getType(), n.getTitle(), n.getBody(),
                        n.isReadFlag(), n.getCreatedAt(), n.getReferenceId()))
                .toList();
    }

    @Transactional
    public void markRead(AppUser user, Long id) {
        AppNotification n = notifications.findById(id).orElseThrow(() -> ApiException.notFound("Notification not found"));
        if (!n.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("Not your notification");
        }
        n.setReadFlag(true);
    }

    public long unread(Long userId) {
        return notifications.countByUserIdAndReadFlagFalse(userId);
    }
}
