package com.campusguard.domain;

public final class Enums {
    private Enums() {}

    public enum Role { STUDENT, ADMIN }

    public enum AccountStatus { PENDING_APPROVAL, ACTIVE, REJECTED, SUSPENDED }

    public enum ReportType { LOST, FOUND }

    public enum ItemCategory {
        ELECTRONICS,
        IDENTITY_CARDS,
        BOOKS_AND_STATIONERY,
        BAGS,
        CLOTHING,
        KEYS,
        ACCESSORIES,
        OTHER
    }

    public enum ReportStatus { OPEN, MATCHED, CLAIM_IN_PROGRESS, RETURNED, CLOSED }

    public enum Confidence { HIGH_CONFIDENCE, POSSIBLE_MATCH, LOW_CONFIDENCE }

    public enum ClaimStatus {
        PENDING,
        VERIFIED,
        REJECTED,
        UNDER_REVIEW,
        APPROVED_FOR_HANDOVER,
        COMPLETED
    }

    public enum VerificationDecision { BELOW_THRESHOLD, MEETS_THRESHOLD }

    public enum ReviewStatus { NONE, FLAGGED_FOR_ADMIN_REVIEW, IN_REVIEW, CLOSED }

    public enum OtpStatus {
        NOT_ISSUED,
        ISSUED,
        DELIVERY_UNAVAILABLE,
        VERIFIED,
        EXPIRED,
        LOCKED
    }

    public enum NotificationType {
        POTENTIAL_MATCH,
        CLAIM_SUBMITTED,
        CLAIM_STATUS_CHANGED,
        CLAIM_APPROVED,
        CLAIM_REJECTED,
        HANDOVER_SCHEDULED,
        OTP_ISSUED,
        OTP_UNAVAILABLE,
        ITEM_RETURNED
    }
}
