package com.campusguard.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class StartupValidator implements ApplicationRunner {

    private final CampusGuardProperties properties;
    private final Environment environment;

    public StartupValidator(CampusGuardProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        properties.validateMatchingWeights();
        boolean test = Arrays.asList(environment.getActiveProfiles()).contains("test");
        if (!test) {
            String key = properties.getCrypto().getKey();
            if (key == null || key.isBlank()) {
                throw new IllegalStateException("campusguard.crypto.key / CAMPUSGUARD_ENCRYPTION_KEY is required");
            }
        }
    }
}
