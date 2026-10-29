package com.campusguard.repo;

import com.campusguard.domain.Claim;
import com.campusguard.domain.Enums;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    List<Claim> findByClaimantIdOrderByCreatedAtDesc(Long claimantId);
    Optional<Claim> findByMatchIdAndClaimantId(Long matchId, Long claimantId);
    long countByStatus(Enums.ClaimStatus status);
    List<Claim> findByStatusInOrderByCreatedAtDesc(List<Enums.ClaimStatus> statuses);

    @Query("""
            SELECT COUNT(c) FROM Claim c
            WHERE c.claimant.id = :claimantId
              AND (c.match.lostReport.id = :itemId OR c.match.foundReport.id = :itemId)
            """)
    long countAttemptsForItem(@Param("claimantId") Long claimantId, @Param("itemId") Long itemId);
}
