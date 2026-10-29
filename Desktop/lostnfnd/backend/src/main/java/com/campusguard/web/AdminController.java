package com.campusguard.web;

import com.campusguard.domain.AppUser;
import com.campusguard.domain.Enums;
import com.campusguard.dto.Dtos;
import com.campusguard.repo.UserRepository;
import com.campusguard.service.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final DashboardService dashboard;
    private final ClaimService claims;
    private final HandoverService handovers;
    private final ReportService reports;
    private final UserRepository users;
    private final AuditService audit;
    private final CurrentUserService current;
    private final AuthService auth;

    public AdminController(DashboardService dashboard, ClaimService claims, HandoverService handovers,
                           ReportService reports, UserRepository users, AuditService audit,
                           CurrentUserService current, AuthService auth) {
        this.dashboard = dashboard;
        this.claims = claims;
        this.handovers = handovers;
        this.reports = reports;
        this.users = users;
        this.audit = audit;
        this.current = current;
        this.auth = auth;
    }

    @GetMapping("/dashboard")
    public Dtos.AdminDashboard dash() {
        return dashboard.admin();
    }

    @GetMapping("/claims")
    public List<Dtos.ClaimView> claimQueue() {
        return claims.queue();
    }

    @GetMapping("/claims/{id}")
    public Dtos.ClaimView claim(@PathVariable Long id) {
        return claims.view(claims.load(id), true);
    }

    @PostMapping("/claims/{id}/open-review")
    public Dtos.ClaimView open(@PathVariable Long id) {
        return claims.openReview(current.require(), id);
    }

    @PostMapping("/claims/{id}/reopen")
    public Dtos.ClaimView reopen(@PathVariable Long id, @Valid @RequestBody Dtos.ReviewRequest req) {
        return claims.reopen(current.require(), id, req.reason());
    }

    @PostMapping("/claims/{id}/approve")
    public Dtos.ClaimView approve(@PathVariable Long id, @Valid @RequestBody Dtos.ReviewRequest req) {
        return claims.approve(current.require(), id, req.reason());
    }

    @PostMapping("/claims/{id}/reject")
    public Dtos.ClaimView reject(@PathVariable Long id, @Valid @RequestBody Dtos.ReviewRequest req) {
        return claims.reject(current.require(), id, req.reason());
    }

    @PostMapping("/handovers/{claimId}/issue-otp")
    public Map<String, String> issue(@PathVariable Long claimId) {
        Enums.OtpStatus status = handovers.issueOtp(claimId, current.require());
        if (status == Enums.OtpStatus.DELIVERY_UNAVAILABLE) {
            return Map.of("otpStatus", status.name(), "message", "OTP email is not configured");
        }
        return Map.of("otpStatus", status.name(), "message", "OTP emailed to the claimant");
    }

    @PostMapping("/handovers/{claimId}/verify-otp")
    public Map<String, String> verify(@PathVariable Long claimId, @Valid @RequestBody Dtos.OtpVerifyRequest req) {
        handovers.verifyOtp(claimId, req.otp(), current.require());
        return Map.of("status", "COMPLETED");
    }

    @GetMapping("/users")
    public List<Dtos.UserView> userList() {
        return users.findAll().stream().map(auth::view).toList();
    }

    @PostMapping("/users/{id}/decision")
    public Dtos.UserView decide(@PathVariable Long id, @Valid @RequestBody Dtos.AccountDecisionRequest req) {
        AppUser actor = current.require();
        AppUser user = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        user.setAccountStatus(req.status());
        users.save(user);
        audit.record(actor, "USER_STATUS", "User", id, req.reason());
        return auth.view(user);
    }

    @PatchMapping("/reports/{id}/status")
    public Dtos.ReportCard reportStatus(@PathVariable Long id, @Valid @RequestBody Dtos.StatusUpdateRequest req) {
        Dtos.ReportCard card = reports.updateStatus(id, req.status());
        audit.record(current.require(), "REPORT_STATUS", "Report", id, req.status().name());
        return card;
    }

    @GetMapping("/audit")
    public List<Dtos.AuditView> auditLog() {
        return audit.recent();
    }
}
