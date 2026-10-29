package com.campusguard.service;

import com.campusguard.config.CampusGuardProperties;
import com.campusguard.domain.Enums;
import com.campusguard.domain.ItemMatch;
import com.campusguard.domain.LostFoundReport;
import com.campusguard.repo.MatchRepository;
import com.campusguard.repo.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MatchingService {
    private static final List<Enums.ReportStatus> ACTIVE = List.of(
            Enums.ReportStatus.OPEN, Enums.ReportStatus.MATCHED, Enums.ReportStatus.CLAIM_IN_PROGRESS);

    private final ReportRepository reports;
    private final MatchRepository matches;
    private final CampusGuardProperties properties;
    private final NotificationService notifications;

    public MatchingService(ReportRepository reports, MatchRepository matches,
                           CampusGuardProperties properties, NotificationService notifications) {
        this.reports = reports;
        this.matches = matches;
        this.properties = properties;
        this.notifications = notifications;
    }

    @Transactional
    public void recalculateFor(LostFoundReport report) {
        Enums.ReportType opposite = report.getReportType() == Enums.ReportType.LOST
                ? Enums.ReportType.FOUND : Enums.ReportType.LOST;
        List<LostFoundReport> candidates = reports.findByReportTypeAndStatusIn(opposite, ACTIVE);
        for (LostFoundReport other : candidates) {
            if (other.getId().equals(report.getId())) {
                continue;
            }
            LostFoundReport lost = report.getReportType() == Enums.ReportType.LOST ? report : other;
            LostFoundReport found = report.getReportType() == Enums.ReportType.FOUND ? report : other;
            var score = MatchingEngine.score(ReportMapper.signals(lost), ReportMapper.signals(found),
                    properties.getMatching());
            ItemMatch match = matches.findByLostReportIdAndFoundReportId(lost.getId(), found.getId())
                    .orElseGet(ItemMatch::new);
            boolean created = match.getId() == null;
            match.setLostReport(lost);
            match.setFoundReport(found);
            match.setScore(score.total());
            match.setTextScore(score.factors().text());
            match.setLocationScore(score.factors().location());
            match.setDateScore(score.factors().date());
            match.setCategoryScore(score.factors().category());
            match.setBrandScore(score.factors().brand());
            match.setColorScore(score.factors().color());
            match.setConfidence(score.confidence());
            matches.save(match);
            bumpMatchScore(lost, score.total());
            bumpMatchScore(found, score.total());
            if (lost.getStatus() == Enums.ReportStatus.OPEN) {
                lost.setStatus(Enums.ReportStatus.MATCHED);
            }
            if (found.getStatus() == Enums.ReportStatus.OPEN) {
                found.setStatus(Enums.ReportStatus.MATCHED);
            }
            if (created && score.total().compareTo(properties.getMatching().getConfidence().getPossible()) >= 0) {
                notifications.notify(lost.getReporter(), Enums.NotificationType.POTENTIAL_MATCH,
                        "Potential match found",
                        "CampusGuard found a possible match for \"" + lost.getItemName() + "\".",
                        match.getId());
                notifications.notify(found.getReporter(), Enums.NotificationType.POTENTIAL_MATCH,
                        "Potential match found",
                        "A lost report may match an item you found: \"" + found.getItemName() + "\".",
                        match.getId());
            }
        }
    }

    private void bumpMatchScore(LostFoundReport report, BigDecimal score) {
        if (report.getLastMatchScore() == null || score.compareTo(report.getLastMatchScore()) > 0) {
            report.setLastMatchScore(score);
        }
    }
}
