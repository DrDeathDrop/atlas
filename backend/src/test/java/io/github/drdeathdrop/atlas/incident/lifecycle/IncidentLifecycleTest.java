package io.github.drdeathdrop.atlas.incident.lifecycle;

import io.github.drdeathdrop.atlas.incident.IncidentStatus;
import io.github.drdeathdrop.atlas.user.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static io.github.drdeathdrop.atlas.incident.IncidentStatus.ACTIVE;
import static io.github.drdeathdrop.atlas.incident.IncidentStatus.ARCHIVED;
import static io.github.drdeathdrop.atlas.incident.IncidentStatus.CONTAINED;
import static io.github.drdeathdrop.atlas.incident.IncidentStatus.REJECTED;
import static io.github.drdeathdrop.atlas.incident.IncidentStatus.REPORTED;
import static io.github.drdeathdrop.atlas.incident.IncidentStatus.RESOLVED;
import static io.github.drdeathdrop.atlas.incident.IncidentStatus.VERIFIED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncidentLifecycleTest {

    private static final List<IncidentStatus[]> ALLOWED = List.of(
            new IncidentStatus[]{REPORTED, VERIFIED},
            new IncidentStatus[]{REPORTED, REJECTED},
            new IncidentStatus[]{VERIFIED, ACTIVE},
            new IncidentStatus[]{ACTIVE, CONTAINED},
            new IncidentStatus[]{CONTAINED, RESOLVED},
            new IncidentStatus[]{CONTAINED, ACTIVE},
            new IncidentStatus[]{RESOLVED, ARCHIVED},
            new IncidentStatus[]{RESOLVED, ACTIVE},
            new IncidentStatus[]{REJECTED, ARCHIVED});

    private final IncidentLifecycle lifecycle = new IncidentLifecycle();

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @MethodSource("allowedTransitions")
    void allowedTransitionsAreAccepted(IncidentStatus from, IncidentStatus to) {
        assertThat(lifecycle.isAllowed(from, to)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} is refused")
    @MethodSource("forbiddenTransitions")
    void everyOtherTransitionIsRefused(IncidentStatus from, IncidentStatus to) {
        assertThat(lifecycle.isAllowed(from, to)).isFalse();
    }

    @Test
    void allowedNextListsExactlyTheNextStates() {
        assertThat(lifecycle.allowedNext(REPORTED)).containsExactlyInAnyOrder(VERIFIED, REJECTED);
        assertThat(lifecycle.allowedNext(VERIFIED)).containsExactlyInAnyOrder(ACTIVE);
        assertThat(lifecycle.allowedNext(ACTIVE)).containsExactlyInAnyOrder(CONTAINED);
        assertThat(lifecycle.allowedNext(CONTAINED)).containsExactlyInAnyOrder(RESOLVED, ACTIVE);
        assertThat(lifecycle.allowedNext(RESOLVED)).containsExactlyInAnyOrder(ARCHIVED, ACTIVE);
        assertThat(lifecycle.allowedNext(REJECTED)).containsExactlyInAnyOrder(ARCHIVED);
    }

    @Test
    void archivedIsFinal() {
        assertThat(lifecycle.allowedNext(ARCHIVED)).isEmpty();
    }

    @ParameterizedTest(name = "{0} may move {1} -> {2}")
    @MethodSource("allowedTransitionsForDispatcherAndAdmin")
    void dispatcherAndAdminMayMakeEveryAllowedMove(Role role, IncidentStatus from, IncidentStatus to) {
        assertThat(lifecycle.isPermitted(role, from, to)).isTrue();
    }

    @Test
    void fieldOperatorMayOnlyMarkAnActiveIncidentAsContained() {
        for (IncidentStatus[] move : ALLOWED) {
            boolean expected = move[0] == ACTIVE && move[1] == CONTAINED;

            assertThat(lifecycle.isPermitted(Role.FIELD_OPERATOR, move[0], move[1]))
                    .as("%s -> %s", move[0], move[1])
                    .isEqualTo(expected);
        }
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ANALYST", "VIEWER"})
    void analystAndViewerMayChangeNothing(Role role) {
        for (IncidentStatus[] move : ALLOWED) {
            assertThat(lifecycle.isPermitted(role, move[0], move[1]))
                    .as("%s -> %s", move[0], move[1])
                    .isFalse();
        }
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void nobodyIsPermittedToMakeAForbiddenMove(Role role) {
        assertThat(lifecycle.isPermitted(role, ARCHIVED, REPORTED)).isFalse();
        assertThat(lifecycle.isPermitted(role, REPORTED, ACTIVE)).isFalse();
    }

    @Test
    void validateAcceptsAPermittedMove() {
        assertThatCode(() -> lifecycle.validate(Role.DISPATCHER, REPORTED, VERIFIED)).doesNotThrowAnyException();
        assertThatCode(() -> lifecycle.validate(Role.FIELD_OPERATOR, ACTIVE, CONTAINED)).doesNotThrowAnyException();
    }

    @Test
    void validateRejectsAForbiddenMove() {
        assertThatThrownBy(() -> lifecycle.validate(Role.DISPATCHER, REPORTED, ACTIVE))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("REPORTED")
                .hasMessageContaining("ACTIVE");
    }

    @Test
    void validateRejectsAnAllowedMoveByTheWrongRole() {
        assertThatThrownBy(() -> lifecycle.validate(Role.VIEWER, REPORTED, VERIFIED))
                .isInstanceOf(TransitionNotPermittedException.class);

        assertThatThrownBy(() -> lifecycle.validate(Role.FIELD_OPERATOR, CONTAINED, RESOLVED))
                .isInstanceOf(TransitionNotPermittedException.class);
    }

    @Test
    void aForbiddenMoveIsReportedAsForbiddenWhateverTheRole() {
        assertThatThrownBy(() -> lifecycle.validate(Role.VIEWER, ARCHIVED, REPORTED))
                .isInstanceOf(InvalidTransitionException.class);
    }

    static Stream<Arguments> allowedTransitions() {
        return ALLOWED.stream().map(move -> Arguments.of(move[0], move[1]));
    }

    static Stream<Arguments> forbiddenTransitions() {
        List<Arguments> forbidden = new ArrayList<>();
        for (IncidentStatus from : IncidentStatus.values()) {
            for (IncidentStatus to : IncidentStatus.values()) {
                boolean allowed = ALLOWED.stream().anyMatch(move -> move[0] == from && move[1] == to);
                if (!allowed) {
                    forbidden.add(Arguments.of(from, to));
                }
            }
        }
        return forbidden.stream();
    }

    static Stream<Arguments> allowedTransitionsForDispatcherAndAdmin() {
        return Stream.of(Role.DISPATCHER, Role.ADMIN)
                .flatMap(role -> ALLOWED.stream().map(move -> Arguments.of(role, move[0], move[1])));
    }
}
