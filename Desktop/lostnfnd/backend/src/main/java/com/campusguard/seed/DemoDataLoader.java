package com.campusguard.seed;

import com.campusguard.domain.*;
import com.campusguard.repo.*;
import com.campusguard.service.EncryptionService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CampusGuard — Smart Lost and Found demo seed (profile: demo).
 */
@Component
@Profile("demo")
@Order(100)
public class DemoDataLoader implements ApplicationRunner {

    public static final String DEMO_STUDENT_PASSWORD = "CampusGuard!demo";

    private final UserRepository users;
    private final ReportRepository reports;
    private final PrivateDetailsRepository privateDetails;
    private final MatchRepository matches;
    private final ClaimRepository claims;
    private final PasswordEncoder encoder;
    private final EncryptionService encryption;

    public DemoDataLoader(UserRepository users, ReportRepository reports, PrivateDetailsRepository privateDetails,
                          MatchRepository matches, ClaimRepository claims, PasswordEncoder encoder,
                          EncryptionService encryption) {
        this.users = users;
        this.reports = reports;
        this.privateDetails = privateDetails;
        this.matches = matches;
        this.claims = claims;
        this.encoder = encoder;
        this.encryption = encryption;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.findByEmailIgnoreCase("aisha@srmist.edu.in").isPresent()) {
            matches.findAll().forEach(match -> {
                if (match.getLostReport().getLastMatchScore() == null) {
                    match.getLostReport().setLastMatchScore(match.getScore());
                }
                if (match.getFoundReport().getLastMatchScore() == null) {
                    match.getFoundReport().setLastMatchScore(match.getScore());
                }
            });
            return;
        }
        AppUser aisha = student("Aisha Rahman", "aisha@srmist.edu.in", "RA2511056030111", "CSE DS");
        AppUser rahul = student("Rahul Mehta", "rahul@srmist.edu.in", "RA2511056030112", "CSE DS");

        LocalDateTime when = LocalDateTime.of(2026, 10, 20, 16, 30);
        LostFoundReport lost = report(aisha, Enums.ReportType.LOST,
                "Sony WH-1000XM4 headphones",
                "Black over-ear Sony WH-1000XM4 noise-cancelling headphones lost near the central library study desks.",
                "Central Library, 2nd floor", when);
        LostFoundReport found = report(rahul, Enums.ReportType.FOUND,
                "Sony WH-1000XM4 headphones",
                "Black Sony WH-1000XM4 over-ear headphones found on a table in the central library.",
                "Central Library, 2nd floor", when);

        var sealed = encryption.encrypt("SERIAL:SN-WH1000XM4-4411\nscratch on left ear cup near the hinge");
        ItemPrivateDetails details = new ItemPrivateDetails();
        details.setReport(lost);
        details.setCiphertext(sealed.ciphertext());
        details.setNonce(sealed.nonce());
        privateDetails.save(details);

        ItemMatch match = new ItemMatch();
        match.setLostReport(lost);
        match.setFoundReport(found);
        match.setTextScore(new BigDecimal("85.5"));
        match.setLocationScore(new BigDecimal("100.0"));
        match.setDateScore(new BigDecimal("100.0"));
        match.setCategoryScore(new BigDecimal("100.0"));
        match.setBrandScore(new BigDecimal("100.0"));
        match.setColorScore(new BigDecimal("100.0"));
        match.setScore(new BigDecimal("94.2"));
        match.setConfidence(Enums.Confidence.HIGH_CONFIDENCE);
        matches.save(match);
        lost.setLastMatchScore(new BigDecimal("94.2"));
        found.setLastMatchScore(new BigDecimal("94.2"));
        lost.setStatus(Enums.ReportStatus.CLAIM_IN_PROGRESS);
        found.setStatus(Enums.ReportStatus.CLAIM_IN_PROGRESS);

        Claim claim = new Claim();
        claim.setMatch(match);
        claim.setClaimant(aisha);
        claim.setVerificationScore(new BigDecimal("50.0"));
        claim.setThresholdUsed(new BigDecimal("60.0"));
        claim.setVerificationDecision(Enums.VerificationDecision.BELOW_THRESHOLD);
        claim.setStatus(Enums.ClaimStatus.REJECTED);
        claim.setReviewStatus(Enums.ReviewStatus.FLAGGED_FOR_ADMIN_REVIEW);
        claims.save(claim);
    }

    private AppUser student(String name, String email, String id, String dept) {
        AppUser u = new AppUser();
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(DEMO_STUDENT_PASSWORD));
        u.setStudentId(id);
        u.setDepartment(dept);
        u.setRole(Enums.Role.STUDENT);
        u.setAccountStatus(Enums.AccountStatus.ACTIVE);
        u.setCollegeEmailFormat(true);
        return users.save(u);
    }

    private LostFoundReport report(AppUser user, Enums.ReportType type, String name, String desc, String loc, LocalDateTime when) {
        LostFoundReport r = new LostFoundReport();
        r.setReporter(user);
        r.setReportType(type);
        r.setItemName(name);
        r.setCategory(Enums.ItemCategory.ELECTRONICS);
        r.setPublicDescription(desc);
        r.setLocation(loc);
        r.setOccurredAt(when);
        r.setBrand("Sony");
        r.setColor("Black");
        r.setStatus(Enums.ReportStatus.MATCHED);
        return reports.save(r);
    }
}
