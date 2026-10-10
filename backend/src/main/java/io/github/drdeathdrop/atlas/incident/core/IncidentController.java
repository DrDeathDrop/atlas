package io.github.drdeathdrop.atlas.incident.core;

import io.github.drdeathdrop.atlas.incident.IncidentSummary;
import io.github.drdeathdrop.atlas.user.Role;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {
    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public IncidentSummary report(@Valid @RequestBody ReportIncidentRequest request,
                                  @AuthenticationPrincipal Jwt jwt) {
        return incidentService.report(request, userId(jwt));
    }

    @GetMapping
    public List<IncidentSummary> list() {
        return incidentService.list();
    }

    @GetMapping("/{incidentId}")
    public IncidentSummary get(@PathVariable UUID incidentId) {
        return incidentService.get(incidentId);
    }

    @PatchMapping("/{incidentId}/status")
    public IncidentSummary changeStatus(@PathVariable UUID incidentId,
                                        @Valid @RequestBody ChangeStatusRequest request,
                                        @AuthenticationPrincipal Jwt jwt) {
        return incidentService.changeStatus(incidentId, request.status(), userId(jwt), role(jwt));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private static Role role(Jwt jwt) {
        return Role.valueOf(jwt.getClaimAsString("role"));
    }
}
