package io.github.drdeathdrop.atlas.resource.allocation;

import io.github.drdeathdrop.atlas.resource.AssignmentSummary;
import io.github.drdeathdrop.atlas.resource.ResourceAssigned;
import io.github.drdeathdrop.atlas.resource.ResourceReleased;
import io.github.drdeathdrop.atlas.resource.ResourceStatus;
import io.github.drdeathdrop.atlas.resource.ResourceType;
import io.github.drdeathdrop.atlas.resource.inventory.Resource;
import io.github.drdeathdrop.atlas.resource.inventory.ResourceNotFoundException;
import io.github.drdeathdrop.atlas.resource.inventory.ResourceRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceAllocationServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private ApplicationEventPublisher events;

    private ResourceAllocationService service;

    @BeforeEach
    void setUp() {
        service = new ResourceAllocationService(resourceRepository, assignmentRepository, events);
    }

    @Test
    void assignSendsAnAvailableResourceToTheIncident() {
        UUID resourceId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID dispatcher = UUID.randomUUID();
        Resource resource = ambulance();
        when(resourceRepository.findByIdForUpdate(resourceId)).thenReturn(Optional.of(resource));
        when(assignmentRepository.saveAndFlush(any(Assignment.class))).thenAnswer(call -> call.getArgument(0));

        AssignmentSummary result = service.assign(resourceId, incidentId, dispatcher, Role.DISPATCHER);

        assertThat(resource.getStatus()).isEqualTo(ResourceStatus.EN_ROUTE);
        assertThat(result.resourceId()).isEqualTo(resourceId);
        assertThat(result.incidentId()).isEqualTo(incidentId);
        assertThat(result.callSign()).isEqualTo("AMBULANCE-17");
        assertThat(result.assignedAt()).isNotNull();
        assertThat(result.releasedAt()).isNull();

        ArgumentCaptor<Assignment> saved = ArgumentCaptor.forClass(Assignment.class);
        verify(assignmentRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getResourceId()).isEqualTo(resourceId);
        assertThat(saved.getValue().getIncidentId()).isEqualTo(incidentId);
        assertThat(saved.getValue().getAssignedBy()).isEqualTo(dispatcher);

        ArgumentCaptor<Object> published = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(published.capture());
        assertThat(published.getValue()).isInstanceOf(ResourceAssigned.class);
        ResourceAssigned event = (ResourceAssigned) published.getValue();
        assertThat(event.resourceId()).isEqualTo(resourceId);
        assertThat(event.callSign()).isEqualTo("AMBULANCE-17");
        assertThat(event.incidentId()).isEqualTo(incidentId);
        assertThat(event.assignedBy()).isEqualTo(dispatcher);
        assertThat(event.assignedByRole()).isEqualTo(Role.DISPATCHER);
    }

    @Test
    void assignRefusesAResourceThatIsNotAvailable() {
        UUID resourceId = UUID.randomUUID();
        Resource resource = ambulance();
        resource.changeStatus(ResourceStatus.ON_SCENE);
        when(resourceRepository.findByIdForUpdate(resourceId)).thenReturn(Optional.of(resource));

        assertThatThrownBy(() -> service.assign(resourceId, UUID.randomUUID(), UUID.randomUUID(), Role.DISPATCHER))
                .isInstanceOf(ResourceNotAvailableException.class)
                .hasMessageContaining("AMBULANCE-17");

        assertThat(resource.getStatus()).isEqualTo(ResourceStatus.ON_SCENE);
        verify(assignmentRepository, never()).saveAndFlush(any());
        verifyNoInteractions(events);
    }

    @Test
    void assignFailsForAnUnknownResource() {
        UUID resourceId = UUID.randomUUID();
        when(resourceRepository.findByIdForUpdate(resourceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assign(resourceId, UUID.randomUUID(), UUID.randomUUID(), Role.DISPATCHER))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(events);
    }

    @Test
    void releaseMakesTheResourceAvailableAgain() {
        UUID assignmentId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID dispatcher = UUID.randomUUID();
        Assignment assignment = new Assignment(resourceId, incidentId, UUID.randomUUID());
        Resource resource = ambulance();
        resource.changeStatus(ResourceStatus.ON_SCENE);
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(resourceRepository.findByIdForUpdate(resourceId)).thenReturn(Optional.of(resource));

        AssignmentSummary result = service.release(assignmentId, dispatcher, Role.DISPATCHER);

        assertThat(resource.getStatus()).isEqualTo(ResourceStatus.AVAILABLE);
        assertThat(assignment.isOpen()).isFalse();
        assertThat(assignment.getReleasedBy()).isEqualTo(dispatcher);
        assertThat(result.releasedAt()).isNotNull();
        assertThat(result.callSign()).isEqualTo("AMBULANCE-17");

        ArgumentCaptor<Object> published = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(published.capture());
        assertThat(published.getValue()).isInstanceOf(ResourceReleased.class);
        ResourceReleased event = (ResourceReleased) published.getValue();
        assertThat(event.resourceId()).isEqualTo(resourceId);
        assertThat(event.incidentId()).isEqualTo(incidentId);
        assertThat(event.previousStatus()).isEqualTo(ResourceStatus.ON_SCENE);
        assertThat(event.releasedBy()).isEqualTo(dispatcher);
        assertThat(event.releasedByRole()).isEqualTo(Role.DISPATCHER);
    }

    @Test
    void releaseRefusesAnAssignmentThatWasAlreadyReleased() {
        UUID assignmentId = UUID.randomUUID();
        Assignment assignment = new Assignment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        assignment.release(UUID.randomUUID());
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> service.release(assignmentId, UUID.randomUUID(), Role.DISPATCHER))
                .isInstanceOf(AssignmentAlreadyReleasedException.class);

        verifyNoInteractions(events);
        verifyNoInteractions(resourceRepository);
    }

    @Test
    void releaseFailsForAnUnknownAssignment() {
        UUID assignmentId = UUID.randomUUID();
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.release(assignmentId, UUID.randomUUID(), Role.DISPATCHER))
                .isInstanceOf(AssignmentNotFoundException.class);

        verifyNoInteractions(events);
    }

    private static Resource ambulance() {
        return new Resource("AMBULANCE-17", ResourceType.AMBULANCE, GeoPoints.of(42.1354, 24.7453), null);
    }
}
