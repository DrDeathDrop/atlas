package io.github.drdeathdrop.atlas.audit.log;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
public class AuditController {
    private final AuditEntryRepository repository;

    public AuditController(AuditEntryRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/incidents/{incidentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'DISPATCHER')")
    @Transactional(readOnly = true)
    public List<AuditEntryView> incidentHistory(@PathVariable UUID incidentId) {
        return repository.findByEntityTypeAndEntityIdOrderByOccurredAtAsc(AuditEntry.INCIDENT, incidentId).stream()
                .map(AuditEntryView::of)
                .toList();
    }
}
