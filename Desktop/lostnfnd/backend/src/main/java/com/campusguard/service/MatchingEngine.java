package com.campusguard.service;

import com.campusguard.config.CampusGuardProperties;
import com.campusguard.domain.Enums;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Weighted six-factor matcher for CampusGuard — Smart Lost and Found.
 * <p>
 * Missing-data policy: if an optional factor is missing on either report, that
 * factor scores 0. Weights are never re-normalized. Missing data is never treated
 * as a perfect match.
 */
public final class MatchingEngine {

    public static final BigDecimal HUNDRED = new BigDecimal("100.0");
    public static final BigDecimal ZERO = new BigDecimal("0.0");
    private static final Pattern TOKEN = Pattern.compile("[^a-z0-9]+");

    private MatchingEngine() {}

    public record FactorScores(
            BigDecimal text,
            BigDecimal location,
            BigDecimal date,
            BigDecimal category,
            BigDecimal brand,
            BigDecimal color
    ) {}

    public record MatchScore(BigDecimal total, Enums.Confidence confidence, FactorScores factors) {}

    public record ReportSignals(
            Long id,
            Enums.ReportType type,
            String itemName,
            String publicDescription,
            String location,
            LocalDate occurredDate,
            Enums.ItemCategory category,
            String brand,
            String color
    ) {}

    public static MatchScore combine(FactorScores factors, CampusGuardProperties.Matching.Weights weights,
                                     CampusGuardProperties.Matching.Confidence thresholds) {
        BigDecimal total = factors.text().multiply(weights.getText())
                .add(factors.location().multiply(weights.getLocation()))
                .add(factors.date().multiply(weights.getDate()))
                .add(factors.category().multiply(weights.getCategory()))
                .add(factors.brand().multiply(weights.getBrand()))
                .add(factors.color().multiply(weights.getColor()));
        total = total.setScale(1, RoundingMode.HALF_UP);
        if (total.compareTo(HUNDRED) > 0) {
            total = HUNDRED;
        }
        if (total.compareTo(ZERO) < 0) {
            total = ZERO;
        }
        Enums.Confidence confidence;
        if (total.compareTo(thresholds.getHigh()) >= 0) {
            confidence = Enums.Confidence.HIGH_CONFIDENCE;
        } else if (total.compareTo(thresholds.getPossible()) >= 0) {
            confidence = Enums.Confidence.POSSIBLE_MATCH;
        } else {
            confidence = Enums.Confidence.LOW_CONFIDENCE;
        }
        return new MatchScore(total, confidence, factors);
    }

    public static MatchScore score(ReportSignals a, ReportSignals b, CampusGuardProperties.Matching matching) {
        if (a.type() == b.type()) {
            throw new IllegalArgumentException("Cannot match two reports of the same type");
        }
        if (a.id() != null && a.id().equals(b.id())) {
            throw new IllegalArgumentException("Cannot match a report with itself");
        }
        FactorScores factors = new FactorScores(
                textSimilarity(joinText(a), joinText(b)),
                locationSimilarity(a.location(), b.location()),
                dateSimilarity(a.occurredDate(), b.occurredDate(), matching.getDateMaxDays()),
                categorySimilarity(a.category(), b.category()),
                brandSimilarity(a.brand(), b.brand()),
                colorSimilarity(a.color(), b.color())
        );
        return combine(factors, matching.getWeights(), matching.getConfidence());
    }

    public static BigDecimal textSimilarity(String left, String right) {
        return jaccard(tokenize(left), tokenize(right));
    }

    public static BigDecimal locationSimilarity(String left, String right) {
        if (isBlank(left) || isBlank(right)) {
            return ZERO;
        }
        String n1 = normalizeLocation(left);
        String n2 = normalizeLocation(right);
        if (n1.equals(n2)) {
            return HUNDRED;
        }
        return jaccard(tokenize(n1), tokenize(n2));
    }

    public static BigDecimal dateSimilarity(LocalDate left, LocalDate right, int maxDays) {
        if (left == null || right == null) {
            return ZERO;
        }
        long days = Math.abs(ChronoUnit.DAYS.between(left, right));
        if (days == 0) {
            return HUNDRED;
        }
        if (days >= maxDays) {
            return ZERO;
        }
        BigDecimal remaining = BigDecimal.valueOf(maxDays - days)
                .divide(BigDecimal.valueOf(maxDays), 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(1, RoundingMode.HALF_UP);
        return remaining;
    }

    public static BigDecimal categorySimilarity(Enums.ItemCategory left, Enums.ItemCategory right) {
        if (left == null || right == null) {
            return ZERO;
        }
        return left == right ? HUNDRED : ZERO;
    }

    public static BigDecimal brandSimilarity(String left, String right) {
        return equalsNormalized(left, right);
    }

    public static BigDecimal colorSimilarity(String left, String right) {
        return equalsNormalized(left, right);
    }

    public static String explain(FactorScores factors, BigDecimal total, Enums.Confidence confidence) {
        return "Overall " + total.toPlainString() + "% (" + confidence + "). "
                + "Text " + factors.text().toPlainString() + "%, "
                + "location " + factors.location().toPlainString() + "%, "
                + "date " + factors.date().toPlainString() + "%, "
                + "category " + factors.category().toPlainString() + "%, "
                + "brand " + factors.brand().toPlainString() + "%, "
                + "color " + factors.color().toPlainString()
                + "%. A high match score suggests correspondence but does not prove ownership.";
    }

    public static BigDecimal jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return ZERO;
        }
        Set<String> inter = new LinkedHashSet<>(a);
        inter.retainAll(b);
        Set<String> union = new LinkedHashSet<>(a);
        union.addAll(b);
        return BigDecimal.valueOf(inter.size())
                .divide(BigDecimal.valueOf(union.size()), 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(1, RoundingMode.HALF_UP);
    }

    public static Set<String> tokenize(String raw) {
        if (isBlank(raw)) {
            return Set.of();
        }
        return Arrays.stream(TOKEN.split(raw.toLowerCase(Locale.ROOT)))
                .filter(s -> s.length() > 1)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static String normalizeLocation(String raw) {
        return TOKEN.matcher(raw.toLowerCase(Locale.ROOT).trim()).replaceAll(" ").trim();
    }

    private static String joinText(ReportSignals s) {
        return Objects.toString(s.itemName(), "") + " " + Objects.toString(s.publicDescription(), "");
    }

    private static BigDecimal equalsNormalized(String left, String right) {
        if (isBlank(left) || isBlank(right)) {
            return ZERO;
        }
        return left.trim().equalsIgnoreCase(right.trim()) ? HUNDRED : ZERO;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
