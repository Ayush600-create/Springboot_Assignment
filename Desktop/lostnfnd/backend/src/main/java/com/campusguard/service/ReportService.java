package com.campusguard.service;

import com.campusguard.domain.AppUser;
import com.campusguard.domain.Enums;
import com.campusguard.domain.ItemMatch;
import com.campusguard.domain.ItemPrivateDetails;
import com.campusguard.domain.LostFoundReport;
import com.campusguard.dto.Dtos;
import com.campusguard.repo.MatchRepository;
import com.campusguard.repo.PrivateDetailsRepository;
import com.campusguard.repo.ReportRepository;
import com.campusguard.web.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportService {
    private final ReportRepository reports;
    private final PrivateDetailsRepository privateDetails;
    private final MatchRepository matches;
    private final EncryptionService encryption;
    private final FileStorageService files;
    private final MatchingService matching;

    public ReportService(ReportRepository reports, PrivateDetailsRepository privateDetails,
                         MatchRepository matches, EncryptionService encryption,
                         FileStorageService files, MatchingService matching) {
        this.reports = reports;
        this.privateDetails = privateDetails;
        this.matches = matches;
        this.encryption = encryption;
        this.files = files;
        this.matching = matching;
    }

    @Transactional
    public Dtos.ReportCard create(AppUser user, Enums.ReportType type, String itemName, Enums.ItemCategory category,
                                  String publicDescription, String location, LocalDateTime occurredAt,
                                  String brand, String color, String serialNumber, String privateMarks,
                                  MultipartFile photo) {
        if (user.getAccountStatus() != Enums.AccountStatus.ACTIVE) {
            throw ApiException.forbidden("Account is not approved");
        }
        LostFoundReport report = new LostFoundReport();
        report.setReporter(user);
        report.setReportType(type);
        report.setItemName(itemName.trim());
        report.setCategory(category);
        report.setPublicDescription(publicDescription.trim());
        report.setLocation(location.trim());
        report.setOccurredAt(occurredAt);
        report.setBrand(blankToNull(brand));
        report.setColor(blankToNull(color));
        report.setPhotoFilename(files.store(photo));
        reports.save(report);

        StringBuilder privatePayload = new StringBuilder();
        if (!isBlank(serialNumber)) {
            privatePayload.append("SERIAL:").append(serialNumber.trim());
        }
        if (!isBlank(privateMarks)) {
            if (!privatePayload.isEmpty()) privatePayload.append('\n');
            privatePayload.append(privateMarks.trim());
        }
        if (!privatePayload.isEmpty()) {
            var sealed = encryption.encrypt(privatePayload.toString());
            ItemPrivateDetails details = new ItemPrivateDetails();
            details.setReport(report);
            details.setCiphertext(sealed.ciphertext());
            details.setNonce(sealed.nonce());
            privateDetails.save(details);
            report.setPrivateDetails(details);
        }
        matching.recalculateFor(report);
        return toCard(report);
    }

    @Transactional(readOnly = true)
    public Dtos.ReportCard get(Long id) {
        return toCard(load(id));
    }

    public LostFoundReport load(Long id) {
        return reports.findById(id).orElseThrow(() -> ApiException.notFound("Report not found"));
    }

    public Page<Dtos.ReportCard> search(String q, Enums.ReportType type, Enums.ItemCategory category,
                                        Enums.ReportStatus status, String location, String brand, String color,
                                        LocalDate fromDate, LocalDate toDate,
                                        String sort, int page, int size) {
        Sort s = switch (sort == null ? "newest" : sort) {
            case "oldest" -> Sort.by("createdAt").ascending();
            case "relevance" -> Sort.by(Sort.Order.desc("lastMatchScore").nullsLast(), Sort.Order.desc("createdAt"));
            default -> Sort.by("createdAt").descending();
        };
        LocalDateTime from = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime to = toDate == null ? null : toDate.atTime(23, 59, 59);
        return reports.search(emptyToNull(q), type, category, status, emptyToNull(location),
                        emptyToNull(brand), emptyToNull(color), from, to,
                        PageRequest.of(page, Math.min(size, 50), s))
                .map(this::toCard);
    }

    public List<Dtos.ReportCard> mine(AppUser user) {
        return reports.findByReporterIdOrderByCreatedAtDesc(user.getId()).stream().map(this::toCard).toList();
    }

    public List<Dtos.MatchView> matchesFor(Long reportId) {
        return matches.findByLostReportIdOrFoundReportIdOrderByScoreDesc(reportId, reportId)
                .stream().map(ReportMapper::match).toList();
    }

    public Dtos.MatchView match(Long id) {
        return ReportMapper.match(matches.findById(id).orElseThrow(() -> ApiException.notFound("Match not found")));
    }

    public ItemMatch loadMatch(Long id) {
        return matches.findById(id).orElseThrow(() -> ApiException.notFound("Match not found"));
    }

    @Transactional
    public Dtos.ReportCard updateStatus(Long id, Enums.ReportStatus status) {
        LostFoundReport report = load(id);
        report.setStatus(status);
        return toCard(report);
    }

    public Dtos.ReportCard toCard(LostFoundReport r) {
        Dtos.ReportCard card = ReportMapper.card(r);
        boolean privateOnFile = r.getId() != null && privateDetails.existsByReportId(r.getId());
        return new Dtos.ReportCard(card.id(), card.reportType(), card.itemName(), card.category(),
                card.publicDescription(), card.location(), card.occurredAt(), card.brand(), card.color(),
                card.photoUrl(), card.status(), privateOnFile, card.createdAt());
    }

    private static String blankToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }

    private static String emptyToNull(String s) {
        return isBlank(s) ? null : s;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
