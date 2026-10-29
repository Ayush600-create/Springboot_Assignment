package com.campusguard.dto;

import com.campusguard.domain.Enums;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    public record RegisterRequest(
            @NotBlank String name,
            @Email @NotBlank String email,
            @Size(min = 8) String password,
            @NotBlank String studentId,
            @NotBlank String department
    ) {}

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

    public record AuthResponse(String token, UserView user) {}

    public record UserView(Long id, String name, String email, String studentId, String department,
                           Enums.Role role, Enums.AccountStatus accountStatus, boolean collegeEmailFormat) {}

    public record PublicStats(long activeLostReports, long foundReports, long potentialMatches, boolean demoData) {}

    public record StudentDashboard(
            UserView profile,
            long lostReports,
            long foundReports,
            long potentialMatches,
            long activeClaims,
            List<ReportCard> recentItems,
            List<NotificationView> recentNotifications,
            List<ClaimView> claims,
            boolean demoData
    ) {}

    public record AdminDashboard(
            long totalStudents,
            long lostReports,
            long foundReports,
            long unresolvedReports,
            long potentialMatches,
            long pendingClaims,
            long approvedClaims,
            long completedHandovers,
            List<AuditView> recentActivity,
            boolean demoData
    ) {}

    public record ReportCard(
            Long id,
            Enums.ReportType reportType,
            String itemName,
            Enums.ItemCategory category,
            String publicDescription,
            String location,
            LocalDateTime occurredAt,
            String brand,
            String color,
            String photoUrl,
            Enums.ReportStatus status,
            boolean privateDetailsOnFile,
            Instant createdAt
    ) {}

    public record MatchView(
            Long id,
            BigDecimal score,
            Enums.Confidence confidence,
            BigDecimal textScore,
            BigDecimal locationScore,
            BigDecimal dateScore,
            BigDecimal categoryScore,
            BigDecimal brandScore,
            BigDecimal colorScore,
            String explanation,
            ReportCard lostItem,
            ReportCard foundItem
    ) {}

    public record ClaimRequest(@NotNull Long matchId, @NotBlank @Size(min = 4, max = 2000) String ownershipStatement) {}

    public record ClaimView(
            Long id,
            Long matchId,
            Enums.ClaimStatus status,
            Enums.ReviewStatus reviewStatus,
            Enums.VerificationDecision verificationDecision,
            BigDecimal verificationScore,
            BigDecimal thresholdUsed,
            String adminReason,
            Instant createdAt,
            Instant updatedAt,
            Enums.OtpStatus otpStatus,
            String genericOutcome
    ) {}

    public record ReviewRequest(@NotBlank String reason) {}

    public record OtpVerifyRequest(@NotBlank @Size(min = 6, max = 6) String otp) {}

    public record NotificationView(Long id, Enums.NotificationType type, String title, String body,
                                   boolean read, Instant createdAt, Long referenceId) {}

    public record AuditView(Long id, String actorEmail, String action, String entityType, Long entityId,
                            String details, Instant createdAt) {}

    public record StatusUpdateRequest(@NotNull Enums.ReportStatus status) {}

    public record AccountDecisionRequest(@NotNull Enums.AccountStatus status, String reason) {}
}
