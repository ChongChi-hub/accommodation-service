package vn.cozynest.admin.infrastructure.messaging;

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
public class AdminRabbitConfig {

    @Bean
    public TopicExchange adminEventsExchange() {
        return new TopicExchange(EventTopology.EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange adminDeadLetterExchange() {
        return new DirectExchange(EventTopology.DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue adminDeadLetterQueue() {
        return QueueBuilder.durable(EventTopology.DLQ_QUEUE).build();
    }

    @Bean
    public Binding adminDlqBinding(Queue adminDeadLetterQueue, DirectExchange adminDeadLetterExchange) {
        return BindingBuilder.bind(adminDeadLetterQueue).to(adminDeadLetterExchange).with(EventTopology.DLQ_ROUTING_KEY);
    }
}

