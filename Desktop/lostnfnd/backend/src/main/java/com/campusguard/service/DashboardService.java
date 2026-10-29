package com.campusguard.service;

import com.campusguard.domain.AppUser;
import com.campusguard.domain.Enums;
import com.campusguard.dto.Dtos;
import com.campusguard.repo.ClaimRepository;
import com.campusguard.repo.HandoverRepository;
import com.campusguard.repo.MatchRepository;
import com.campusguard.repo.ReportRepository;
import com.campusguard.repo.UserRepository;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class DashboardService {
    private final ReportRepository reports;
    private final MatchRepository matches;
    private final ClaimRepository claims;
    private final UserRepository users;
    private final HandoverRepository handovers;
    private final ReportService reportService;
    private final ClaimService claimService;
    private final NotificationService notifications;
    private final AuditService audit;
    private final AuthService auth;
    private final Environment environment;

    public DashboardService(ReportRepository reports, MatchRepository matches, ClaimRepository claims,
                            UserRepository users, HandoverRepository handovers, ReportService reportService,
                            ClaimService claimService, NotificationService notifications, AuditService audit,
                            AuthService auth, Environment environment) {
        this.reports = reports;
        this.matches = matches;
        this.claims = claims;
        this.users = users;
        this.handovers = handovers;
        this.reportService = reportService;
        this.claimService = claimService;
        this.notifications = notifications;
        this.audit = audit;
        this.auth = auth;
        this.environment = environment;
    }

    public boolean demo() {
        return Arrays.asList(environment.getActiveProfiles()).contains("demo");
    }

    public Dtos.PublicStats publicStats() {
        return new Dtos.PublicStats(
                reports.countByReportTypeAndStatusNot(Enums.ReportType.LOST, Enums.ReportStatus.RETURNED),
                reports.countByReportType(Enums.ReportType.FOUND),
                matches.count(),
                demo()
        );
    }

    public Dtos.StudentDashboard student(AppUser user) {
        var mine = reportService.mine(user);
        long lost = mine.stream().filter(r -> r.reportType() == Enums.ReportType.LOST).count();
        long found = mine.stream().filter(r -> r.reportType() == Enums.ReportType.FOUND).count();
        long matchCount = mine.stream().mapToLong(r -> reportService.matchesFor(r.id()).size()).sum();
        var claimViews = claimService.mine(user);
        long active = claimViews.stream()
                .filter(c -> c.status() != Enums.ClaimStatus.COMPLETED && c.status() != Enums.ClaimStatus.REJECTED)
                .count();
        return new Dtos.StudentDashboard(
                auth.view(user),
                lost,
                found,
                matchCount,
                active,
                mine.stream().limit(6).toList(),
                notifications.list(user).stream().limit(8).toList(),
                claimViews.stream().limit(8).toList(),
                demo()
        );
    }

    public Dtos.AdminDashboard admin() {
        return new Dtos.AdminDashboard(
                users.countByRole(Enums.Role.STUDENT),
                reports.countByReportType(Enums.ReportType.LOST),
                reports.countByReportType(Enums.ReportType.FOUND),
                reports.countByStatusNot(Enums.ReportStatus.RETURNED),
                matches.count(),
                claims.countByStatus(Enums.ClaimStatus.VERIFIED) + claims.countByStatus(Enums.ClaimStatus.UNDER_REVIEW)
                        + claims.countByStatus(Enums.ClaimStatus.REJECTED),
                claims.countByStatus(Enums.ClaimStatus.APPROVED_FOR_HANDOVER),
                handovers.countByCompletedAtIsNotNull(),
                audit.recent(),
                demo()
        );
    }
}
