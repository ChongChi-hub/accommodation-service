package vn.cozynest.identity.infrastructure.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.cozynest.common.events.EventTopology;

@Configuration
public class IdentityRabbitConfig {

    @Bean
    public TopicExchange identityEventsExchange() {
        return new TopicExchange(EventTopology.EVENTS_EXCHANGE, true, false);
    }
}
