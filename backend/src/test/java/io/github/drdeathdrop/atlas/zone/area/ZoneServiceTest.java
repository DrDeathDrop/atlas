package io.github.drdeathdrop.atlas.zone.area;

import io.github.drdeathdrop.atlas.shared.geo.GeoPosition;
import io.github.drdeathdrop.atlas.shared.geo.GeoShapes;
import io.github.drdeathdrop.atlas.shared.geo.InvalidShapeException;
import io.github.drdeathdrop.atlas.zone.ZoneSummary;
import io.github.drdeathdrop.atlas.zone.ZoneType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZoneServiceTest {

    private static final List<GeoPosition> TRIANGLE = List.of(
            new GeoPosition(42.10, 24.70),
            new GeoPosition(42.10, 24.80),
            new GeoPosition(42.20, 24.75));

    private static final UUID DISPATCHER = UUID.randomUUID();

    @Mock
    private ZoneRepository repository;

    @InjectMocks
    private ZoneService service;

    @Test
    void createStoresTheZoneAndReturnsTheCornersThatWereSent() {
        when(repository.saveAndFlush(any(Zone.class))).thenAnswer(call -> call.getArgument(0));

        ZoneSummary result = service.create(
                new CreateZoneRequest(" Riverside ", ZoneType.EVACUATION, TRIANGLE), DISPATCHER);

        assertThat(result.name()).isEqualTo("Riverside");
        assertThat(result.type()).isEqualTo(ZoneType.EVACUATION);
        assertThat(result.boundary()).containsExactlyElementsOf(TRIANGLE);
        assertThat(result.liftedAt()).isNull();

        ArgumentCaptor<Zone> saved = ArgumentCaptor.forClass(Zone.class);
        verify(repository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getCreatedBy()).isEqualTo(DISPATCHER);
    }

    @Test
    void createRejectsABoundaryThatCrossesItself() {
        List<GeoPosition> bowTie = List.of(
                new GeoPosition(42.10, 24.70),
                new GeoPosition(42.20, 24.80),
                new GeoPosition(42.10, 24.80),
                new GeoPosition(42.20, 24.70));

        assertThatThrownBy(() -> service.create(
                new CreateZoneRequest("Riverside", ZoneType.AFFECTED, bowTie), DISPATCHER))
                .isInstanceOf(InvalidShapeException.class);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void liftRecordsWhoLiftedTheZoneAndWhen() {
        UUID id = UUID.randomUUID();
        Zone zone = zone();
        when(repository.findById(id)).thenReturn(Optional.of(zone));

        ZoneSummary result = service.lift(id, DISPATCHER);

        assertThat(result.liftedAt()).isNotNull();
        assertThat(zone.getLiftedBy()).isEqualTo(DISPATCHER);
        assertThat(zone.isLifted()).isTrue();
    }

    @Test
    void liftRejectsAZoneThatIsAlreadyLifted() {
        UUID id = UUID.randomUUID();
        Zone zone = zone();
        when(repository.findById(id)).thenReturn(Optional.of(zone));
        service.lift(id, DISPATCHER);

        assertThatThrownBy(() -> service.lift(id, DISPATCHER))
                .isInstanceOf(ZoneAlreadyLiftedException.class);
    }

    @Test
    void liftFailsForAnUnknownZone() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.lift(id, DISPATCHER))
                .isInstanceOf(ZoneNotFoundException.class);
    }

    @Test
    void listActiveReturnsOnlyWhatTheRepositoryCallsActive() {
        when(repository.findByLiftedAtIsNullOrderByCreatedAtDesc()).thenReturn(List.of(zone()));

        assertThat(service.listActive()).extracting(ZoneSummary::name).containsExactly("Riverside");
    }

    private static Zone zone() {
        return new Zone("Riverside", ZoneType.EVACUATION, GeoShapes.polygon(TRIANGLE), DISPATCHER);
    }
}
