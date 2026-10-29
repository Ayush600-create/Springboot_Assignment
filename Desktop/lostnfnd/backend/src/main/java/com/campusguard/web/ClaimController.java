package com.campusguard.web;

import com.campusguard.dto.Dtos;
import com.campusguard.service.ClaimService;
import com.campusguard.service.CurrentUserService;
import com.campusguard.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {
    private final ClaimService claims;
    private final ReportService reports;
    private final CurrentUserService current;

    public ClaimController(ClaimService claims, ReportService reports, CurrentUserService current) {
        this.claims = claims;
        this.reports = reports;
        this.current = current;
    }

    @PostMapping
    public Dtos.ClaimView submit(@Valid @RequestBody Dtos.ClaimRequest req) {
        return claims.submit(current.require(), reports.loadMatch(req.matchId()), req.ownershipStatement());
    }

    @GetMapping("/mine")
    public List<Dtos.ClaimView> mine() {
        return claims.mine(current.require());
    }

    @GetMapping("/{id}")
    public Dtos.ClaimView one(@PathVariable Long id) {
        return claims.getForUser(current.require(), id);
    }
}
