package vn.cozynest.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ApiGatewayApplication {

    @Value("${IDENTITY_SERVICE_URL:http://localhost:8081}")
    private String identityServiceUrl;

    @Value("${LISTING_SERVICE_URL:http://localhost:8082}")
    private String listingServiceUrl;

    @Value("${BOOKING_SERVICE_URL:http://localhost:8083}")
    private String bookingServiceUrl;

    @Value("${BILLING_SERVICE_URL:http://localhost:8084}")
    private String billingServiceUrl;

    @Value("${COMMUNICATION_SERVICE_URL:http://localhost:8085}")
    private String communicationServiceUrl;

    @Value("${ADMIN_SERVICE_URL:http://localhost:8086}")
    private String adminServiceUrl;

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    public org.springframework.cloud.gateway.filter.GlobalFilter requestIdFilter() {
        return (exchange, chain) -> {
            String incomingReqId = exchange.getRequest().getHeaders().getFirst("X-Request-ID");
            String cleanUuid = java.util.UUID.randomUUID().toString().replace("-", "");
            String requestId = (incomingReqId != null && !incomingReqId.isBlank())
                    ? incomingReqId
                    : java.util.UUID.randomUUID().toString();

            String traceId = cleanUuid.length() >= 32 ? cleanUuid.substring(0, 32) : (cleanUuid + "00000000000000000000000000000000").substring(0, 32);

            org.springframework.http.server.reactive.ServerHttpRequest mutatedReq = exchange.getRequest().mutate()
                    .header("X-Request-ID", requestId)
                    .header("traceparent", "00-" + traceId + "-0000000000000001-01")
                    .build();

            exchange.getResponse().getHeaders().set("X-Request-ID", requestId);
            return chain.filter(exchange.mutate().request(mutatedReq).build());
        };
    }

    @Bean
    public org.springframework.web.cors.reactive.CorsWebFilter corsWebFilter() {
        org.springframework.web.cors.CorsConfiguration corsConfig = new org.springframework.web.cors.CorsConfiguration();
        corsConfig.setAllowedOrigins(java.util.List.of("http://localhost:3000", "http://127.0.0.1:3000"));
        corsConfig.setMaxAge(3600L);
        corsConfig.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        corsConfig.addAllowedHeader("*");
        corsConfig.setAllowCredentials(true);

        org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource source =
                new org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new org.springframework.web.cors.reactive.CorsWebFilter(source);
    }

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("identity-avatars", r -> r
                        .order(-1)
                        .path("/uploads/avatars/**", "/api/v1/media/avatars/**")
                        .filters(f -> f.dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_UNIQUE")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_UNIQUE"))
                        .uri(identityServiceUrl))
                .route("admin-users", r -> r
                        .order(1)
                        .path("/api/v1/admin/users", "/api/v1/admin/users/**")
                        .filters(f -> f.dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_UNIQUE")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_UNIQUE"))
                        .uri(identityServiceUrl))
                .route("admin-listing", r -> r
                        .order(2)
                        .path("/api/v1/admin/rooms", "/api/v1/admin/rooms/**", "/api/v1/admin/reviews", "/api/v1/admin/reviews/**")
                        .filters(f -> f.dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_UNIQUE")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_UNIQUE"))
                        .uri(listingServiceUrl))
                .route("identity-service", r -> r
                        .order(5)
                        .path("/api/v1/auth/**", "/api/v1/users/**", "/api/v1/me", "/api/v1/me/**", "/api/v1/landlord/kyc/**", "/api/v1/moderation/kyc/**", "/api/v1/admin/kyc/**", "/.well-known/**")
                        .filters(f -> f.dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_UNIQUE")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_UNIQUE"))
                        .uri(identityServiceUrl))
                .route("booking-service", r -> r
                        .order(5)
                        .path("/api/v1/landlord/appointments/**", "/api/v1/tenant/appointments/**", "/api/v1/booking/**", "/api/v1/appointments/**", "/api/v1/rental-requests/**", "/api/v1/contracts/**", "/api/v1/maintenance/**")
                        .filters(f -> f.dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_UNIQUE")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_UNIQUE"))
                        .uri(bookingServiceUrl))
                .route("listing-service", r -> r
                        .order(10)
                        .path("/api/v1/rooms", "/api/v1/rooms/**", "/api/v1/hosts", "/api/v1/hosts/**", "/api/v1/room-types", "/api/v1/room-types/**", "/api/v1/locations/**", "/api/v1/amenities/**", "/api/v1/reviews/**", "/api/v1/bookmarks/**", "/api/v1/upload/**", "/uploads/**", "/api/v1/landlord/**", "/api/v1/media/**", "/api/v1/cms/**")
                        .filters(f -> f.dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_UNIQUE")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_UNIQUE"))
                        .uri(listingServiceUrl))
                .route("billing-service", r -> r
                        .order(5)
                        .path("/api/v1/billing/**", "/api/v1/payments/**", "/api/v1/invoices/**")
                        .filters(f -> f.dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_UNIQUE")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_UNIQUE"))
                        .uri(billingServiceUrl))
                .route("communication-service", r -> r
                        .order(5)
                        .path("/api/v1/communication/**", "/api/v1/notifications", "/api/v1/notifications/**", "/api/v1/chat/**")
                        .filters(f -> f.dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_UNIQUE")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_UNIQUE"))
                        .uri(communicationServiceUrl))
                .route("admin-service", r -> r
                        .order(50)
                        .path("/api/v1/admin-service/**", "/api/v1/admin/**", "/api/v1/reports/**", "/api/v1/disputes/**")
                        .filters(f -> f.dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_UNIQUE")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_UNIQUE"))
                        .uri(adminServiceUrl))
                .build();
    }
}


