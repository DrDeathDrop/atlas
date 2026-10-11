package io.github.drdeathdrop.atlas.notification.live;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import static org.assertj.core.api.Assertions.assertThat;

class LiveSessionMetricsTest {

    private final MeterRegistry registry = new SimpleMeterRegistry();
    private final LiveSessionMetrics metrics = new LiveSessionMetrics(registry);

    @Test
    void connectedSessionsAreCounted() {
        metrics.on(new SessionConnectedEvent(this, message(SimpMessageType.CONNECT_ACK, "one")));
        metrics.on(new SessionConnectedEvent(this, message(SimpMessageType.CONNECT_ACK, "two")));

        assertThat(openSessions()).isEqualTo(2.0);
    }

    @Test
    void aDisconnectReportedTwiceIsCountedOnce() {
        metrics.on(new SessionConnectedEvent(this, message(SimpMessageType.CONNECT_ACK, "one")));
        metrics.on(new SessionConnectedEvent(this, message(SimpMessageType.CONNECT_ACK, "two")));

        metrics.on(disconnect("one"));
        metrics.on(disconnect("one"));

        assertThat(openSessions()).isEqualTo(1.0);
    }

    private double openSessions() {
        return registry.get(LiveSessionMetrics.SESSIONS).gauge().value();
    }

    private SessionDisconnectEvent disconnect(String sessionId) {
        return new SessionDisconnectEvent(this, message(SimpMessageType.DISCONNECT, sessionId), sessionId,
                CloseStatus.NORMAL);
    }

    private static Message<byte[]> message(SimpMessageType type, String sessionId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(type);
        accessor.setSessionId(sessionId);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
