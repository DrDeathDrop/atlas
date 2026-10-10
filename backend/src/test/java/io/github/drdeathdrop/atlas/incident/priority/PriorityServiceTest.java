package io.github.drdeathdrop.atlas.incident.priority;

import io.github.drdeathdrop.atlas.incident.IncidentCategory;
import io.github.drdeathdrop.atlas.incident.IncidentStatus;
import io.github.drdeathdrop.atlas.incident.Priority;
import io.github.drdeathdrop.atlas.incident.PriorityFactor;
import io.github.drdeathdrop.atlas.incident.Severity;
import io.github.drdeathdrop.atlas.incident.core.Incident;
import io.github.drdeathdrop.atlas.incident.core.IncidentNotFoundException;
import io.github.drdeathdrop.atlas.incident.core.IncidentRepository;
import io.github.drdeathdrop.atlas.resource.ResourceAllocation;
import io.github.drdeathdrop.atlas.shared.geo.GeoPoints;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriorityServiceTest {

    @Mock
    private IncidentRepository incidents;

    @Mock
    private ResourceAllocation resourceAllocation;

    private PriorityService service;

    @BeforeEach
    void setUp() {
        service = new PriorityService(incidents, resourceAllocation, new PriorityCalculator());
    }

    @Test
    void openIncidentsAreListedMostUrgentFirst() {
        Incident minor = incident(Severity.LOW, IncidentCategory.OTHER, 0);
        Incident major = incident(Severity.CRITICAL, IncidentCategory.EXPLOSION, 5000);
        Incident middle = incident(Severity.MEDIUM, IncidentCategory.FLOOD, 50);
        when(incidents.findByStatusIn(PriorityService.OPEN)).thenReturn(List.of(minor, major, middle));
        when(resourceAllocation.incidentsWithOpenAssignments())
                .thenReturn(Set.of(minor.getId(), major.getId(), middle.getId()));

        List<Priority> result = service.openIncidentsByPriority();

        assertThat(result).extracting(Priority::incidentId)
                .containsExactly(major.getId(), middle.getId(), minor.getId());
        assertThat(result).extracting(Priority::score).isSortedAccordingTo((a, b) -> b - a);
    }

    @Test
    void anIncidentWithoutResourcesIsMarkedUnattended() {
        Incident attended = incident(Severity.HIGH, IncidentCategory.FLOOD, 100);
        Incident unattended = incident(Severity.HIGH, IncidentCategory.FLOOD, 100);
        when(incidents.findByStatusIn(PriorityService.OPEN)).thenReturn(List.of(attended, unattended));
        when(resourceAllocation.incidentsWithOpenAssignments()).thenReturn(Set.of(attended.getId()));

        List<Priority> result = service.openIncidentsByPriority();

        assertThat(result.get(0).incidentId()).isEqualTo(unattended.getId());
        assertThat(result.get(0).factors()).extracting(PriorityFactor::name).contains("Unattended");
        assertThat(result.get(1).factors()).extracting(PriorityFactor::name).doesNotContain("Unattended");
    }

    @Test
    void thePriorityOfOneIncidentCanBeRead() {
        Incident incident = incident(Severity.HIGH, IncidentCategory.FLOOD, 100);
        when(incidents.findById(incident.getId())).thenReturn(Optional.of(incident));
        when(resourceAllocation.incidentsWithOpenAssignments()).thenReturn(Set.of(incident.getId()));

        Priority priority = service.forIncident(incident.getId());

        assertThat(priority.incidentId()).isEqualTo(incident.getId());
        assertThat(priority.score()).isEqualTo(30 + 10 + 10);
    }

    @Test
    void anUnknownIncidentHasNoPriority() {
        UUID unknown = UUID.randomUUID();
        when(incidents.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.forIncident(unknown))
                .isInstanceOf(IncidentNotFoundException.class);
    }

    private static Incident incident(Severity severity, IncidentCategory category, int people) {
        Incident incident = new Incident("INC-2026-0001", "Title", "Description", category, severity,
                GeoPoints.of(42.1354, 24.7453), people, UUID.randomUUID());
        ReflectionTestUtils.setField(incident, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(incident, "createdAt", Instant.now());
        incident.moveTo(IncidentStatus.ACTIVE);
        return incident;
    }
}
