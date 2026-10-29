package com.campusguard.repo;

import com.campusguard.domain.Enums;
import com.campusguard.domain.LostFoundReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportRepository extends JpaRepository<LostFoundReport, Long> {
    List<LostFoundReport> findByReporterIdOrderByCreatedAtDesc(Long reporterId);
    List<LostFoundReport> findByReportTypeAndStatusIn(Enums.ReportType type, List<Enums.ReportStatus> statuses);
    long countByReportType(Enums.ReportType type);
    long countByReportTypeAndStatusNot(Enums.ReportType type, Enums.ReportStatus status);
    long countByStatusNot(Enums.ReportStatus status);

    @Query("""
            SELECT r FROM LostFoundReport r
            WHERE (:type IS NULL OR r.reportType = :type)
              AND (:category IS NULL OR r.category = :category)
              AND (:status IS NULL OR r.status = :status)
              AND (:location IS NULL OR LOWER(r.location) LIKE LOWER(CONCAT('%', :location, '%')))
              AND (:brand IS NULL OR LOWER(r.brand) LIKE LOWER(CONCAT('%', :brand, '%')))
              AND (:color IS NULL OR LOWER(r.color) LIKE LOWER(CONCAT('%', :color, '%')))
              AND (:fromDate IS NULL OR r.occurredAt >= :fromDate)
              AND (:toDate IS NULL OR r.occurredAt <= :toDate)
              AND (
                    :q IS NULL OR :q = '' OR
                    LOWER(r.itemName) LIKE LOWER(CONCAT('%', :q, '%')) OR
                    LOWER(r.publicDescription) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
            """)
    Page<LostFoundReport> search(
            @Param("q") String q,
            @Param("type") Enums.ReportType type,
            @Param("category") Enums.ItemCategory category,
            @Param("status") Enums.ReportStatus status,
            @Param("location") String location,
            @Param("brand") String brand,
            @Param("color") String color,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);
}
