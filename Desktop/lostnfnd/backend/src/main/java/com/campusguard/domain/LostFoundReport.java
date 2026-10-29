package com.campusguard.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports", indexes = {
        @Index(name = "idx_reports_type_status", columnList = "reportType,status"),
        @Index(name = "idx_reports_category", columnList = "category"),
        @Index(name = "idx_reports_location", columnList = "location"),
        @Index(name = "idx_reports_occurred", columnList = "occurredAt"),
        @Index(name = "idx_reports_name", columnList = "itemName")
})
public class LostFoundReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AppUser reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.ReportType reportType;

    @Column(nullable = false)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.ItemCategory category;

    @Column(nullable = false, length = 2000)
    private String publicDescription;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    private String brand;
    private String color;
    private String photoFilename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.ReportStatus status = Enums.ReportStatus.OPEN;

    @Column(precision = 4, scale = 1)
    private java.math.BigDecimal lastMatchScore;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToOne(mappedBy = "report", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ItemPrivateDetails privateDetails;

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public AppUser getReporter() { return reporter; }
    public void setReporter(AppUser reporter) { this.reporter = reporter; }
    public Enums.ReportType getReportType() { return reportType; }
    public void setReportType(Enums.ReportType reportType) { this.reportType = reportType; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public Enums.ItemCategory getCategory() { return category; }
    public void setCategory(Enums.ItemCategory category) { this.category = category; }
    public String getPublicDescription() { return publicDescription; }
    public void setPublicDescription(String publicDescription) { this.publicDescription = publicDescription; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getPhotoFilename() { return photoFilename; }
    public void setPhotoFilename(String photoFilename) { this.photoFilename = photoFilename; }
    public Enums.ReportStatus getStatus() { return status; }
    public void setStatus(Enums.ReportStatus status) { this.status = status; }
    public java.math.BigDecimal getLastMatchScore() { return lastMatchScore; }
    public void setLastMatchScore(java.math.BigDecimal lastMatchScore) { this.lastMatchScore = lastMatchScore; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public ItemPrivateDetails getPrivateDetails() { return privateDetails; }
    public void setPrivateDetails(ItemPrivateDetails privateDetails) { this.privateDetails = privateDetails; }
    public boolean hasPrivateDetails() { return privateDetails != null; }
}
