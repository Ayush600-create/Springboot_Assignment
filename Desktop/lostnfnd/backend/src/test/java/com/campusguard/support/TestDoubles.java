package com.campusguard.support;

import com.campusguard.service.OtpGenerator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

@TestConfiguration
public class TestDoubles {

    @Bean
    @Primary
    public CapturingMailSender capturingMailSender() {
        return new CapturingMailSender();
    }

    @Bean
    @Primary
    public ConfigurableOtpGenerator configurableOtpGenerator() {
        return new ConfigurableOtpGenerator();
    }

    public static class ConfigurableOtpGenerator implements OtpGenerator {
        private final AtomicReference<String> next = new AtomicReference<>("123456");
        public void set(String otp) { next.set(otp); }
        @Override public String generate() { return next.get(); }
    }

    public static class CapturingMailSender implements JavaMailSender {
        public final List<SimpleMailMessage> messages = new CopyOnWriteArrayList<>();
        @Override public MimeMessage createMimeMessage() { throw new UnsupportedOperationException(); }
        @Override public MimeMessage createMimeMessage(java.io.InputStream contentStream) { throw new UnsupportedOperationException(); }
        @Override public void send(MimeMessage mimeMessage) { }
        @Override public void send(MimeMessage... mimeMessages) { }
        @Override public void send(MimeMessagePreparator mimeMessagePreparator) { }
        @Override public void send(MimeMessagePreparator... mimeMessagePreparators) { }
        @Override public void send(SimpleMailMessage simpleMessage) { messages.add(simpleMessage); }
        @Override public void send(SimpleMailMessage... simpleMessages) { messages.addAll(List.of(simpleMessages)); }
    }
}
