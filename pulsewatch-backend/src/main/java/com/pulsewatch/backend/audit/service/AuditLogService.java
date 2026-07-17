package com.pulsewatch.backend.audit.service;

import com.pulsewatch.backend.audit.entity.AuditLog;
import com.pulsewatch.backend.audit.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void log(UUID userId, String action, String resourceType, UUID resourceId) {
        AuditLog log = new AuditLog(userId, action, resourceType, resourceId);
        auditLogRepository.save(log);
    }
}
