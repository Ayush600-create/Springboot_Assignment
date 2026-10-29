package com.campusguard.service;

import com.campusguard.config.CampusGuardProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private final JavaMailSender sender;
    private final CampusGuardProperties properties;

    public MailService(JavaMailSender sender, CampusGuardProperties properties) {
        this.sender = sender;
        this.properties = properties;
    }

    public boolean canSend() {
        return properties.getMail().isEnabled();
    }

    public boolean send(String to, String subject, String body) {
        if (!canSend()) {
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.getMail().getFrom());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
            return true;
        } catch (Exception ex) {
            log.warn("Email delivery failed; in-app notification still applies");
            return false;
        }
    }
}
