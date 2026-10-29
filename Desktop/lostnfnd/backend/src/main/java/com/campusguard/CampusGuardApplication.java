package com.campusguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * CampusGuard — Smart Lost and Found
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class CampusGuardApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusGuardApplication.class, args);
    }
}
