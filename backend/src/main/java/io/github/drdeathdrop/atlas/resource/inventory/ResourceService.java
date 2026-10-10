package io.github.drdeathdrop.atlas.resource.inventory;

import io.github.drdeathdrop.atlas.resource.ResourceKind;
import io.github.drdeathdrop.atlas.resource.ResourceStatus;
import io.github.drdeathdrop.atlas.resource.ResourceSummary;
import io.github.drdeathdrop.atlas.shared.geo.GeoPoints;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ResourceService {
    private final ResourceRepository repository;

    public ResourceService(ResourceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ResourceSummary create(CreateResourceRequest request) {
        String callSign = request.callSign().trim();
        if (repository.existsByCallSign(callSign)) {
            throw new CallSignAlreadyUsedException(callSign);
        }

        if (request.teamId() != null) {
            Resource team = repository.findById(request.teamId())
                    .orElseThrow(() -> new ResourceNotFoundException(request.teamId()));
            if (team.getType().kind() != ResourceKind.TEAM || request.type().kind() != ResourceKind.VEHICLE) {
                throw new InvalidTeamException(request.teamId());
            }
        }

        Resource resource = new Resource(
                callSign,
                request.type(),
                GeoPoints.of(request.latitude(), request.longitude()),
                request.teamId());

        return toSummary(repository.saveAndFlush(resource));
    }

    @Transactional
    public ResourceSummary updateLocation(UUID resourceId, UpdateLocationRequest request) {
        Resource resource = repository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(resourceId));

        resource.relocate(GeoPoints.of(request.latitude(), request.longitude()));

        return toSummary(resource);
    }

    @Transactional
    public ResourceSummary setInService(UUID resourceId, boolean inService) {
        Resource resource = repository.findByIdForUpdate(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(resourceId));

        ResourceStatus status = resource.getStatus();
        if (status == ResourceStatus.EN_ROUTE || status == ResourceStatus.ON_SCENE) {
            throw new ResourceOnAssignmentException(resource.getCallSign());
        }
        resource.changeStatus(inService ? ResourceStatus.AVAILABLE : ResourceStatus.OUT_OF_SERVICE);

        return toSummary(resource);
    }

    @Transactional(readOnly = true)
    public ResourceSummary get(UUID resourceId) {
        return repository.findById(resourceId)
                .map(ResourceService::toSummary)
                .orElseThrow(() -> new ResourceNotFoundException(resourceId));
    }

    @Transactional(readOnly = true)
    public List<ResourceSummary> list() {
        return repository.findAll(Sort.by("callSign")).stream()
                .map(ResourceService::toSummary)
                .toList();
    }

    public static ResourceSummary toSummary(Resource resource) {
        return new ResourceSummary(
                resource.getId(),
                resource.getCallSign(),
                resource.getType().kind(),
                resource.getType(),
                resource.getStatus(),
                GeoPoints.latitude(resource.getLocation()),
                GeoPoints.longitude(resource.getLocation()),
                resource.getTeamId());
    }
}
