package com.campusguard.seed;

import com.campusguard.config.CampusGuardProperties;
import com.campusguard.domain.AppUser;
import com.campusguard.domain.Enums;
import com.campusguard.repo.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(10)
public class AdminBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final CampusGuardProperties properties;

    public AdminBootstrap(UserRepository users, PasswordEncoder encoder, CampusGuardProperties properties) {
        this.users = users;
        this.encoder = encoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = properties.getAdmin().getEmail();
        String password = properties.getAdmin().getPassword();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }
        if (users.findByEmailIgnoreCase(email).isPresent()) {
            return;
        }
        AppUser admin = new AppUser();
        admin.setName("CampusGuard Admin");
        admin.setEmail(email.trim().toLowerCase());
        admin.setPasswordHash(encoder.encode(password));
        admin.setStudentId("ADMIN");
        admin.setDepartment("Administration");
        admin.setRole(Enums.Role.ADMIN);
        admin.setAccountStatus(Enums.AccountStatus.ACTIVE);
        admin.setCollegeEmailFormat(true);
        users.save(admin);
    }
}
