package io.github.drdeathdrop.atlas.resource.inventory;

import io.github.drdeathdrop.atlas.resource.ResourceSummary;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/resources")
public class ResourceController {
    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResourceSummary create(@Valid @RequestBody CreateResourceRequest request) {
        return resourceService.create(request);
    }

    @GetMapping
    public List<ResourceSummary> list() {
        return resourceService.list();
    }

    @GetMapping("/{resourceId}")
    public ResourceSummary get(@PathVariable UUID resourceId) {
        return resourceService.get(resourceId);
    }

    @PatchMapping("/{resourceId}/location")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'FIELD_OPERATOR')")
    public ResourceSummary updateLocation(@PathVariable UUID resourceId,
                                          @Valid @RequestBody UpdateLocationRequest request) {
        return resourceService.updateLocation(resourceId, request);
    }

    @PatchMapping("/{resourceId}/service")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResourceSummary setInService(@PathVariable UUID resourceId,
                                        @Valid @RequestBody UpdateServiceStateRequest request) {
        return resourceService.setInService(resourceId, request.inService());
    }
}
