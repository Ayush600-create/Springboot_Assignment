package com.campusguard;

import com.campusguard.config.CampusGuardProperties;
import com.campusguard.domain.Enums;
import com.campusguard.service.MatchingEngine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MatchingEngineTest {

    private final CampusGuardProperties.Matching matching = new CampusGuardProperties.Matching();

    @Test
    void headphonesExampleProduces94_2HighConfidence() {
        var factors = new MatchingEngine.FactorScores(
                new BigDecimal("85.5"),
                MatchingEngine.HUNDRED,
                MatchingEngine.HUNDRED,
                MatchingEngine.HUNDRED,
                MatchingEngine.HUNDRED,
                MatchingEngine.HUNDRED
        );
        var result = MatchingEngine.combine(factors, matching.getWeights(), matching.getConfidence());
        assertEquals(new BigDecimal("94.2"), result.total());
        assertEquals(Enums.Confidence.HIGH_CONFIDENCE, result.confidence());
    }

    @Test
    void defaultWeightsSumToOne() {
        var w = matching.getWeights();
        BigDecimal sum = w.getText().add(w.getLocation()).add(w.getDate())
                .add(w.getCategory()).add(w.getBrand()).add(w.getColor())
                .setScale(2, RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("1.00"), sum);
    }

    @Test
    void missingOptionalBrandScoresZeroAndDoesNotRenormalize() {
        var lost = new MatchingEngine.ReportSignals(1L, Enums.ReportType.LOST, "sony headphones",
                "black over ear", "central library", LocalDate.of(2026, 10, 20),
                Enums.ItemCategory.ELECTRONICS, null, "black");
        var found = new MatchingEngine.ReportSignals(2L, Enums.ReportType.FOUND, "sony headphones",
                "black over ear", "central library", LocalDate.of(2026, 10, 20),
                Enums.ItemCategory.ELECTRONICS, "Sony", "black");
        var score = MatchingEngine.score(lost, found, matching);
        assertEquals(0, score.factors().brand().compareTo(MatchingEngine.ZERO));
        BigDecimal withBrand = MatchingEngine.combine(new MatchingEngine.FactorScores(
                score.factors().text(), score.factors().location(), score.factors().date(),
                score.factors().category(), MatchingEngine.HUNDRED, score.factors().color()
        ), matching.getWeights(), matching.getConfidence()).total();
        assertTrue(score.total().compareTo(withBrand) < 0);
    }

    @Test
    void weightSumFailureIsDetected() {
        var props = new CampusGuardProperties();
        props.getMatching().getWeights().setText(new BigDecimal("0.50"));
        assertThrows(IllegalStateException.class, props::validateMatchingWeights);
    }

    @Test
    void jaccardOfAlphaBetaAgainstFourTokensIsFifty() {
        assertEquals(new BigDecimal("50.0"), MatchingEngine.jaccard(
                MatchingEngine.tokenize("alpha beta gamma delta"),
                MatchingEngine.tokenize("alpha beta")));
    }

    @Test
    void rejectsSameTypeAndSelf() {
        var a = new MatchingEngine.ReportSignals(1L, Enums.ReportType.LOST, "bag", "red", "gate",
                LocalDate.now(), Enums.ItemCategory.BAGS, "Nike", "red");
        assertThrows(IllegalArgumentException.class, () -> MatchingEngine.score(a, a, matching));
        var b = new MatchingEngine.ReportSignals(2L, Enums.ReportType.LOST, "bag", "red", "gate",
                LocalDate.now(), Enums.ItemCategory.BAGS, "Nike", "red");
        assertThrows(IllegalArgumentException.class, () -> MatchingEngine.score(a, b, matching));
    }
}
