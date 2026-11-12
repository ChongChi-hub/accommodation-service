package vn.cozynest.communication.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.cozynest.common.events.EventTopology;

@Configuration
public class CommunicationRabbitConfig {

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EventTopology.EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(EventTopology.DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue communicationEventsQueue() {
        return QueueBuilder.durable(EventTopology.COMMUNICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", EventTopology.DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", EventTopology.DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(EventTopology.DLQ_QUEUE).build();
    }

    @Bean
    public Binding bookingEventsBinding(Queue communicationEventsQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(communicationEventsQueue).to(eventsExchange).with("booking.#");
    }

    @Bean
    public Binding billingEventsBinding(Queue communicationEventsQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(communicationEventsQueue).to(eventsExchange).with("billing.#");
    }

    @Bean
    public Binding listingEventsBinding(Queue communicationEventsQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(communicationEventsQueue).to(eventsExchange).with("listing.#");
    }

    @Bean
    public Binding dlqBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(EventTopology.DLQ_ROUTING_KEY);
    }
}

