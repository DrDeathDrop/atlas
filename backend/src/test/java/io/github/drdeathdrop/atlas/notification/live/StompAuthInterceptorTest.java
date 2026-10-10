package io.github.drdeathdrop.atlas.notification.live;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.security.Principal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StompAuthInterceptorTest {

    private static final String USER_ID = UUID.randomUUID().toString();

    @Mock
    private JwtDecoder jwtDecoder;

    private StompAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new StompAuthInterceptor(jwtDecoder, new JwtAuthenticationConverter());
    }

    @Test
    void connectingWithAValidTokenIdentifiesTheUser() {
        when(jwtDecoder.decode("good")).thenReturn(Jwt.withTokenValue("good")
                .header("alg", "HS256")
                .subject(USER_ID)
                .build());
        Message<byte[]> connect = connect("Bearer good");

        interceptor.preSend(connect, null);

        Principal user = MessageHeaderAccessor.getAccessor(connect, StompHeaderAccessor.class).getUser();
        assertThat(user).isNotNull();
        assertThat(user.getName()).isEqualTo(USER_ID);
    }

    @Test
    void connectingWithoutATokenIsRefused() {
        assertThatThrownBy(() -> interceptor.preSend(connect(null), null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void connectingWithAnInvalidTokenIsRefused() {
        when(jwtDecoder.decode("bad")).thenThrow(new BadJwtException("expired"));

        assertThatThrownBy(() -> interceptor.preSend(connect("Bearer bad"), null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void aConnectedUserMaySubscribeToTheUpdatesTopic() {
        Message<byte[]> subscribe = message(StompCommand.SUBSCRIBE, LiveUpdateBroadcaster.TOPIC, true);

        assertThatCode(() -> interceptor.preSend(subscribe, null)).doesNotThrowAnyException();
    }

    @Test
    void subscribingWithoutConnectingFirstIsRefused() {
        Message<byte[]> subscribe = message(StompCommand.SUBSCRIBE, LiveUpdateBroadcaster.TOPIC, false);

        assertThatThrownBy(() -> interceptor.preSend(subscribe, null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void subscribingToAnythingElseIsRefused() {
        Message<byte[]> subscribe = message(StompCommand.SUBSCRIBE, "/topic/other", true);

        assertThatThrownBy(() -> interceptor.preSend(subscribe, null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void clientsCannotSendMessages() {
        Message<byte[]> send = message(StompCommand.SEND, LiveUpdateBroadcaster.TOPIC, true);

        assertThatThrownBy(() -> interceptor.preSend(send, null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    private static Message<byte[]> connect(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static Message<byte[]> message(StompCommand command, String destination, boolean connected) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        if (connected) {
            accessor.setUser(new TestingAuthenticationToken(USER_ID, null));
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
