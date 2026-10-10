package io.github.drdeathdrop.atlas.incident.dispatch;

import io.github.drdeathdrop.atlas.incident.IncidentSummary;
import io.github.drdeathdrop.atlas.incident.core.IncidentService;
import io.github.drdeathdrop.atlas.resource.AssignmentSummary;
import io.github.drdeathdrop.atlas.resource.NearbyResource;
import io.github.drdeathdrop.atlas.resource.ResourceAllocation;
import io.github.drdeathdrop.atlas.resource.ResourceType;
import io.github.drdeathdrop.atlas.user.Role;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incidents/{incidentId}")
public class DispatchController {
    private final IncidentService incidentService;
    private final DispatchService dispatchService;
    private final ResourceAllocation resourceAllocation;

    public DispatchController(IncidentService incidentService, DispatchService dispatchService,
                              ResourceAllocation resourceAllocation) {
        this.incidentService = incidentService;
        this.dispatchService = dispatchService;
        this.resourceAllocation = resourceAllocation;
    }

    @GetMapping("/nearby-resources")
    public List<NearbyResource> nearbyResources(@PathVariable UUID incidentId,
                                                @RequestParam(defaultValue = "15") double radiusKm,
                                                @RequestParam(required = false) ResourceType type) {
        IncidentSummary incident = incidentService.get(incidentId);
        return resourceAllocation.findAvailableNear(incident.latitude(), incident.longitude(), radiusKm, type);
    }

    @PostMapping("/dispatch")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public AssignmentSummary dispatch(@PathVariable UUID incidentId,
                                      @Valid @RequestBody DispatchRequest request,
                                      @AuthenticationPrincipal Jwt jwt) {
        return dispatchService.dispatch(
                incidentId,
                request.resourceId(),
                UUID.fromString(jwt.getSubject()),
                Role.valueOf(jwt.getClaimAsString("role")));
    }

    @GetMapping("/assignments")
    public List<AssignmentSummary> assignments(@PathVariable UUID incidentId) {
        incidentService.get(incidentId);
        return resourceAllocation.assignmentsFor(incidentId);
    }
}
