package com.campusguard.service;

import com.campusguard.config.CampusGuardProperties;
import com.campusguard.domain.AppUser;
import com.campusguard.domain.Claim;
import com.campusguard.domain.Enums;
import com.campusguard.domain.Handover;
import com.campusguard.repo.ClaimRepository;
import com.campusguard.repo.HandoverRepository;
import com.campusguard.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class HandoverService {
    private final HandoverRepository handovers;
    private final ClaimRepository claims;
    private final MailService mail;
    private final NotificationService notifications;
    private final AuditService audit;
    private final PasswordEncoder encoder;
    private final CampusGuardProperties properties;
    private final OtpGenerator otpGenerator;

    public HandoverService(HandoverRepository handovers, ClaimRepository claims, MailService mail,
                           NotificationService notifications, AuditService audit,
                           PasswordEncoder encoder, CampusGuardProperties properties,
                           OtpGenerator otpGenerator) {
        this.handovers = handovers;
        this.claims = claims;
        this.mail = mail;
        this.notifications = notifications;
        this.audit = audit;
        this.encoder = encoder;
        this.properties = properties;
        this.otpGenerator = otpGenerator;
    }

    @Transactional
    public Handover createOnApproval(Claim claim) {
        Handover existing = handovers.findByClaimId(claim.getId()).orElse(null);
        if (existing != null) {
            return existing;
        }
        Handover handover = new Handover();
        handover.setClaim(claim);
        handover.setOtpStatus(Enums.OtpStatus.NOT_ISSUED);
        handovers.save(handover);
        issueOtp(claim.getId(), null);
        return handover;
    }

    @Transactional
    public Enums.OtpStatus issueOtp(Long claimId, AppUser admin) {
        Handover handover = handovers.findByClaimId(claimId)
                .orElseThrow(() -> ApiException.notFound("Handover not found"));
        Claim claim = handover.getClaim();
        if (claim.getStatus() != Enums.ClaimStatus.APPROVED_FOR_HANDOVER) {
            throw new ApiException(HttpStatus.CONFLICT, "OTP can only be issued for approved handovers");
        }
        if (!mail.canSend()) {
            markDeliveryUnavailable(handover, claim, admin);
            return handover.getOtpStatus();
        }
        String otp = otpGenerator.generate();
        boolean sent = mail.send(claim.getClaimant().getEmail(),
                "CampusGuard — Smart Lost and Found: handover OTP",
                "Your CampusGuard handover OTP is " + otp + ". It expires in "
                        + properties.getOtp().getExpiryMinutes()
                        + " minutes. Tell it only to the administrator at pickup.");
        if (!sent) {
            markDeliveryUnavailable(handover, claim, admin);
            return handover.getOtpStatus();
        }
        handover.setOtpHash(encoder.encode(otp));
        handover.setOtpExpiresAt(Instant.now().plus(properties.getOtp().getExpiryMinutes(), ChronoUnit.MINUTES));
        handover.setOtpAttempts(0);
        handover.setOtpUsed(false);
        handover.setOtpStatus(Enums.OtpStatus.ISSUED);
        notifications.notify(claim.getClaimant(), Enums.NotificationType.OTP_ISSUED,
                "Handover OTP issued",
                "A one-time code was emailed to your college address. Tell it only to the administrator at pickup.",
                claim.getId());
        notifications.notify(claim.getClaimant(), Enums.NotificationType.HANDOVER_SCHEDULED,
                "Handover scheduled",
                "Bring identification and your OTP to complete the return.",
                claim.getId());
        if (admin != null) {
            audit.record(admin, "OTP_ISSUED", "Handover", handover.getId(), "OTP hashed and emailed");
        }
        return handover.getOtpStatus();
    }

    private void markDeliveryUnavailable(Handover handover, Claim claim, AppUser admin) {
        handover.setOtpStatus(Enums.OtpStatus.DELIVERY_UNAVAILABLE);
        handover.setOtpHash(null);
        notifications.notify(claim.getClaimant(), Enums.NotificationType.OTP_UNAVAILABLE,
                "OTP email is not configured",
                "CampusGuard cannot send a handover OTP until SMTP is configured. Completion is blocked.",
                claim.getId());
        if (admin != null) {
            audit.record(admin, "OTP_UNAVAILABLE", "Handover", handover.getId(), "SMTP not configured");
        }
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void verifyOtp(Long claimId, String otp, AppUser admin) {
        Handover handover = handovers.findByClaimId(claimId)
                .orElseThrow(() -> ApiException.notFound("Handover not found"));
        Claim claim = handover.getClaim();
        if (claim.getStatus() != Enums.ClaimStatus.APPROVED_FOR_HANDOVER) {
            throw new ApiException(HttpStatus.CONFLICT, "Invalid claim status for OTP verification");
        }
        if (handover.getOtpStatus() == Enums.OtpStatus.DELIVERY_UNAVAILABLE
                || handover.getOtpStatus() == Enums.OtpStatus.NOT_ISSUED) {
            throw ApiException.bad("OTP email is not configured");
        }
        if (handover.isOtpUsed() || handover.getOtpStatus() == Enums.OtpStatus.VERIFIED) {
            throw new ApiException(HttpStatus.CONFLICT, "OTP already used");
        }
        if (handover.getOtpStatus() == Enums.OtpStatus.LOCKED) {
            throw ApiException.tooMany("OTP attempt limit exceeded");
        }
        if (handover.getOtpExpiresAt() != null && Instant.now().isAfter(handover.getOtpExpiresAt())) {
            handover.setOtpStatus(Enums.OtpStatus.EXPIRED);
            throw ApiException.bad("OTP has expired");
        }
        if (handover.getOtpAttempts() >= properties.getOtp().getMaxAttempts()) {
            handover.setOtpStatus(Enums.OtpStatus.LOCKED);
            throw ApiException.tooMany("OTP attempt limit exceeded");
        }
        handover.setOtpAttempts(handover.getOtpAttempts() + 1);
        if (handover.getOtpHash() == null || otp == null || !encoder.matches(otp, handover.getOtpHash())) {
            if (handover.getOtpAttempts() >= properties.getOtp().getMaxAttempts()) {
                handover.setOtpStatus(Enums.OtpStatus.LOCKED);
                throw ApiException.tooMany("OTP attempt limit exceeded");
            }
            throw ApiException.bad("Incorrect OTP");
        }
        handover.setOtpUsed(true);
        handover.setOtpStatus(Enums.OtpStatus.VERIFIED);
        handover.setCompletedAt(Instant.now());
        claim.setStatus(Enums.ClaimStatus.COMPLETED);
        claims.save(claim);
        claim.getMatch().getLostReport().setStatus(Enums.ReportStatus.RETURNED);
        claim.getMatch().getFoundReport().setStatus(Enums.ReportStatus.RETURNED);
        audit.record(admin, "HANDOVER_COMPLETED", "Handover", handover.getId(), "OTP verified; item marked RETURNED");
        notifications.notify(claim.getClaimant(), Enums.NotificationType.ITEM_RETURNED,
                "Item returned",
                "CampusGuard recorded a confirmed handover for your claim.",
                claim.getId());
    }

    public Handover require(Long claimId) {
        return handovers.findByClaimId(claimId).orElseThrow(() -> ApiException.notFound("Handover not found"));
    }
}
