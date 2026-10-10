package io.github.drdeathdrop.atlas.zone.closure;

import io.github.drdeathdrop.atlas.shared.geo.GeoPosition;
import io.github.drdeathdrop.atlas.shared.geo.GeoShapes;
import io.github.drdeathdrop.atlas.shared.geo.InvalidShapeException;
import io.github.drdeathdrop.atlas.zone.RoadClosureSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class RoadClosureServiceTest {

    private static final List<GeoPosition> PATH = List.of(
            new GeoPosition(42.14, 24.74),
            new GeoPosition(42.15, 24.75),
            new GeoPosition(42.16, 24.75));

    private static final UUID DISPATCHER = UUID.randomUUID();

    @Mock
    private RoadClosureRepository repository;

    @InjectMocks
    private RoadClosureService service;

    @Test
    void createStoresTheClosureWithItsPath() {
        when(repository.saveAndFlush(any(RoadClosure.class))).thenAnswer(call -> call.getArgument(0));

        RoadClosureSummary result = service.create(
                new CreateRoadClosureRequest(" bul. Maritsa ", " Flooded underpass ", PATH), DISPATCHER);

        assertThat(result.roadName()).isEqualTo("bul. Maritsa");
        assertThat(result.reason()).isEqualTo("Flooded underpass");
        assertThat(result.path()).containsExactlyElementsOf(PATH);
        assertThat(result.reopenedAt()).isNull();
    }

    @Test
    void createTreatsABlankReasonAsNoReason() {
        when(repository.saveAndFlush(any(RoadClosure.class))).thenAnswer(call -> call.getArgument(0));

        RoadClosureSummary result = service.create(
                new CreateRoadClosureRequest("bul. Maritsa", "  ", PATH), DISPATCHER);

        assertThat(result.reason()).isNull();
    }

    @Test
    void createRejectsAPathWithASinglePoint() {
        assertThatThrownBy(() -> service.create(
                new CreateRoadClosureRequest("bul. Maritsa", null, PATH.subList(0, 1)), DISPATCHER))
                .isInstanceOf(InvalidShapeException.class);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void reopenRecordsWhoReopenedTheRoadAndWhen() {
        UUID id = UUID.randomUUID();
        RoadClosure closure = closure();
        when(repository.findById(id)).thenReturn(Optional.of(closure));

        RoadClosureSummary result = service.reopen(id, DISPATCHER);

        assertThat(result.reopenedAt()).isNotNull();
        assertThat(closure.getReopenedBy()).isEqualTo(DISPATCHER);
    }

    @Test
    void reopenRejectsARoadThatIsAlreadyOpen() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(closure()));
        service.reopen(id, DISPATCHER);

        assertThatThrownBy(() -> service.reopen(id, DISPATCHER))
                .isInstanceOf(RoadAlreadyReopenedException.class);
    }

    @Test
    void reopenFailsForAnUnknownClosure() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reopen(id, DISPATCHER))
                .isInstanceOf(RoadClosureNotFoundException.class);
    }

    private static RoadClosure closure() {
        return new RoadClosure("bul. Maritsa", null, GeoShapes.line(PATH), DISPATCHER);
    }
}
