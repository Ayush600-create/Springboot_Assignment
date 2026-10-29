package com.campusguard.service;

import com.campusguard.config.CampusGuardProperties;
import com.campusguard.domain.AppUser;
import com.campusguard.domain.Claim;
import com.campusguard.domain.Enums;
import com.campusguard.domain.ItemMatch;
import com.campusguard.domain.ItemPrivateDetails;
import com.campusguard.dto.Dtos;
import com.campusguard.repo.ClaimRepository;
import com.campusguard.repo.HandoverRepository;
import com.campusguard.repo.PrivateDetailsRepository;
import com.campusguard.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ClaimService {
    private final ClaimRepository claims;
    private final PrivateDetailsRepository privateDetails;
    private final HandoverRepository handovers;
    private final EncryptionService encryption;
    private final NotificationService notifications;
    private final AuditService audit;
    private final CampusGuardProperties properties;
    private final RateLimitService rateLimit;
    private final HandoverService handoverService;

    public ClaimService(ClaimRepository claims, PrivateDetailsRepository privateDetails,
                        HandoverRepository handovers, EncryptionService encryption,
                        NotificationService notifications, AuditService audit,
                        CampusGuardProperties properties, RateLimitService rateLimit,
                        HandoverService handoverService) {
        this.claims = claims;
        this.privateDetails = privateDetails;
        this.handovers = handovers;
        this.encryption = encryption;
        this.notifications = notifications;
        this.audit = audit;
        this.properties = properties;
        this.rateLimit = rateLimit;
        this.handoverService = handoverService;
    }

    @Transactional
    public Dtos.ClaimView submit(AppUser claimant, ItemMatch match, String ownershipStatement) {
        rateLimit.checkVerify(claimant.getId() + ":" + match.getId());
        if (claims.findByMatchIdAndClaimantId(match.getId(), claimant.getId()).isPresent()) {
            throw ApiException.conflict("You already submitted a claim for this match");
        }
        long attempts = claims.countAttemptsForItem(claimant.getId(), match.getLostReport().getId())
                + claims.countAttemptsForItem(claimant.getId(), match.getFoundReport().getId());
        int max = properties.getClaim().getVerification().getMaxAttempts();
        if (attempts >= max) {
            throw ApiException.tooMany("Verification attempt limit reached for this item");
        }

        BigDecimal threshold = properties.getClaim().getVerification().getThreshold();
        BigDecimal score = scoreOwnership(match, ownershipStatement);
        Claim claim = new Claim();
        claim.setMatch(match);
        claim.setClaimant(claimant);
        claim.setVerificationScore(score);
        claim.setThresholdUsed(threshold);
        claim.setVerificationAttempts(1);
        if (score.compareTo(threshold) >= 0) {
            claim.setVerificationDecision(Enums.VerificationDecision.MEETS_THRESHOLD);
            claim.setStatus(Enums.ClaimStatus.VERIFIED);
            claim.setReviewStatus(Enums.ReviewStatus.NONE);
        } else {
            claim.setVerificationDecision(Enums.VerificationDecision.BELOW_THRESHOLD);
            claim.setStatus(Enums.ClaimStatus.REJECTED);
            claim.setReviewStatus(Enums.ReviewStatus.FLAGGED_FOR_ADMIN_REVIEW);
        }
        claims.save(claim);
        match.getLostReport().setStatus(Enums.ReportStatus.CLAIM_IN_PROGRESS);
        match.getFoundReport().setStatus(Enums.ReportStatus.CLAIM_IN_PROGRESS);
        notifications.notify(claimant, Enums.NotificationType.CLAIM_SUBMITTED,
                "Claim submitted",
                genericOutcome(claim),
                claim.getId());
        notifications.notify(match.getLostReport().getReporter(), Enums.NotificationType.CLAIM_SUBMITTED,
                "A claim was submitted",
                "A student submitted a claim on a match involving your report.",
                claim.getId());
        return view(claim, false);
    }

    public BigDecimal scoreOwnership(ItemMatch match, String statement) {
        String secret = decryptPrivate(match);
        if (secret == null || secret.isBlank() || statement == null || statement.isBlank()) {
            return MatchingEngine.ZERO;
        }
        var answerTokens = MatchingEngine.tokenize(statement);
        BigDecimal best = MatchingEngine.jaccard(MatchingEngine.tokenize(secret), answerTokens);
        for (String line : secret.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            BigDecimal lineScore = MatchingEngine.jaccard(MatchingEngine.tokenize(line), answerTokens);
            if (lineScore.compareTo(best) > 0) {
                best = lineScore;
            }
        }
        return best.setScale(1, RoundingMode.HALF_UP);
    }

    private String decryptPrivate(ItemMatch match) {
        ItemPrivateDetails details = privateDetails.findByReportId(match.getLostReport().getId())
                .or(() -> privateDetails.findByReportId(match.getFoundReport().getId()))
                .orElse(null);
        if (details == null) {
            return "";
        }
        return encryption.decrypt(details.getCiphertext(), details.getNonce());
    }

    @Transactional
    public Dtos.ClaimView openReview(AppUser admin, Long claimId) {
        Claim claim = load(claimId);
        transition(claim, Enums.ClaimStatus.VERIFIED, Enums.ClaimStatus.UNDER_REVIEW);
        claim.setReviewStatus(Enums.ReviewStatus.IN_REVIEW);
        audit.record(admin, "OPEN_REVIEW", "Claim", claim.getId(), "Admin opened claim review");
        return view(claim, true);
    }

    @Transactional
    public Dtos.ClaimView reopen(AppUser admin, Long claimId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw ApiException.bad("A recorded reason is required to reopen a claim");
        }
        Claim claim = load(claimId);
        if (claim.getReopenCount() >= 1) {
            throw ApiException.conflict("A rejected claim may be reopened only once");
        }
        transition(claim, Enums.ClaimStatus.REJECTED, Enums.ClaimStatus.UNDER_REVIEW);
        claim.setReopenCount(claim.getReopenCount() + 1);
        claim.setAdminReason(reason.trim());
        claim.setReviewStatus(Enums.ReviewStatus.IN_REVIEW);
        audit.record(admin, "REOPEN_CLAIM", "Claim", claim.getId(), reason.trim());
        notifyStatus(claim, "Claim reopened for review");
        return view(claim, true);
    }

    @Transactional
    public Dtos.ClaimView approve(AppUser admin, Long claimId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw ApiException.bad("A recorded reason is required");
        }
        Claim claim = load(claimId);
        transition(claim, Enums.ClaimStatus.UNDER_REVIEW, Enums.ClaimStatus.APPROVED_FOR_HANDOVER);
        claim.setAdminReason(reason.trim());
        claim.setReviewStatus(Enums.ReviewStatus.CLOSED);
        claims.save(claim);
        handoverService.createOnApproval(claim);
        audit.record(admin, "APPROVE_CLAIM", "Claim", claim.getId(), reason.trim());
        notifications.notify(claim.getClaimant(), Enums.NotificationType.CLAIM_APPROVED,
                "Claim approved for handover",
                "An administrator approved your claim. Complete OTP handover at pickup.",
                claim.getId());
        return view(claim, true);
    }

    @Transactional
    public Dtos.ClaimView reject(AppUser admin, Long claimId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw ApiException.bad("A recorded reason is required");
        }
        Claim claim = load(claimId);
        Set<Enums.ClaimStatus> from = EnumSet.of(Enums.ClaimStatus.UNDER_REVIEW, Enums.ClaimStatus.REJECTED);
        if (claim.getStatus() == Enums.ClaimStatus.REJECTED
                && claim.getReviewStatus() == Enums.ReviewStatus.FLAGGED_FOR_ADMIN_REVIEW) {
            claim.setAdminReason(reason.trim());
            claim.setReviewStatus(Enums.ReviewStatus.CLOSED);
            audit.record(admin, "CONFIRM_REJECTION", "Claim", claim.getId(), reason.trim());
            notifyStatus(claim, "Claim rejection confirmed");
            return view(claim, true);
        }
        if (!from.contains(claim.getStatus()) || claim.getStatus() != Enums.ClaimStatus.UNDER_REVIEW) {
            throw new ApiException(HttpStatus.CONFLICT, "Invalid claim status transition");
        }
        transition(claim, Enums.ClaimStatus.UNDER_REVIEW, Enums.ClaimStatus.REJECTED);
        claim.setAdminReason(reason.trim());
        claim.setReviewStatus(Enums.ReviewStatus.CLOSED);
        audit.record(admin, "REJECT_CLAIM", "Claim", claim.getId(), reason.trim());
        notifications.notify(claim.getClaimant(), Enums.NotificationType.CLAIM_REJECTED,
                "Claim rejected",
                "An administrator rejected this claim.",
                claim.getId());
        return view(claim, true);
    }

    public List<Dtos.ClaimView> mine(AppUser user) {
        return claims.findByClaimantIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(c -> view(c, false)).toList();
    }

    public List<Dtos.ClaimView> queue() {
        return claims.findByStatusInOrderByCreatedAtDesc(List.of(
                Enums.ClaimStatus.VERIFIED,
                Enums.ClaimStatus.UNDER_REVIEW,
                Enums.ClaimStatus.REJECTED,
                Enums.ClaimStatus.APPROVED_FOR_HANDOVER,
                Enums.ClaimStatus.COMPLETED
        )).stream().map(c -> view(c, true)).toList();
    }

    public Claim load(Long id) {
        return claims.findById(id).orElseThrow(() -> ApiException.notFound("Claim not found"));
    }

    public Dtos.ClaimView view(Claim claim, boolean includeScores) {
        Enums.OtpStatus otp = handovers.findByClaimId(claim.getId()).map(h -> h.getOtpStatus()).orElse(null);
        return new Dtos.ClaimView(
                claim.getId(),
                claim.getMatch().getId(),
                claim.getStatus(),
                claim.getReviewStatus(),
                claim.getVerificationDecision(),
                includeScores ? claim.getVerificationScore() : null,
                includeScores ? claim.getThresholdUsed() : null,
                includeScores ? claim.getAdminReason() : null,
                claim.getCreatedAt(),
                claim.getUpdatedAt(),
                otp,
                genericOutcome(claim)
        );
    }

    public Dtos.ClaimView getForUser(AppUser user, Long id) {
        Claim claim = load(id);
        boolean owner = claim.getClaimant().getId().equals(user.getId());
        boolean party = claim.getMatch().getLostReport().getReporter().getId().equals(user.getId())
                || claim.getMatch().getFoundReport().getReporter().getId().equals(user.getId());
        boolean admin = user.getRole() == Enums.Role.ADMIN;
        if (!owner && !party && !admin) {
            throw ApiException.forbidden("Not allowed to view this claim");
        }
        return view(claim, admin);
    }

    private void transition(Claim claim, Enums.ClaimStatus from, Enums.ClaimStatus to) {
        if (claim.getStatus() != from) {
            throw new ApiException(HttpStatus.CONFLICT, "Invalid claim status transition from "
                    + claim.getStatus() + " to " + to);
        }
        claim.setStatus(to);
    }

    private void notifyStatus(Claim claim, String title) {
        notifications.notify(claim.getClaimant(), Enums.NotificationType.CLAIM_STATUS_CHANGED,
                title, genericOutcome(claim), claim.getId());
    }

    private String genericOutcome(Claim claim) {
        if (claim.getVerificationDecision() == Enums.VerificationDecision.MEETS_THRESHOLD) {
            return "Ownership check met the configured threshold. An administrator must still review the claim.";
        }
        return "Ownership check did not meet the configured threshold. The claim is flagged for administrator review.";
    }
}
