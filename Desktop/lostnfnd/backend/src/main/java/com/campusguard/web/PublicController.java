package com.campusguard.web;

import com.campusguard.dto.Dtos;
import com.campusguard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicController {
    private final DashboardService dashboard;

    public PublicController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/stats")
    public Dtos.PublicStats stats() {
        return dashboard.publicStats();
    }
}
