package com.campusguard.repo;

import com.campusguard.domain.ItemMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<ItemMatch, Long> {
    Optional<ItemMatch> findByLostReportIdAndFoundReportId(Long lostId, Long foundId);
    List<ItemMatch> findByLostReportIdOrFoundReportIdOrderByScoreDesc(Long lostId, Long foundId);
}
