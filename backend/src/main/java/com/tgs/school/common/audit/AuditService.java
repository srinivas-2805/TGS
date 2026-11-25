package com.tgs.school.common.audit;

import com.tgs.school.auth.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String entityType, String entityId, String details) {
        Long actorId = SecurityUtils.getCurrentUserIdOrNull();
        String actorUsername = SecurityUtils.getCurrentUsernameOrSystem();
        auditLogRepository.save(AuditLog.builder()
                .actorUserId(actorId)
                .actorUsername(actorUsername)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build());
    }
}
