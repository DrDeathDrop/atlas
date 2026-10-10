package io.github.drdeathdrop.atlas.facility.registry;

import io.github.drdeathdrop.atlas.facility.FacilitySummary;
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
@RequestMapping("/api/facilities")
public class FacilityController {
    private final FacilityService facilityService;

    public FacilityController(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public FacilitySummary create(@Valid @RequestBody CreateFacilityRequest request) {
        return facilityService.create(request);
    }

    @GetMapping
    public List<FacilitySummary> list() {
        return facilityService.list();
    }

    @PatchMapping("/{facilityId}/occupancy")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'FIELD_OPERATOR')")
    public FacilitySummary updateOccupancy(@PathVariable UUID facilityId,
                                           @Valid @RequestBody UpdateOccupancyRequest request) {
        return facilityService.updateOccupancy(facilityId, request);
    }
}
