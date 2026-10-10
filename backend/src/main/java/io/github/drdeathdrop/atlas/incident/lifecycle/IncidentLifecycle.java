package io.github.drdeathdrop.atlas.incident.lifecycle;

import io.github.drdeathdrop.atlas.incident.IncidentStatus;
import io.github.drdeathdrop.atlas.user.Role;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class IncidentLifecycle {
    private static final Map<IncidentStatus, Set<IncidentStatus>> ALLOWED = Map.of(
            IncidentStatus.REPORTED, Set.of(IncidentStatus.VERIFIED, IncidentStatus.REJECTED),
            IncidentStatus.VERIFIED, Set.of(IncidentStatus.ACTIVE),
            IncidentStatus.ACTIVE, Set.of(IncidentStatus.CONTAINED),
            IncidentStatus.CONTAINED, Set.of(IncidentStatus.RESOLVED, IncidentStatus.ACTIVE),
            IncidentStatus.RESOLVED, Set.of(IncidentStatus.ARCHIVED, IncidentStatus.ACTIVE),
            IncidentStatus.REJECTED, Set.of(IncidentStatus.ARCHIVED),
            IncidentStatus.ARCHIVED, Set.of()
    );

    public Set<IncidentStatus> allowedNext(IncidentStatus from) {
        return ALLOWED.get(from);
    }

    public boolean isAllowed(IncidentStatus from, IncidentStatus to) {
        return allowedNext(from).contains(to);
    }

    public boolean isPermitted(Role role, IncidentStatus from, IncidentStatus to) {
        if (!isAllowed(from, to)) {
            return false;
        }

        return switch (role) {
            case ADMIN, DISPATCHER -> true;
            case FIELD_OPERATOR -> from == IncidentStatus.ACTIVE && to == IncidentStatus.CONTAINED;
            case ANALYST, VIEWER -> false;
        };
    }

    public void validate(Role role, IncidentStatus from, IncidentStatus to) {
        if (!isAllowed(from, to)) {
            throw new InvalidTransitionException(from, to);
        }
        if (!isPermitted(role, from, to)) {
            throw new TransitionNotPermittedException(role, from, to);
        }
    }
}
