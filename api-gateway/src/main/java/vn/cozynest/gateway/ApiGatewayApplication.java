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
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("identity-service", r -> r
                        .path("/api/v1/auth/**", "/api/v1/users/**", "/api/v1/landlord/kyc/**", "/api/v1/moderation/kyc/**", "/.well-known/**")
                        .uri(identityServiceUrl))
                .route("listing-service", r -> r
                        .path("/api/v1/rooms", "/api/v1/rooms/**", "/api/v1/locations/**", "/api/v1/amenities/**", "/api/v1/reviews/**", "/api/v1/bookmarks/**", "/api/v1/upload/**", "/uploads/**", "/api/v1/landlord/**", "/api/v1/media/**")
                        .uri(listingServiceUrl))
                .route("booking-service", r -> r
                        .path("/api/v1/landlord/appointments/**", "/api/v1/tenant/appointments/**", "/api/v1/booking/**", "/api/v1/appointments/**", "/api/v1/rental-requests/**", "/api/v1/contracts/**", "/api/v1/maintenance/**")
                        .uri(bookingServiceUrl))
                .route("billing-service", r -> r
                        .path("/api/v1/billing/**", "/api/v1/payments/**", "/api/v1/invoices/**")
                        .uri(billingServiceUrl))
                .route("communication-service", r -> r
                        .path("/api/v1/communication/**", "/api/v1/notifications/**", "/api/v1/chat/**")
                        .uri(communicationServiceUrl))
                .route("admin-service", r -> r
                        .path("/api/v1/admin-service/**", "/api/v1/admin/**", "/api/v1/reports/**", "/api/v1/disputes/**")
                        .uri(adminServiceUrl))
                .build();
    }
}


