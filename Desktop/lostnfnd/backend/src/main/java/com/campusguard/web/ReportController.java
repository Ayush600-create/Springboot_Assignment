package com.campusguard.web;

import com.campusguard.domain.Enums;
import com.campusguard.dto.Dtos;
import com.campusguard.service.CurrentUserService;
import com.campusguard.service.FileStorageService;
import com.campusguard.service.ReportService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ReportController {
    private final ReportService reports;
    private final CurrentUserService current;
    private final FileStorageService files;

    public ReportController(ReportService reports, CurrentUserService current, FileStorageService files) {
        this.reports = reports;
        this.current = current;
        this.files = files;
    }

    @PostMapping("/reports")
    public Dtos.ReportCard create(
            @RequestParam Enums.ReportType reportType,
            @RequestParam String itemName,
            @RequestParam Enums.ItemCategory category,
            @RequestParam String publicDescription,
            @RequestParam String location,
            @RequestParam LocalDateTime occurredAt,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String serialNumber,
            @RequestParam(required = false) String privateIdentifyingMarks,
            @RequestParam(required = false) MultipartFile photograph
    ) {
        return reports.create(current.require(), reportType, itemName, category, publicDescription, location,
                occurredAt, brand, color, serialNumber, privateIdentifyingMarks, photograph);
    }

    @GetMapping("/reports")
    public Page<Dtos.ReportCard> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Enums.ReportType type,
            @RequestParam(required = false) Enums.ItemCategory category,
            @RequestParam(required = false) Enums.ReportStatus status,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return reports.search(q, type, category, status, location, brand, color, fromDate, toDate, sort, page, size);
    }

    @GetMapping("/reports/mine")
    public List<Dtos.ReportCard> mine() {
        return reports.mine(current.require());
    }

    @GetMapping("/reports/{id}")
    public Dtos.ReportCard one(@PathVariable Long id) {
        return reports.get(id);
    }

    @GetMapping("/reports/{id}/matches")
    public List<Dtos.MatchView> matches(@PathVariable Long id) {
        return reports.matchesFor(id);
    }

    @GetMapping("/matches/{id}")
    public Dtos.MatchView match(@PathVariable Long id) {
        return reports.match(id);
    }

    @GetMapping("/files/{filename}")
    public ResponseEntity<Resource> file(@PathVariable String filename) {
        var path = files.resolve(filename);
        return ResponseEntity.ok()
                .contentType(files.mediaType(filename))
                .cacheControl(CacheControl.noStore())
                .body(new FileSystemResource(path));
    }
}
