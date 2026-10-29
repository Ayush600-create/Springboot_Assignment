package com.campusguard;

import com.campusguard.domain.Handover;
import com.campusguard.repo.HandoverRepository;
import com.campusguard.support.TestDoubles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDoubles.class)
@TestPropertySource(properties = "campusguard.mail.enabled=true")
class OtpHandoverIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired HandoverRepository handovers;
    @Autowired TestDoubles.CapturingMailSender mail;
    @Autowired TestDoubles.ConfigurableOtpGenerator otp;

    @Test
    void correctWrongExpiredReusedAndAttemptLimit() throws Exception {
        otp.set("654321");
        mail.messages.clear();
        long claimA = approvedClaim("a");
        mvc.perform(post("/api/admin/handovers/" + claimA + "/verify-otp")
                        .header("Authorization", bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"000000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Incorrect OTP"));

        mvc.perform(post("/api/admin/handovers/" + claimA + "/verify-otp")
                        .header("Authorization", bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mvc.perform(post("/api/admin/handovers/" + claimA + "/verify-otp")
                        .header("Authorization", bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"654321\"}"))
                .andExpect(status().isConflict());

        assertTrue(mail.messages.stream().anyMatch(m -> m.getText() != null && m.getText().contains("654321")));

        long claimB = approvedClaim("b");
        Handover handover = handovers.findByClaimId(claimB).orElseThrow();
        handover.setOtpExpiresAt(Instant.now().minusSeconds(60));
        handovers.save(handover);
        mvc.perform(post("/api/admin/handovers/" + claimB + "/verify-otp")
                        .header("Authorization", bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"654321\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("OTP has expired"));

        long claimC = approvedClaim("c");
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/admin/handovers/" + claimC + "/verify-otp")
                            .header("Authorization", bearerAdmin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"otp\":\"000000\"}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/admin/handovers/" + claimC + "/verify-otp")
                        .header("Authorization", bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"000000\"}"))
                .andExpect(status().isTooManyRequests());
    }

    private long approvedClaim(String tag) throws Exception {
        String suffix = tag + UUID.randomUUID().toString().substring(0, 6);
        String lostEmail = "l" + suffix + "@srmist.edu.in";
        String foundEmail = "f" + suffix + "@srmist.edu.in";
        String admin = token("admin@test.local", "TestAdmin!pass1");
        registerActive(lostEmail, admin);
        registerActive(foundEmail, admin);
        String lostToken = token(lostEmail, "Password1!");
        String foundToken = token(foundEmail, "Password1!");
        long lostId = report(lostToken, "LOST", "alpha beta gamma delta");
        report(foundToken, "FOUND", null);
        JsonNode matches = mapper.readTree(mvc.perform(get("/api/reports/" + lostId + "/matches")
                        .header("Authorization", "Bearer " + lostToken)).andReturn().getResponse().getContentAsString());
        long matchId = matches.get(0).get("id").asLong();
        MvcResult claimRes = mvc.perform(post("/api/claims").header("Authorization", "Bearer " + lostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":%d,\"ownershipStatement\":\"alpha beta gamma delta\"}".formatted(matchId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andReturn();
        long claimId = mapper.readTree(claimRes.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(post("/api/admin/claims/" + claimId + "/open-review")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        mvc.perform(post("/api/admin/claims/" + claimId + "/approve")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"documents checked\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpStatus").value("ISSUED"));
        return claimId;
    }

    private long report(String token, String type, String marks) throws Exception {
        var builder = multipart("/api/reports")
                .param("reportType", type)
                .param("itemName", "lab keys")
                .param("category", "KEYS")
                .param("publicDescription", "bunch of lab keys")
                .param("location", "block a")
                .param("occurredAt", "2026-10-22T09:00:00")
                .param("brand", "Yale")
                .param("color", "silver");
        if (marks != null) builder = builder.param("privateIdentifyingMarks", marks);
        return mapper.readTree(mvc.perform(builder.header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    private void registerActive(String email, String admin) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"User","email":"%s","password":"Password1!","studentId":"RA%s","department":"CSE"}
                """.formatted(email, UUID.randomUUID().toString().substring(0, 6)))).andExpect(status().isOk());
        JsonNode users = mapper.readTree(mvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + admin)).andReturn().getResponse().getContentAsString());
        long id = 0;
        for (JsonNode n : users) {
            if (email.equalsIgnoreCase(n.get("email").asText())) id = n.get("id").asLong();
        }
        mvc.perform(post("/api/admin/users/" + id + "/decision")
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACTIVE\",\"reason\":\"approved\"}")).andExpect(status().isOk());
    }

    private String token(String email, String password) throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    private String bearerAdmin() throws Exception {
        return "Bearer " + token("admin@test.local", "TestAdmin!pass1");
    }
}
