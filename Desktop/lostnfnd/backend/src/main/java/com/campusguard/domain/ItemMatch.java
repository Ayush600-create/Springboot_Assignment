package com.campusguard.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "item_matches", uniqueConstraints = {
        @UniqueConstraint(name = "uk_match_pair", columnNames = {"lost_report_id", "found_report_id"})
})
public class ItemMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "lost_report_id")
    private LostFoundReport lostReport;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "found_report_id")
    private LostFoundReport foundReport;

    @Column(nullable = false, precision = 4, scale = 1)
    private BigDecimal score;

    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal textScore;
    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal locationScore;
    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal dateScore;
    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal categoryScore;
    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal brandScore;
    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal colorScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.Confidence confidence;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public LostFoundReport getLostReport() { return lostReport; }
    public void setLostReport(LostFoundReport lostReport) { this.lostReport = lostReport; }
    public LostFoundReport getFoundReport() { return foundReport; }
    public void setFoundReport(LostFoundReport foundReport) { this.foundReport = foundReport; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public BigDecimal getTextScore() { return textScore; }
    public void setTextScore(BigDecimal textScore) { this.textScore = textScore; }
    public BigDecimal getLocationScore() { return locationScore; }
    public void setLocationScore(BigDecimal locationScore) { this.locationScore = locationScore; }
    public BigDecimal getDateScore() { return dateScore; }
    public void setDateScore(BigDecimal dateScore) { this.dateScore = dateScore; }
    public BigDecimal getCategoryScore() { return categoryScore; }
    public void setCategoryScore(BigDecimal categoryScore) { this.categoryScore = categoryScore; }
    public BigDecimal getBrandScore() { return brandScore; }
    public void setBrandScore(BigDecimal brandScore) { this.brandScore = brandScore; }
    public BigDecimal getColorScore() { return colorScore; }
    public void setColorScore(BigDecimal colorScore) { this.colorScore = colorScore; }
    public Enums.Confidence getConfidence() { return confidence; }
    public void setConfidence(Enums.Confidence confidence) { this.confidence = confidence; }
    public Instant getCreatedAt() { return createdAt; }
}
