package io.github.drdeathdrop.atlas.zone.closure;

import io.github.drdeathdrop.atlas.zone.RoadClosureSummary;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/road-closures")
public class RoadClosureController {
    private final RoadClosureService roadClosureService;

    public RoadClosureController(RoadClosureService roadClosureService) {
        this.roadClosureService = roadClosureService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public RoadClosureSummary create(@Valid @RequestBody CreateRoadClosureRequest request,
                                     @AuthenticationPrincipal Jwt jwt) {
        return roadClosureService.create(request, UUID.fromString(jwt.getSubject()));
    }

    @GetMapping
    public List<RoadClosureSummary> listActive() {
        return roadClosureService.listActive();
    }

    @PostMapping("/{closureId}/reopen")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public RoadClosureSummary reopen(@PathVariable UUID closureId, @AuthenticationPrincipal Jwt jwt) {
        return roadClosureService.reopen(closureId, UUID.fromString(jwt.getSubject()));
    }
}
