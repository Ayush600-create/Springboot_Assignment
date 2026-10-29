package com.campusguard.repo;

import com.campusguard.domain.Handover;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HandoverRepository extends JpaRepository<Handover, Long> {
    Optional<Handover> findByClaimId(Long claimId);
    long countByCompletedAtIsNotNull();
}
