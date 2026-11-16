package vn.cozynest.listing.infrastructure.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.cozynest.common.events.EventTopology;

@Configuration
public class ListingRabbitConfig {

    @Bean
    public TopicExchange listingEventsExchange() {
        return new TopicExchange(EventTopology.EVENTS_EXCHANGE, true, false);
    }
}
