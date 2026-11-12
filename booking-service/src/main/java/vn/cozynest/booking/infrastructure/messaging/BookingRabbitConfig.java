package vn.cozynest.booking.infrastructure.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.cozynest.common.events.EventTopology;

@Configuration
public class BookingRabbitConfig {

    @Bean
    public TopicExchange bookingEventsExchange() {
        return new TopicExchange(EventTopology.EVENTS_EXCHANGE, true, false);
    }
}

