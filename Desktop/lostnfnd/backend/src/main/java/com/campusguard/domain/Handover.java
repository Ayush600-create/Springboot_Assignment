package com.campusguard.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "handovers")
public class Handover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "claim_id", unique = true)
    private Claim claim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.OtpStatus otpStatus = Enums.OtpStatus.NOT_ISSUED;

    private String otpHash;
    private Instant otpExpiresAt;
    private int otpAttempts;
    private boolean otpUsed;

    private Instant completedAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Claim getClaim() { return claim; }
    public void setClaim(Claim claim) { this.claim = claim; }
    public Enums.OtpStatus getOtpStatus() { return otpStatus; }
    public void setOtpStatus(Enums.OtpStatus otpStatus) { this.otpStatus = otpStatus; }
    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }
    public Instant getOtpExpiresAt() { return otpExpiresAt; }
    public void setOtpExpiresAt(Instant otpExpiresAt) { this.otpExpiresAt = otpExpiresAt; }
    public int getOtpAttempts() { return otpAttempts; }
    public void setOtpAttempts(int otpAttempts) { this.otpAttempts = otpAttempts; }
    public boolean isOtpUsed() { return otpUsed; }
    public void setOtpUsed(boolean otpUsed) { this.otpUsed = otpUsed; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
