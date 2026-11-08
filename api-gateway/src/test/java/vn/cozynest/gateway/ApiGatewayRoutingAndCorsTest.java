package vn.cozynest.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.core.env.Environment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayRoutingAndCorsTest {

    @Autowired
    private GatewayProperties gatewayProperties;

    @Autowired
    private Environment env;

    @Test
    @DisplayName("Verify API Gateway routes are registered for all 6 microservices")
    void testGatewayRoutesRegistered() {
        java.util.List<String> routeIds = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String routeId = env.getProperty("spring.cloud.gateway.routes[" + i + "].id");
            if (routeId != null) {
                routeIds.add(routeId);
            }
        }

        assertThat(routeIds)
                .contains("identity-auth")
                .contains("listing-service")
                .contains("booking-service")
                .contains("billing-service")
                .contains("communication-service")
                .contains("admin-service");
    }

    @Test
    @DisplayName("Verify Global CORS configuration permits Frontend origin and credentials")
    void testCorsConfiguration() {
        String allowedOrigin0 = env.getProperty("spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-origins[0]");
        if (allowedOrigin0 == null) {
            allowedOrigin0 = env.getProperty("spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-origins");
        }
        String allowCredentials = env.getProperty("spring.cloud.gateway.globalcors.cors-configurations.[/**].allow-credentials");

        assertThat(allowedOrigin0).contains("http://localhost:3000");
        assertThat(allowCredentials).isEqualTo("true");
    }

    @Test
    @DisplayName("4. Verify /internal/** endpoints are strictly blocked from external API Gateway routing")
    void testInternalEndpointsNotExposedToGateway() {
        for (int i = 0; i < 10; i++) {
            String predicates = env.getProperty("spring.cloud.gateway.routes[" + i + "].predicates[0]");
            if (predicates != null) {
                assertThat(predicates)
                        .withFailMessage("Gateway route predicate %s must NOT expose /internal/**", predicates)
                        .doesNotContain("/internal/**");
            }
        }
    }
}


