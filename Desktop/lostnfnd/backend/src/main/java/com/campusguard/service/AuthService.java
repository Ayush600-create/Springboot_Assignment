package com.campusguard.service;

import com.campusguard.config.CampusGuardProperties;
import com.campusguard.domain.AppUser;
import com.campusguard.domain.Enums;
import com.campusguard.dto.Dtos;
import com.campusguard.repo.UserRepository;
import com.campusguard.security.JwtService;
import com.campusguard.web.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final CampusGuardProperties properties;
    private final RateLimitService rateLimit;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt,
                       CampusGuardProperties properties, RateLimitService rateLimit) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.properties = properties;
        this.rateLimit = rateLimit;
    }

    @Transactional
    public Dtos.AuthResponse register(Dtos.RegisterRequest req) {
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw ApiException.conflict("An account with that email already exists");
        }
        AppUser user = new AppUser();
        user.setName(req.name().trim());
        user.setEmail(req.email().trim().toLowerCase());
        user.setPasswordHash(encoder.encode(req.password()));
        user.setStudentId(req.studentId().trim());
        user.setDepartment(req.department().trim());
        user.setRole(Enums.Role.STUDENT);
        boolean college = isCollegeEmail(user.getEmail());
        user.setCollegeEmailFormat(college);
        user.setAccountStatus(Enums.AccountStatus.PENDING_APPROVAL);
        if (college && properties.isAutoApproveCollegeEmail()) {
            user.setAccountStatus(Enums.AccountStatus.ACTIVE);
        }
        users.save(user);
        if (user.getAccountStatus() != Enums.AccountStatus.ACTIVE) {
            return new Dtos.AuthResponse(null, view(user));
        }
        return toAuth(user);
    }

    public Dtos.AuthResponse login(Dtos.LoginRequest req) {
        rateLimit.checkLogin(req.email());
        AppUser user = users.findByEmailIgnoreCase(req.email())
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        if (user.getAccountStatus() == Enums.AccountStatus.PENDING_APPROVAL) {
            throw ApiException.forbidden("Your student account is waiting for admin approval. Typing an ID does not verify you.");
        }
        if (user.getAccountStatus() != Enums.AccountStatus.ACTIVE) {
            throw ApiException.forbidden("This account is not active");
        }
        return toAuth(user);
    }

    public Dtos.UserView view(AppUser user) {
        return new Dtos.UserView(user.getId(), user.getName(), user.getEmail(), user.getStudentId(),
                user.getDepartment(), user.getRole(), user.getAccountStatus(), user.isCollegeEmailFormat());
    }

    private Dtos.AuthResponse toAuth(AppUser user) {
        return new Dtos.AuthResponse(jwt.issue(user.getEmail(), user.getRole().name()), view(user));
    }

    private boolean isCollegeEmail(String email) {
        String lower = email.toLowerCase();
        return properties.getCollegeEmailSuffixes().stream()
                .anyMatch(s -> lower.endsWith("@" + s.toLowerCase()));
    }
}
