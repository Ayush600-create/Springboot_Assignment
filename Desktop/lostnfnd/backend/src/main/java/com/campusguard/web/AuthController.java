package com.campusguard.web;

import com.campusguard.dto.Dtos;
import com.campusguard.service.AuthService;
import com.campusguard.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final CurrentUserService current;

    public AuthController(AuthService auth, CurrentUserService current) {
        this.auth = auth;
        this.current = current;
    }

    @PostMapping("/register")
    public Dtos.AuthResponse register(@Valid @RequestBody Dtos.RegisterRequest req) {
        return auth.register(req);
    }

    @PostMapping("/login")
    public Dtos.AuthResponse login(@Valid @RequestBody Dtos.LoginRequest req) {
        return auth.login(req);
    }

    @GetMapping("/me")
    public Dtos.UserView me() {
        return auth.view(current.require());
    }
}
