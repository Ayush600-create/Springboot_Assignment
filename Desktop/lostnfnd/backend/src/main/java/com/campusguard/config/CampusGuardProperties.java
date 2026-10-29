package com.campusguard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "campusguard")
public class CampusGuardProperties {

    private Jwt jwt = new Jwt();
    private Crypto crypto = new Crypto();
    private String uploadDir = "uploads";
    private List<String> collegeEmailSuffixes = new ArrayList<>();
    private boolean autoApproveCollegeEmail;
    private Matching matching = new Matching();
    private Claim claim = new Claim();
    private Otp otp = new Otp();
    private Mail mail = new Mail();
    private Admin admin = new Admin();
    private RateLimit rateLimit = new RateLimit();

    public void validateMatchingWeights() {
        Matching.Weights w = matching.getWeights();
        BigDecimal sum = w.getText().add(w.getLocation()).add(w.getDate())
                .add(w.getCategory()).add(w.getBrand()).add(w.getColor());
        if (sum.setScale(2, RoundingMode.HALF_UP).compareTo(new BigDecimal("1.00")) != 0) {
            throw new IllegalStateException(
                    "campusguard.matching.weights.* must sum to 1.00 but summed to " + sum.toPlainString());
        }
    }

    public Jwt getJwt() { return jwt; }
    public Crypto getCrypto() { return crypto; }
    public String getUploadDir() { return uploadDir; }
    public void setUploadDir(String uploadDir) { this.uploadDir = uploadDir; }
    public List<String> getCollegeEmailSuffixes() { return collegeEmailSuffixes; }
    public void setCollegeEmailSuffixes(List<String> collegeEmailSuffixes) { this.collegeEmailSuffixes = collegeEmailSuffixes; }
    public boolean isAutoApproveCollegeEmail() { return autoApproveCollegeEmail; }
    public void setAutoApproveCollegeEmail(boolean autoApproveCollegeEmail) { this.autoApproveCollegeEmail = autoApproveCollegeEmail; }
    public Matching getMatching() { return matching; }
    public Claim getClaim() { return claim; }
    public Otp getOtp() { return otp; }
    public Mail getMail() { return mail; }
    public Admin getAdmin() { return admin; }
    public RateLimit getRateLimit() { return rateLimit; }

    public static class Jwt {
        private String secret;
        private long expirationMs = 86400000;
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getExpirationMs() { return expirationMs; }
        public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; }
    }

    public static class Crypto {
        private String key;
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
    }

    public static class Matching {
        private int dateMaxDays = 14;
        private Weights weights = new Weights();
        private Confidence confidence = new Confidence();
        public int getDateMaxDays() { return dateMaxDays; }
        public void setDateMaxDays(int dateMaxDays) { this.dateMaxDays = dateMaxDays; }
        public Weights getWeights() { return weights; }
        public Confidence getConfidence() { return confidence; }

        public static class Weights {
            private BigDecimal text = new BigDecimal("0.40");
            private BigDecimal location = new BigDecimal("0.20");
            private BigDecimal date = new BigDecimal("0.15");
            private BigDecimal category = new BigDecimal("0.10");
            private BigDecimal brand = new BigDecimal("0.10");
            private BigDecimal color = new BigDecimal("0.05");
            public BigDecimal getText() { return text; }
            public void setText(BigDecimal text) { this.text = text; }
            public BigDecimal getLocation() { return location; }
            public void setLocation(BigDecimal location) { this.location = location; }
            public BigDecimal getDate() { return date; }
            public void setDate(BigDecimal date) { this.date = date; }
            public BigDecimal getCategory() { return category; }
            public void setCategory(BigDecimal category) { this.category = category; }
            public BigDecimal getBrand() { return brand; }
            public void setBrand(BigDecimal brand) { this.brand = brand; }
            public BigDecimal getColor() { return color; }
            public void setColor(BigDecimal color) { this.color = color; }
        }

        public static class Confidence {
            private BigDecimal high = new BigDecimal("80.0");
            private BigDecimal possible = new BigDecimal("60.0");
            public BigDecimal getHigh() { return high; }
            public void setHigh(BigDecimal high) { this.high = high; }
            public BigDecimal getPossible() { return possible; }
            public void setPossible(BigDecimal possible) { this.possible = possible; }
        }
    }

    public static class Claim {
        private Verification verification = new Verification();
        public Verification getVerification() { return verification; }
        public static class Verification {
            private BigDecimal threshold = new BigDecimal("60.0");
            private int maxAttempts = 3;
            public BigDecimal getThreshold() { return threshold; }
            public void setThreshold(BigDecimal threshold) { this.threshold = threshold; }
            public int getMaxAttempts() { return maxAttempts; }
            public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        }
    }

    public static class Otp {
        private int expiryMinutes = 10;
        private int maxAttempts = 3;
        public int getExpiryMinutes() { return expiryMinutes; }
        public void setExpiryMinutes(int expiryMinutes) { this.expiryMinutes = expiryMinutes; }
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    }

    public static class Mail {
        private boolean enabled;
        private String from = "noreply@campusguard.local";
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
    }

    public static class Admin {
        private String email;
        private String password;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RateLimit {
        private int loginPerMinute = 8;
        private int verifyPerMinute = 10;
        public int getLoginPerMinute() { return loginPerMinute; }
        public void setLoginPerMinute(int loginPerMinute) { this.loginPerMinute = loginPerMinute; }
        public int getVerifyPerMinute() { return verifyPerMinute; }
        public void setVerifyPerMinute(int verifyPerMinute) { this.verifyPerMinute = verifyPerMinute; }
    }
}
