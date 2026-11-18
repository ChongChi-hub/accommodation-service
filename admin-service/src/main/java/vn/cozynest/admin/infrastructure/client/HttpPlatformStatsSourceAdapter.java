package vn.cozynest.admin.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import vn.cozynest.admin.application.port.PlatformStatsSourcePort;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

@Component
public class HttpPlatformStatsSourceAdapter implements PlatformStatsSourcePort {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final String identityUrl;
    private final String listingUrl;
    private final String bookingUrl;
    private final String billingUrl;
    private final String internalServiceKey;

    public HttpPlatformStatsSourceAdapter(
            @Value("${app.identity-service.url:http://localhost:8081}") String identityUrl,
            @Value("${app.listing-service.url:http://localhost:8082}") String listingUrl,
            @Value("${app.booking-service.url:http://localhost:8083}") String bookingUrl,
            @Value("${app.billing-service.url:http://localhost:8084}") String billingUrl,
            @Value("${app.internal.service-key:CozyNestInternalServiceKeySecret2026}") String internalServiceKey
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(5).toMillis());
        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.identityUrl = identityUrl;
        this.listingUrl = listingUrl;
        this.bookingUrl = bookingUrl;
        this.billingUrl = billingUrl;
        this.internalServiceKey = internalServiceKey;
    }

    @Override
    public Map<String, Object> fetchIdentityStats(LocalDate from, LocalDate to) {
        return fetch(identityUrl + "/internal/stats/identity?from=" + from + "&to=" + to);
    }

    @Override
    public Map<String, Object> fetchListingStats(LocalDate from, LocalDate to) {
        return fetch(listingUrl + "/internal/stats/listing?from=" + from + "&to=" + to);
    }

    @Override
    public Map<String, Object> fetchBookingStats(LocalDate from, LocalDate to) {
        return fetch(bookingUrl + "/internal/stats/booking?from=" + from + "&to=" + to);
    }

    @Override
    public Map<String, Object> fetchBillingStats(LocalDate from, LocalDate to) {
        return fetch(billingUrl + "/internal/stats/billing?from=" + from + "&to=" + to);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetch(String url) {
        Map<String, Object> envelope = restClient.get()
                .uri(url)
                .header("X-Internal-Service-Key", internalServiceKey)
                .retrieve()
                .body(MAP_TYPE);
        if (envelope == null || !envelope.containsKey("data")) {
            throw new IllegalStateException("Upstream internal stats endpoint returned invalid envelope");
        }
        Object data = envelope.get("data");
        if (!(data instanceof Map<?, ?> map)) {
            throw new IllegalStateException("Upstream internal stats endpoint returned invalid data payload");
        }
        return (Map<String, Object>) map;
    }
}
