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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDoubles.class)
@TestPropertySource(properties = "campusguard.claim.verification.threshold=45.0")
class ClaimThresholdIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void sameFiftyScoreMeetsLowerConfiguredThreshold() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String lostEmail = "lost" + suffix + "@srmist.edu.in";
        String foundEmail = "found" + suffix + "@srmist.edu.in";
        String adminToken = token("admin@test.local", "TestAdmin!pass1");
        registerActive(lostEmail, "Lost", adminToken);
        registerActive(foundEmail, "Found", adminToken);
        String lostToken = token(lostEmail, "Password1!");
        String foundToken = token(foundEmail, "Password1!");

        long lostId = createReport(lostToken, "LOST", "alpha beta gamma delta");
        createReport(foundToken, "FOUND", null);
        JsonNode matches = mapper.readTree(mvc.perform(get("/api/reports/" + lostId + "/matches")
                        .header("Authorization", "Bearer " + lostToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        long matchId = matches.get(0).get("id").asLong();

        mvc.perform(post("/api/claims").header("Authorization", "Bearer " + lostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":%d,\"ownershipStatement\":\"alpha beta\"}".formatted(matchId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.verificationDecision").value("MEETS_THRESHOLD"));
    }

    private long createReport(String token, String type, String privateMarks) throws Exception {
        var builder = multipart("/api/reports")
                .param("reportType", type)
                .param("itemName", "canvas tote")
                .param("category", "BAGS")
                .param("publicDescription", "plain canvas tote")
                .param("location", "south gate")
                .param("occurredAt", "2026-10-21T10:00:00")
                .param("brand", "Generic")
                .param("color", "beige");
        if (privateMarks != null) {
            builder = builder.param("privateIdentifyingMarks", privateMarks);
        }
        MvcResult res = mvc.perform(builder.header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }

    private void registerActive(String email, String name, String adminToken) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"%s","email":"%s","password":"Password1!","studentId":"RA%s","department":"CSE"}
                """.formatted(name, email, UUID.randomUUID().toString().substring(0, 6))))
                .andExpect(status().isOk());
        JsonNode users = mapper.readTree(mvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)).andReturn().getResponse().getContentAsString());
        long id = 0;
        for (JsonNode n : users) {
            if (email.equalsIgnoreCase(n.get("email").asText())) id = n.get("id").asLong();
        }
        mvc.perform(post("/api/admin/users/" + id + "/decision")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\",\"reason\":\"approved\"}"))
                .andExpect(status().isOk());
    }

    private String token(String email, String password) throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }
}
