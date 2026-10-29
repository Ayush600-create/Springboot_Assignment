package com.campusguard.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "claims", indexes = {
        @Index(name = "idx_claims_status", columnList = "status"),
        @Index(name = "idx_claims_claimant_item", columnList = "claimant_id")
})
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private ItemMatch match;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AppUser claimant;

    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal verificationScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.VerificationDecision verificationDecision;

    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal thresholdUsed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.ClaimStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.ReviewStatus reviewStatus = Enums.ReviewStatus.NONE;

    private String adminReason;
    private int reopenCount;
    private int verificationAttempts = 1;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public ItemMatch getMatch() { return match; }
    public void setMatch(ItemMatch match) { this.match = match; }
    public AppUser getClaimant() { return claimant; }
    public void setClaimant(AppUser claimant) { this.claimant = claimant; }
    public BigDecimal getVerificationScore() { return verificationScore; }
    public void setVerificationScore(BigDecimal verificationScore) { this.verificationScore = verificationScore; }
    public Enums.VerificationDecision getVerificationDecision() { return verificationDecision; }
    public void setVerificationDecision(Enums.VerificationDecision verificationDecision) { this.verificationDecision = verificationDecision; }
    public BigDecimal getThresholdUsed() { return thresholdUsed; }
    public void setThresholdUsed(BigDecimal thresholdUsed) { this.thresholdUsed = thresholdUsed; }
    public Enums.ClaimStatus getStatus() { return status; }
    public void setStatus(Enums.ClaimStatus status) { this.status = status; }
    public Enums.ReviewStatus getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(Enums.ReviewStatus reviewStatus) { this.reviewStatus = reviewStatus; }
    public String getAdminReason() { return adminReason; }
    public void setAdminReason(String adminReason) { this.adminReason = adminReason; }
    public int getReopenCount() { return reopenCount; }
    public void setReopenCount(int reopenCount) { this.reopenCount = reopenCount; }
    public int getVerificationAttempts() { return verificationAttempts; }
    public void setVerificationAttempts(int verificationAttempts) { this.verificationAttempts = verificationAttempts; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
