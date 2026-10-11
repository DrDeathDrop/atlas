package io.github.drdeathdrop.atlas.notification.live;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LiveSessionMetrics {
    static final String SESSIONS = "atlas.live.sessions";

    private final Set<String> sessions = ConcurrentHashMap.newKeySet();

    public LiveSessionMetrics(MeterRegistry registry) {
        Gauge.builder(SESSIONS, sessions, Set::size)
                .description("Browser tabs connected for live updates")
                .register(registry);
    }

    @EventListener
    public void on(SessionConnectedEvent event) {
        String sessionId = SimpMessageHeaderAccessor.getSessionId(event.getMessage().getHeaders());
        if (sessionId != null) {
            sessions.add(sessionId);
        }
    }

    @EventListener
    public void on(SessionDisconnectEvent event) {
        sessions.remove(event.getSessionId());
    }
}
