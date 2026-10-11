package io.github.drdeathdrop.atlas.simulation.engine;

import io.github.drdeathdrop.atlas.simulation.SimulationSummary;
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
@RequestMapping("/api/simulations")
@PreAuthorize("hasRole('ADMIN')")
public class SimulationController {
    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SimulationSummary start(@Valid @RequestBody StartSimulationRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        return simulationService.start(request, UUID.fromString(jwt.getSubject()));
    }

    @GetMapping
    public List<SimulationSummary> latest() {
        return simulationService.latest();
    }

    @PostMapping("/{runId}/stop")
    public SimulationSummary stop(@PathVariable UUID runId) {
        return simulationService.stop(runId);
    }

    @PostMapping("/{runId}/resume")
    public SimulationSummary resume(@PathVariable UUID runId) {
        return simulationService.resume(runId);
    }
}
