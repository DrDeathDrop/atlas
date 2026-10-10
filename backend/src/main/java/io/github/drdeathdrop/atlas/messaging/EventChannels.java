package io.github.drdeathdrop.atlas.messaging;

public final class EventChannels {
    public static final String EXCHANGE = "atlas.events";
    public static final String DEAD_LETTER_EXCHANGE = "atlas.events.dead";
    public static final String NOTIFICATIONS_QUEUE = "atlas.notifications";
    public static final String NOTIFICATIONS_DEAD_LETTER_QUEUE = "atlas.notifications.dead";

    private EventChannels() {
    }
}
