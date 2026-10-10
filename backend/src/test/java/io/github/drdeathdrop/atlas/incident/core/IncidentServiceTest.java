package io.github.drdeathdrop.atlas.incident.core;

import io.github.drdeathdrop.atlas.incident.IncidentCategory;
import io.github.drdeathdrop.atlas.incident.IncidentReported;
import io.github.drdeathdrop.atlas.incident.IncidentStatus;
import io.github.drdeathdrop.atlas.incident.IncidentStatusChanged;
import io.github.drdeathdrop.atlas.incident.IncidentSummary;
import io.github.drdeathdrop.atlas.incident.Severity;
import io.github.drdeathdrop.atlas.incident.lifecycle.IncidentLifecycle;
import io.github.drdeathdrop.atlas.incident.lifecycle.InvalidTransitionException;
import io.github.drdeathdrop.atlas.incident.lifecycle.TransitionNotPermittedException;
import io.github.drdeathdrop.atlas.shared.geo.GeoPoints;
import io.github.drdeathdrop.atlas.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    private static final double PLOVDIV_LATITUDE = 42.1354;
    private static final double PLOVDIV_LONGITUDE = 24.7453;

    @Mock
    private IncidentRepository repository;

    @Mock
    private ApplicationEventPublisher events;

    private IncidentService service;

    @BeforeEach
    void setUp() {
        service = new IncidentService(repository, new IncidentLifecycle(), events);
    }

    @Test
    void reportSavesANewIncidentInTheReportedState() {
        UUID reporter = UUID.randomUUID();
        when(repository.nextReferenceNumber()).thenReturn(42L);
        when(repository.saveAndFlush(any(Incident.class))).thenAnswer(call -> call.getArgument(0));

        IncidentSummary result = service.report(floodRequest(), reporter, Role.DISPATCHER);

        ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
        verify(repository).saveAndFlush(captor.capture());
        Incident saved = captor.getValue();

        assertThat(saved.getReference()).matches("INC-\\d{4}-0042");
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.REPORTED);
        assertThat(saved.getTitle()).isEqualTo("Flood in Plovdiv");
        assertThat(saved.getCategory()).isEqualTo(IncidentCategory.FLOOD);
        assertThat(saved.getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(saved.getAffectedPeople()).isEqualTo(25000);
        assertThat(saved.getReportedBy()).isEqualTo(reporter);
        assertThat(GeoPoints.latitude(saved.getLocation())).isCloseTo(PLOVDIV_LATITUDE, within(0.000001));
        assertThat(GeoPoints.longitude(saved.getLocation())).isCloseTo(PLOVDIV_LONGITUDE, within(0.000001));

        assertThat(result.reference()).isEqualTo(saved.getReference());
        assertThat(result.status()).isEqualTo(IncidentStatus.REPORTED);
        assertThat(result.latitude()).isCloseTo(PLOVDIV_LATITUDE, within(0.000001));
        assertThat(result.longitude()).isCloseTo(PLOVDIV_LONGITUDE, within(0.000001));
    }

    @Test
    void reportAnnouncesTheNewIncident() {
        UUID reporter = UUID.randomUUID();
        when(repository.nextReferenceNumber()).thenReturn(7L);
        when(repository.saveAndFlush(any(Incident.class))).thenAnswer(call -> call.getArgument(0));

        IncidentSummary result = service.report(floodRequest(), reporter, Role.DISPATCHER);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(IncidentReported.class);

        IncidentReported event = (IncidentReported) captor.getValue();
        assertThat(event.reference()).isEqualTo(result.reference());
        assertThat(event.reportedBy()).isEqualTo(reporter);
        assertThat(event.reporterRole()).isEqualTo(Role.DISPATCHER);
    }

    @Test
    void changeStatusMovesAnIncidentAlongAnAllowedPath() {
        UUID incidentId = UUID.randomUUID();
        UUID dispatcher = UUID.randomUUID();
        Incident incident = incidentIn(IncidentStatus.REPORTED);
        when(repository.findById(incidentId)).thenReturn(Optional.of(incident));

        IncidentSummary result = service.changeStatus(incidentId, IncidentStatus.VERIFIED, dispatcher, Role.DISPATCHER);

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.VERIFIED);
        assertThat(result.status()).isEqualTo(IncidentStatus.VERIFIED);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(IncidentStatusChanged.class);

        IncidentStatusChanged event = (IncidentStatusChanged) captor.getValue();
        assertThat(event.from()).isEqualTo(IncidentStatus.REPORTED);
        assertThat(event.to()).isEqualTo(IncidentStatus.VERIFIED);
        assertThat(event.changedBy()).isEqualTo(dispatcher);
        assertThat(event.changedByRole()).isEqualTo(Role.DISPATCHER);
        assertThat(event.reference()).isEqualTo(incident.getReference());
    }

    @Test
    void changeStatusLetsAFieldOperatorContainAnActiveIncident() {
        UUID incidentId = UUID.randomUUID();
        Incident incident = incidentIn(IncidentStatus.ACTIVE);
        when(repository.findById(incidentId)).thenReturn(Optional.of(incident));

        service.changeStatus(incidentId, IncidentStatus.CONTAINED, UUID.randomUUID(), Role.FIELD_OPERATOR);

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.CONTAINED);
    }

    @Test
    void changeStatusRefusesAForbiddenMoveAndChangesNothing() {
        UUID incidentId = UUID.randomUUID();
        Incident incident = incidentIn(IncidentStatus.REPORTED);
        when(repository.findById(incidentId)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() ->
                service.changeStatus(incidentId, IncidentStatus.ACTIVE, UUID.randomUUID(), Role.DISPATCHER))
                .isInstanceOf(InvalidTransitionException.class);

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.REPORTED);
        verifyNoInteractions(events);
    }

    @Test
    void changeStatusRefusesAnAllowedMoveByTheWrongRoleAndChangesNothing() {
        UUID incidentId = UUID.randomUUID();
        Incident incident = incidentIn(IncidentStatus.REPORTED);
        when(repository.findById(incidentId)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() ->
                service.changeStatus(incidentId, IncidentStatus.VERIFIED, UUID.randomUUID(), Role.VIEWER))
                .isInstanceOf(TransitionNotPermittedException.class);

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.REPORTED);
        verifyNoInteractions(events);
    }

    @Test
    void changeStatusFailsForAnUnknownIncident() {
        UUID incidentId = UUID.randomUUID();
        when(repository.findById(incidentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.changeStatus(incidentId, IncidentStatus.VERIFIED, UUID.randomUUID(), Role.DISPATCHER))
                .isInstanceOf(IncidentNotFoundException.class);

        verifyNoInteractions(events);
    }

    @Test
    void allowedTransitionsListsOnlyWhatTheRoleMayDo() {
        UUID incidentId = UUID.randomUUID();
        when(repository.findById(incidentId)).thenReturn(Optional.of(incidentIn(IncidentStatus.CONTAINED)));

        assertThat(service.allowedTransitions(incidentId, Role.DISPATCHER))
                .containsExactly(IncidentStatus.ACTIVE, IncidentStatus.RESOLVED);
        assertThat(service.allowedTransitions(incidentId, Role.FIELD_OPERATOR)).isEmpty();
        assertThat(service.allowedTransitions(incidentId, Role.VIEWER)).isEmpty();
    }

    @Test
    void allowedTransitionsGivesAFieldOperatorTheirSingleMove() {
        UUID incidentId = UUID.randomUUID();
        when(repository.findById(incidentId)).thenReturn(Optional.of(incidentIn(IncidentStatus.ACTIVE)));

        assertThat(service.allowedTransitions(incidentId, Role.FIELD_OPERATOR))
                .containsExactly(IncidentStatus.CONTAINED);
    }

    private static ReportIncidentRequest floodRequest() {
        return new ReportIncidentRequest(
                "Flood in Plovdiv",
                "The Maritsa has burst its banks near the centre.",
                IncidentCategory.FLOOD,
                Severity.CRITICAL,
                PLOVDIV_LATITUDE,
                PLOVDIV_LONGITUDE,
                25000);
    }

    private static Incident incidentIn(IncidentStatus status) {
        Incident incident = new Incident(
                "INC-2026-0001",
                "Flood in Plovdiv",
                "The Maritsa has burst its banks near the centre.",
                IncidentCategory.FLOOD,
                Severity.CRITICAL,
                GeoPoints.of(PLOVDIV_LATITUDE, PLOVDIV_LONGITUDE),
                25000,
                UUID.randomUUID());
        if (status != IncidentStatus.REPORTED) {
            incident.moveTo(status);
        }
        return incident;
    }
}
