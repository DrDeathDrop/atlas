package io.github.drdeathdrop.atlas.incident.dispatch;

import io.github.drdeathdrop.atlas.incident.IncidentSummary;
import io.github.drdeathdrop.atlas.incident.core.IncidentService;
import io.github.drdeathdrop.atlas.resource.NearbyResource;
import io.github.drdeathdrop.atlas.resource.ResourceAllocation;
import io.github.drdeathdrop.atlas.resource.ResourceType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incidents/{incidentId}")
public class DispatchController {
    private final IncidentService incidentService;
    private final ResourceAllocation resourceAllocation;

    public DispatchController(IncidentService incidentService, ResourceAllocation resourceAllocation) {
        this.incidentService = incidentService;
        this.resourceAllocation = resourceAllocation;
    }

    @GetMapping("/nearby-resources")
    public List<NearbyResource> nearbyResources(@PathVariable UUID incidentId,
                                                @RequestParam(defaultValue = "15") double radiusKm,
                                                @RequestParam(required = false) ResourceType type) {
        IncidentSummary incident = incidentService.get(incidentId);
        return resourceAllocation.findAvailableNear(incident.latitude(), incident.longitude(), radiusKm, type);
    }
}
