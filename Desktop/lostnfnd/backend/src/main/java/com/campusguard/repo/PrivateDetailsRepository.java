package com.campusguard.repo;

import com.campusguard.domain.ItemPrivateDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrivateDetailsRepository extends JpaRepository<ItemPrivateDetails, Long> {
    Optional<ItemPrivateDetails> findByReportId(Long reportId);
    boolean existsByReportId(Long reportId);
}
