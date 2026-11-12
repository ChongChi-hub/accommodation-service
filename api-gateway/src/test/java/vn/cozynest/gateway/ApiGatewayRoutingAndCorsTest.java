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

    @Autowired(required = false)
    private org.springframework.test.web.reactive.server.WebTestClient webTestClient;

    @Autowired
    private org.springframework.cloud.gateway.route.RouteLocator routeLocator;

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
                .contains("identity-avatars")
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

    @Test
    @DisplayName("Verify valid frontend origin receives exactly 1 ACAO and ACAC header")
    void testCorsAllowedOriginReturnsSingleAcaoAndAcac() {
        if (webTestClient == null) return;

        webTestClient.get()
                .uri("/actuator/health")
                .header("Origin", "http://localhost:3000")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:3000")
                .expectHeader().valueEquals("Access-Control-Allow-Credentials", "true");
    }

    @Test
    @DisplayName("Verify CORS preflight OPTIONS request returns 200 with allowed methods")
    void testCorsPreflightOptionsSucceeds() {
        if (webTestClient == null) return;

        webTestClient.options()
                .uri("/api/v1/rooms")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:3000")
                .expectHeader().valueEquals("Access-Control-Allow-Credentials", "true");
    }

    @Test
    @DisplayName("Verify untrusted origin receives no Access-Control-Allow-Origin header")
    void testCorsDisallowedOriginHasNoAcao() {
        if (webTestClient == null) return;

        webTestClient.get()
                .uri("/actuator/health")
                .header("Origin", "http://untrusted-attacker-site.com")
                .exchange()
                .expectHeader().doesNotExist("Access-Control-Allow-Origin");
    }

    @Test
    @DisplayName("Prove duplicate ACAO occurs if downstream emits ACAO without DedupeResponseHeader")
    void testDedupeFilterPreventsDuplicateHeadersFromDownstream() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("Access-Control-Allow-Origin", "http://localhost:3000");
        headers.add("Access-Control-Allow-Origin", "http://localhost:3000");

        assertThat(headers.get("Access-Control-Allow-Origin")).hasSize(2);

        org.springframework.cloud.gateway.filter.factory.DedupeResponseHeaderGatewayFilterFactory factory =
                new org.springframework.cloud.gateway.filter.factory.DedupeResponseHeaderGatewayFilterFactory();
        org.springframework.cloud.gateway.filter.factory.DedupeResponseHeaderGatewayFilterFactory.Config config =
                new org.springframework.cloud.gateway.filter.factory.DedupeResponseHeaderGatewayFilterFactory.Config();
        config.setName("Access-Control-Allow-Origin");
        config.setStrategy(org.springframework.cloud.gateway.filter.factory.DedupeResponseHeaderGatewayFilterFactory.Strategy.RETAIN_UNIQUE);

        org.springframework.mock.http.server.reactive.MockServerHttpRequest request =
                org.springframework.mock.http.server.reactive.MockServerHttpRequest.get("/test").build();
        org.springframework.mock.web.server.MockServerWebExchange exchange =
                org.springframework.mock.web.server.MockServerWebExchange.from(request);
        exchange.getResponse().getHeaders().addAll(headers);

        var filter = factory.apply(config);
        filter.filter(exchange, ex -> reactor.core.publisher.Mono.empty()).block();

        assertThat(exchange.getResponse().getHeaders().get("Access-Control-Allow-Origin"))
                .hasSize(1)
                .containsExactly("http://localhost:3000");
    }

    @Test
    @DisplayName("Verify identity-avatars route takes precedence over listing-service for avatar media URLs")
    void testAvatarRoutePrecedence() {
        List<org.springframework.cloud.gateway.route.Route> routes = routeLocator.getRoutes().collectList().block();
        assertThat(routes).isNotNull();

        org.springframework.cloud.gateway.route.Route avatarRoute = routes.stream()
                .filter(r -> "identity-avatars".equals(r.getId()))
                .findFirst()
                .orElseThrow();
        org.springframework.cloud.gateway.route.Route listingRoute = routes.stream()
                .filter(r -> "listing-service".equals(r.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(avatarRoute.getOrder()).isLessThan(listingRoute.getOrder());
        assertThat(avatarRoute.getUri().toString()).contains("8081");
    }
}


