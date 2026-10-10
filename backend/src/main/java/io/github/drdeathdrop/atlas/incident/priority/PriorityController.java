package io.github.drdeathdrop.atlas.incident.priority;

import io.github.drdeathdrop.atlas.incident.Priority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incidents")
public class PriorityController {
    private final PriorityService priorityService;

    public PriorityController(PriorityService priorityService) {
        this.priorityService = priorityService;
    }

    @GetMapping("/priorities")
    public List<Priority> openIncidentsByPriority() {
        return priorityService.openIncidentsByPriority();
    }

    @GetMapping("/{incidentId}/priority")
    public Priority forIncident(@PathVariable UUID incidentId) {
        return priorityService.forIncident(incidentId);
    }
}
