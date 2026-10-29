package com.campusguard.service;

import com.campusguard.domain.AppUser;
import com.campusguard.domain.AuditLog;
import com.campusguard.dto.Dtos;
import com.campusguard.repo.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {
    private final AuditLogRepository logs;

    public AuditService(AuditLogRepository logs) {
        this.logs = logs;
    }

    @Transactional
    public void record(AppUser actor, String action, String entityType, Long entityId, String details) {
        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        logs.save(log);
    }

    @Transactional(readOnly = true)
    public List<Dtos.AuditView> recent() {
        return logs.findTop50ByOrderByCreatedAtDesc().stream()
                .map(a -> new Dtos.AuditView(
                        a.getId(),
                        a.getActor() == null ? "system" : a.getActor().getEmail(),
                        a.getAction(), a.getEntityType(), a.getEntityId(), a.getDetails(), a.getCreatedAt()))
                .toList();
    }
}
