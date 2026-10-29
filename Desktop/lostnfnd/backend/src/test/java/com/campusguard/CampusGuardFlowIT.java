package com.campusguard;

import com.campusguard.support.TestDoubles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDoubles.class)
class CampusGuardFlowIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void registerLoginAuthorizationReportsMatchingClaimsPrivacyAndSmtpBlock() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String studentEmail = "stu" + suffix + "@srmist.edu.in";
        String finderEmail = "find" + suffix + "@srmist.edu.in";
        String outsiderEmail = "out" + suffix + "@srmist.edu.in";

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Pending Student","email":"%s","password":"Password1!","studentId":"RA1","department":"CSE"}
                """.formatted(studentEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(nullValue()))
                .andExpect(jsonPath("$.user.accountStatus").value("PENDING_APPROVAL"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","password":"Password1!"}
                """.formatted(studentEmail)))
                .andExpect(status().isForbidden());

        String adminToken = token("admin@test.local", "TestAdmin!pass1");
        approve(adminToken, studentEmail);
        String studentToken = token(studentEmail, "Password1!");
        registerActive(finderEmail, "Finder User", adminToken);
        registerActive(outsiderEmail, "Outsider", adminToken);
        String finderToken = token(finderEmail, "Password1!");
        String outsiderToken = token(outsiderEmail, "Password1!");

        mvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        MockMultipartFile photo = new MockMultipartFile("photograph", "item.jpg", "image/jpeg", new byte[]{1, 2, 3, 4});
        MvcResult lostRes = mvc.perform(multipart("/api/reports")
                        .file(photo)
                        .param("reportType", "LOST")
                        .param("itemName", "sony headphones")
                        .param("category", "ELECTRONICS")
                        .param("publicDescription", "black over ear headphones")
                        .param("location", "central library")
                        .param("occurredAt", "2026-10-20T16:30:00")
                        .param("brand", "Sony")
                        .param("color", "black")
                        .param("serialNumber", "SECRET-SERIAL-4411")
                        .param("privateIdentifyingMarks", "alpha beta gamma delta")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemName").value("sony headphones"))
                .andReturn();
        String lostJson = lostRes.getResponse().getContentAsString();
        assertFalse(lostJson.contains("SECRET-SERIAL-4411"));
        assertFalse(lostJson.contains("alpha beta gamma delta"));
        assertFalse(lostJson.contains("ciphertext"));
        long lostId = mapper.readTree(lostJson).get("id").asLong();

        mvc.perform(multipart("/api/reports")
                        .param("reportType", "FOUND")
                        .param("itemName", "sony headphones")
                        .param("category", "ELECTRONICS")
                        .param("publicDescription", "black over ear headphones")
                        .param("location", "central library")
                        .param("occurredAt", "2026-10-20T16:30:00")
                        .param("brand", "Sony")
                        .param("color", "black")
                        .header("Authorization", "Bearer " + finderToken))
                .andExpect(status().isOk());

        JsonNode matches = getJson(studentToken, "/api/reports/" + lostId + "/matches");
        assertEquals(1, matches.size());
        assertEquals(new BigDecimal("100.0"), matches.get(0).get("score").decimalValue());
        assertEquals("HIGH_CONFIDENCE", matches.get(0).get("confidence").asText());
        long matchId = matches.get(0).get("id").asLong();

        mvc.perform(get("/api/reports").param("q", "headphones").param("category", "ELECTRONICS")
                        .param("type", "LOST").param("location", "library")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].itemName", containsString("headphones")));

        String lostBody = mvc.perform(get("/api/reports/" + lostId).header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(lostBody.contains("SECRET-SERIAL-4411"));
        assertFalse(lostBody.toLowerCase().contains("alpha beta gamma delta"));

        mvc.perform(post("/api/claims").header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":%d,\"ownershipStatement\":\"alpha beta\"}".formatted(matchId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reviewStatus").value("FLAGGED_FOR_ADMIN_REVIEW"))
                .andExpect(jsonPath("$.verificationDecision").value("BELOW_THRESHOLD"))
                .andExpect(jsonPath("$.verificationScore").value(nullValue()));

        JsonNode queue = getJson(adminToken, "/api/admin/claims");
        long claimId = 0;
        for (JsonNode n : queue) {
            if (n.get("matchId").asLong() == matchId) {
                claimId = n.get("id").asLong();
                assertEquals("50.0", n.get("verificationScore").asText());
                assertEquals("60.0", n.get("thresholdUsed").asText());
            }
        }
        assertTrue(claimId > 0);

        mvc.perform(post("/api/admin/claims/" + claimId + "/approve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"looks fine\"}"))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/admin/claims/" + claimId + "/reopen")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/admin/claims/" + claimId + "/reopen")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"claimant produced extra campus ID evidence\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        mvc.perform(post("/api/admin/claims/" + claimId + "/reopen")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"second reopen\"}"))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/admin/claims/" + claimId + "/approve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"identity documents match the report\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED_FOR_HANDOVER"))
                .andExpect(jsonPath("$.otpStatus").value("DELIVERY_UNAVAILABLE"));

        mvc.perform(post("/api/admin/handovers/" + claimId + "/verify-otp")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("OTP email is not configured"));
    }

    @Test
    void fiftyPointScoreMeetsFortyFiveAndMissesSixty() {
        var engineScore = com.campusguard.service.MatchingEngine.jaccard(
                com.campusguard.service.MatchingEngine.tokenize("alpha beta gamma delta"),
                com.campusguard.service.MatchingEngine.tokenize("alpha beta"));
        assertEquals(new BigDecimal("50.0"), engineScore);
        assertTrue(engineScore.compareTo(new BigDecimal("45.0")) >= 0);
        assertTrue(engineScore.compareTo(new BigDecimal("60.0")) < 0);
    }

    private void registerActive(String email, String name, String adminToken) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"%s","email":"%s","password":"Password1!","studentId":"RA%s","department":"CSE"}
                """.formatted(name, email, UUID.randomUUID().toString().substring(0, 6))))
                .andExpect(status().isOk());
        approve(adminToken, email);
    }

    private void approve(String adminToken, String email) throws Exception {
        long id = id(getJson(adminToken, "/api/admin/users"), email);
        mvc.perform(post("/api/admin/users/" + id + "/decision")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\",\"reason\":\"approved\"}"))
                .andExpect(status().isOk());
    }

    private String token(String email, String password) throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    private JsonNode getJson(String token, String url) throws Exception {
        MvcResult res = mvc.perform(get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readTree(res.getResponse().getContentAsString());
    }

    private long id(JsonNode users, String email) {
        for (JsonNode n : users) {
            if (email.equalsIgnoreCase(n.get("email").asText())) {
                return n.get("id").asLong();
            }
        }
        throw new AssertionError("user not found: " + email);
    }
}
