package io.github.drdeathdrop.atlas.resource.inventory;

import io.github.drdeathdrop.atlas.resource.ResourceKind;
import io.github.drdeathdrop.atlas.resource.ResourceStatus;
import io.github.drdeathdrop.atlas.resource.ResourceSummary;
import io.github.drdeathdrop.atlas.resource.ResourceType;
import io.github.drdeathdrop.atlas.shared.geo.GeoPoints;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository repository;

    @Mock
    private ApplicationEventPublisher events;

    @InjectMocks
    private ResourceService service;

    @Test
    void createStoresANewResourceAsAvailable() {
        when(repository.existsByCallSign("AMBULANCE-17")).thenReturn(false);
        when(repository.saveAndFlush(any(Resource.class))).thenAnswer(call -> call.getArgument(0));

        ResourceSummary result = service.create(
                new CreateResourceRequest(" AMBULANCE-17 ", ResourceType.AMBULANCE, 42.1354, 24.7453, null));

        assertThat(result.callSign()).isEqualTo("AMBULANCE-17");
        assertThat(result.type()).isEqualTo(ResourceType.AMBULANCE);
        assertThat(result.kind()).isEqualTo(ResourceKind.VEHICLE);
        assertThat(result.status()).isEqualTo(ResourceStatus.AVAILABLE);
        assertThat(result.latitude()).isCloseTo(42.1354, within(0.000001));
        assertThat(result.longitude()).isCloseTo(24.7453, within(0.000001));
    }

    @Test
    void createRejectsACallSignThatIsAlreadyUsed() {
        when(repository.existsByCallSign("AMBULANCE-17")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new CreateResourceRequest("AMBULANCE-17", ResourceType.AMBULANCE, 42.1354, 24.7453, null)))
                .isInstanceOf(CallSignAlreadyUsedException.class);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsAVehicleWhoseTeamIsNotATeam() {
        UUID notATeam = UUID.randomUUID();
        when(repository.existsByCallSign("AMBULANCE-18")).thenReturn(false);
        when(repository.findById(notATeam)).thenReturn(Optional.of(
                new Resource("FIRETRUCK-04", ResourceType.FIRE_TRUCK, GeoPoints.of(42.1, 24.7), null)));

        assertThatThrownBy(() -> service.create(
                new CreateResourceRequest("AMBULANCE-18", ResourceType.AMBULANCE, 42.1354, 24.7453, notATeam)))
                .isInstanceOf(InvalidTeamException.class);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void updateLocationMovesTheResource() {
        UUID resourceId = UUID.randomUUID();
        Resource resource = new Resource("AMBULANCE-17", ResourceType.AMBULANCE, GeoPoints.of(42.1, 24.7), null);
        when(repository.findById(resourceId)).thenReturn(Optional.of(resource));

        ResourceSummary result = service.updateLocation(resourceId, new UpdateLocationRequest(42.2, 24.8));

        assertThat(result.latitude()).isCloseTo(42.2, within(0.000001));
        assertThat(result.longitude()).isCloseTo(24.8, within(0.000001));
    }

    @Test
    void aResourceCanBeTakenOutOfServiceAndReturned() {
        UUID resourceId = UUID.randomUUID();
        Resource resource = new Resource("AMBULANCE-17", ResourceType.AMBULANCE, GeoPoints.of(42.1, 24.7), null);
        when(repository.findByIdForUpdate(resourceId)).thenReturn(Optional.of(resource));

        assertThat(service.setInService(resourceId, false).status()).isEqualTo(ResourceStatus.OUT_OF_SERVICE);
        assertThat(service.setInService(resourceId, true).status()).isEqualTo(ResourceStatus.AVAILABLE);
    }

    @Test
    void aResourceOnAnIncidentCannotBeTakenOutOfService() {
        UUID resourceId = UUID.randomUUID();
        Resource resource = new Resource("AMBULANCE-17", ResourceType.AMBULANCE, GeoPoints.of(42.1, 24.7), null);
        resource.changeStatus(ResourceStatus.EN_ROUTE);
        when(repository.findByIdForUpdate(resourceId)).thenReturn(Optional.of(resource));

        assertThatThrownBy(() -> service.setInService(resourceId, false))
                .isInstanceOf(ResourceOnAssignmentException.class);

        assertThat(resource.getStatus()).isEqualTo(ResourceStatus.EN_ROUTE);
    }

    @Test
    void changingTheServiceStateFailsForAnUnknownResource() {
        UUID resourceId = UUID.randomUUID();
        when(repository.findByIdForUpdate(resourceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setInService(resourceId, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
