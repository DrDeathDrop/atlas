package io.github.drdeathdrop.atlas.resource.allocation;

import io.github.drdeathdrop.atlas.resource.AssignmentSummary;
import io.github.drdeathdrop.atlas.resource.ResourceAllocation;
import io.github.drdeathdrop.atlas.user.Role;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {
    private final ResourceAllocation resourceAllocation;

    public AssignmentController(ResourceAllocation resourceAllocation) {
        this.resourceAllocation = resourceAllocation;
    }

    @PostMapping("/{assignmentId}/release")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public AssignmentSummary release(@PathVariable UUID assignmentId, @AuthenticationPrincipal Jwt jwt) {
        return resourceAllocation.release(
                assignmentId,
                UUID.fromString(jwt.getSubject()),
                Role.valueOf(jwt.getClaimAsString("role")));
    }
}
