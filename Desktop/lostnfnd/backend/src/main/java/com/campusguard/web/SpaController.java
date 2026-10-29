package com.campusguard.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Forwards all non-API, non-static routes to index.html so React Router
 * handles client-side navigation (e.g. /login, /app/dashboard).
 */
@Controller
public class SpaController {

    @RequestMapping(value = {
            "/",
            "/login",
            "/register",
            "/app",
            "/app/**"
    })
    public String spa(HttpServletRequest request) {
        return "forward:/index.html";
    }
}
