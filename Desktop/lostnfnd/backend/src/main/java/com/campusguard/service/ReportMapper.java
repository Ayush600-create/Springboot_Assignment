package com.campusguard.service;

import com.campusguard.domain.ItemMatch;
import com.campusguard.domain.LostFoundReport;
import com.campusguard.dto.Dtos;

public final class ReportMapper {
    private ReportMapper() {}

    public static Dtos.ReportCard card(LostFoundReport r) {
        String photo = r.getPhotoFilename() == null ? null : "/api/files/" + r.getPhotoFilename();
        return new Dtos.ReportCard(
                r.getId(),
                r.getReportType(),
                r.getItemName(),
                r.getCategory(),
                r.getPublicDescription(),
                r.getLocation(),
                r.getOccurredAt(),
                r.getBrand(),
                r.getColor(),
                photo,
                r.getStatus(),
                r.getPrivateDetails() != null || r.hasPrivateDetails(),
                r.getCreatedAt()
        );
    }

    public static Dtos.MatchView match(ItemMatch m) {
        var factors = new MatchingEngine.FactorScores(
                m.getTextScore(), m.getLocationScore(), m.getDateScore(),
                m.getCategoryScore(), m.getBrandScore(), m.getColorScore());
        return new Dtos.MatchView(
                m.getId(),
                m.getScore(),
                m.getConfidence(),
                m.getTextScore(),
                m.getLocationScore(),
                m.getDateScore(),
                m.getCategoryScore(),
                m.getBrandScore(),
                m.getColorScore(),
                MatchingEngine.explain(factors, m.getScore(), m.getConfidence()),
                card(m.getLostReport()),
                card(m.getFoundReport())
        );
    }

    public static MatchingEngine.ReportSignals signals(LostFoundReport r) {
        return new MatchingEngine.ReportSignals(
                r.getId(),
                r.getReportType(),
                r.getItemName(),
                r.getPublicDescription(),
                r.getLocation(),
                r.getOccurredAt() == null ? null : r.getOccurredAt().toLocalDate(),
                r.getCategory(),
                r.getBrand(),
                r.getColor()
        );
    }
}
