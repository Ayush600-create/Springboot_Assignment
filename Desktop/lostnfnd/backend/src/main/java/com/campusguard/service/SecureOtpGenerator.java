package com.campusguard.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class SecureOtpGenerator implements OtpGenerator {
    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
