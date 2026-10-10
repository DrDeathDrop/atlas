package io.github.drdeathdrop.atlas.facility.registry;

import io.github.drdeathdrop.atlas.facility.FacilitySummary;
import io.github.drdeathdrop.atlas.shared.geo.GeoPoints;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FacilityService {
    private final FacilityRepository repository;

    public FacilityService(FacilityRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public FacilitySummary create(CreateFacilityRequest request) {
        String name = request.name().trim();
        if (repository.existsByName(name)) {
            throw new FacilityNameAlreadyUsedException(name);
        }

        String address = request.address() == null || request.address().isBlank() ? null : request.address().trim();

        Facility facility = new Facility(
                name,
                request.type(),
                address,
                GeoPoints.of(request.latitude(), request.longitude()),
                request.capacity());

        return toSummary(repository.saveAndFlush(facility));
    }

    @Transactional
    public FacilitySummary updateOccupancy(UUID facilityId, UpdateOccupancyRequest request) {
        Facility facility = repository.findById(facilityId)
                .orElseThrow(() -> new FacilityNotFoundException(facilityId));

        if (request.occupancy() > facility.getCapacity()) {
            throw new OccupancyOverCapacityException(request.occupancy(), facility.getCapacity());
        }
        facility.changeOccupancy(request.occupancy());

        return toSummary(facility);
    }

    @Transactional(readOnly = true)
    public List<FacilitySummary> list() {
        return repository.findAll(Sort.by("name")).stream()
                .map(FacilityService::toSummary)
                .toList();
    }

    private static FacilitySummary toSummary(Facility facility) {
        return new FacilitySummary(
                facility.getId(),
                facility.getName(),
                facility.getType(),
                facility.getAddress(),
                GeoPoints.latitude(facility.getLocation()),
                GeoPoints.longitude(facility.getLocation()),
                facility.getCapacity(),
                facility.getOccupancy());
    }
}
