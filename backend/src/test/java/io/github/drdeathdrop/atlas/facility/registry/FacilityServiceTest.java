package io.github.drdeathdrop.atlas.facility.registry;

import io.github.drdeathdrop.atlas.facility.FacilitySummary;
import io.github.drdeathdrop.atlas.facility.FacilityType;
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
class FacilityServiceTest {

    @Mock
    private FacilityRepository repository;

    @Mock
    private ApplicationEventPublisher events;

    @InjectMocks
    private FacilityService service;

    @Test
    void createStoresAnEmptyFacility() {
        when(repository.existsByName("City Hospital")).thenReturn(false);
        when(repository.saveAndFlush(any(Facility.class))).thenAnswer(call -> call.getArgument(0));

        FacilitySummary result = service.create(new CreateFacilityRequest(
                " City Hospital ", FacilityType.HOSPITAL, "  ", 42.1354, 24.7453, 400));

        assertThat(result.name()).isEqualTo("City Hospital");
        assertThat(result.type()).isEqualTo(FacilityType.HOSPITAL);
        assertThat(result.address()).isNull();
        assertThat(result.capacity()).isEqualTo(400);
        assertThat(result.occupancy()).isZero();
        assertThat(result.latitude()).isCloseTo(42.1354, within(0.000001));
        assertThat(result.longitude()).isCloseTo(24.7453, within(0.000001));
    }

    @Test
    void createRejectsANameThatIsAlreadyUsed() {
        when(repository.existsByName("City Hospital")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateFacilityRequest(
                "City Hospital", FacilityType.HOSPITAL, null, 42.1354, 24.7453, 400)))
                .isInstanceOf(FacilityNameAlreadyUsedException.class);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void updateOccupancyChangesHowFullTheFacilityIs() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(shelter(500)));

        FacilitySummary result = service.updateOccupancy(id, new UpdateOccupancyRequest(500));

        assertThat(result.occupancy()).isEqualTo(500);
    }

    @Test
    void updateOccupancyRejectsMorePeopleThanTheCapacity() {
        UUID id = UUID.randomUUID();
        Facility shelter = shelter(500);
        when(repository.findById(id)).thenReturn(Optional.of(shelter));

        assertThatThrownBy(() -> service.updateOccupancy(id, new UpdateOccupancyRequest(501)))
                .isInstanceOf(OccupancyOverCapacityException.class);

        assertThat(shelter.getOccupancy()).isZero();
    }

    @Test
    void updateOccupancyFailsForAnUnknownFacility() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateOccupancy(id, new UpdateOccupancyRequest(1)))
                .isInstanceOf(FacilityNotFoundException.class);
    }

    private static Facility shelter(int capacity) {
        return new Facility("Sports Hall", FacilityType.SHELTER, null, GeoPoints.of(42.14, 24.75), capacity);
    }
}
