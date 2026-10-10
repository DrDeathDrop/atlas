package io.github.drdeathdrop.atlas.zone.area;

import io.github.drdeathdrop.atlas.zone.ZoneSummary;
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
@RequestMapping("/api/zones")
public class ZoneController {
    private final ZoneService zoneService;

    public ZoneController(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ZoneSummary create(@Valid @RequestBody CreateZoneRequest request, @AuthenticationPrincipal Jwt jwt) {
        return zoneService.create(request, UUID.fromString(jwt.getSubject()));
    }

    @GetMapping
    public List<ZoneSummary> listActive() {
        return zoneService.listActive();
    }

    @PostMapping("/{zoneId}/lift")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ZoneSummary lift(@PathVariable UUID zoneId, @AuthenticationPrincipal Jwt jwt) {
        return zoneService.lift(zoneId, UUID.fromString(jwt.getSubject()));
    }
}
