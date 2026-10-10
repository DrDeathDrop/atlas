package io.github.drdeathdrop.atlas.notification.live;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

@Component
public class StompAuthInterceptor implements ChannelInterceptor {
    private static final String BEARER = "Bearer ";

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter authenticationConverter;

    public StompAuthInterceptor(JwtDecoder jwtDecoder, JwtAuthenticationConverter authenticationConverter) {
        this.jwtDecoder = jwtDecoder;
        this.authenticationConverter = authenticationConverter;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (command == StompCommand.CONNECT || command == StompCommand.STOMP) {
            accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
        } else if (command == StompCommand.SUBSCRIBE) {
            if (accessor.getUser() == null) {
                throw new MessageDeliveryException("Connect with an access token before subscribing");
            }
            if (!LiveUpdateBroadcaster.TOPIC.equals(accessor.getDestination())) {
                throw new MessageDeliveryException("Unknown destination");
            }
        } else if (command == StompCommand.SEND) {
            throw new MessageDeliveryException("This connection only delivers updates");
        }

        return message;
    }

    private Authentication authenticate(String header) {
        if (header == null || !header.startsWith(BEARER)) {
            throw new MessageDeliveryException("An access token is required");
        }

        try {
            return authenticationConverter.convert(jwtDecoder.decode(header.substring(BEARER.length())));
        } catch (JwtException exception) {
            throw new MessageDeliveryException("The access token is not valid");
        }
    }
}
