package com.campusguard.service;

import com.campusguard.domain.AppUser;
import com.campusguard.repo.UserRepository;
import com.campusguard.web.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final UserRepository users;

    public CurrentUserService(UserRepository users) {
        this.users = users;
    }

    public AppUser require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw ApiException.unauthorized("Authentication required");
        }
        return users.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> ApiException.unauthorized("Unknown user"));
    }
}
