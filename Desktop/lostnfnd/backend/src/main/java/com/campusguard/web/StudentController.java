package com.campusguard.web;

import com.campusguard.dto.Dtos;
import com.campusguard.service.CurrentUserService;
import com.campusguard.service.DashboardService;
import com.campusguard.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StudentController {
    private final DashboardService dashboard;
    private final CurrentUserService current;
    private final NotificationService notifications;

    public StudentController(DashboardService dashboard, CurrentUserService current, NotificationService notifications) {
        this.dashboard = dashboard;
        this.current = current;
        this.notifications = notifications;
    }

    @GetMapping("/student/dashboard")
    public Dtos.StudentDashboard dashboard() {
        return dashboard.student(current.require());
    }

    @GetMapping("/notifications")
    public List<Dtos.NotificationView> notifications() {
        return notifications.list(current.require());
    }

    @PostMapping("/notifications/{id}/read")
    public Map<String, String> read(@PathVariable Long id) {
        notifications.markRead(current.require(), id);
        return Map.of("status", "ok");
    }

    @GetMapping("/notifications/unread-count")
    public Map<String, Long> unread() {
        return Map.of("count", notifications.unread(current.require().getId()));
    }
}
