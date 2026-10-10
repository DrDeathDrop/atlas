package io.github.drdeathdrop.atlas.messaging.broker;

import io.github.drdeathdrop.atlas.messaging.EventChannels;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopology {
    @Bean
    TopicExchange eventsExchange() {
        return new TopicExchange(EventChannels.EXCHANGE, true, false);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(EventChannels.DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue notificationsQueue() {
        return QueueBuilder.durable(EventChannels.NOTIFICATIONS_QUEUE)
                .deadLetterExchange(EventChannels.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(EventChannels.NOTIFICATIONS_QUEUE)
                .build();
    }

    @Bean
    Queue notificationsDeadLetterQueue() {
        return QueueBuilder.durable(EventChannels.NOTIFICATIONS_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding incidentEventsToNotifications(Queue notificationsQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(notificationsQueue).to(eventsExchange).with("incident.*");
    }

    @Bean
    Binding resourceEventsToNotifications(Queue notificationsQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(notificationsQueue).to(eventsExchange).with("resource.*");
    }

    @Bean
    Binding deadNotifications(Queue notificationsDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(notificationsDeadLetterQueue)
                .to(deadLetterExchange)
                .with(EventChannels.NOTIFICATIONS_QUEUE);
    }
}
