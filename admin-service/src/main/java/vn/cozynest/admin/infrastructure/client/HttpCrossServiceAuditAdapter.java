package vn.cozynest.admin.infrastructure.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class HttpCrossServiceAuditAdapter {

    private static final Logger log = LoggerFactory.getLogger(HttpCrossServiceAuditAdapter.class);
    private static final String DEFAULT_INTERNAL_KEY = "CozyNestInternalServiceKeySecret2026";

    private final RestClient identityClient;
    private final RestClient bookingClient;
    private final RestClient billingClient;
    private final String internalServiceKey;

    @Autowired
    public HttpCrossServiceAuditAdapter(
            @Value("${app.identity-service.url:http://localhost:8081}") String identityUrl,
            @Value("${app.booking-service.url:http://localhost:8083}") String bookingUrl,
            @Value("${app.billing-service.url:http://localhost:8084}") String billingUrl,
            @Value("${app.internal.service-key:" + DEFAULT_INTERNAL_KEY + "}") String internalServiceKey
    ) {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(Duration.ofSeconds(2));
        rf.setReadTimeout(Duration.ofSeconds(3));
        this.identityClient = RestClient.builder().baseUrl(identityUrl).requestFactory(rf).build();
        this.bookingClient = RestClient.builder().baseUrl(bookingUrl).requestFactory(rf).build();
        this.billingClient = RestClient.builder().baseUrl(billingUrl).requestFactory(rf).build();
        this.internalServiceKey = internalServiceKey;
    }

    public List<Map<String, Object>> fetchAll() {
        List<Map<String, Object>> all = new ArrayList<>();
        all.addAll(fetchFrom(billingClient, "/internal/billing/audit-events", "billing"));
        all.addAll(fetchFrom(bookingClient, "/internal/contracts/audit-events", "booking"));
        all.addAll(fetchFrom(identityClient, "/internal/users/audit-events", "identity"));
        return all;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchFrom(RestClient client, String uri, String svc) {
        try {
            Map<String, Object> body = client.get()
                    .uri(uri)
                    .header("X-Internal-Service-Key", internalServiceKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            Object dataObj = body != null ? body.get("data") : null;
            if (dataObj instanceof List<?> list) {
                List<Map<String, Object>> result = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        result.add((Map<String, Object>) m);
                    }
                }
                return result;
            }
        } catch (Exception ex) {
            log.debug("Audit aggregation unavailable for {}: {}", svc, ex.getMessage());
        }
        return List.of();
    }
}
