package vn.cozynest.communication.infrastructure.email;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class EmailDispatchAndEmailJsTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-11-19T00:00:00Z"), ZoneOffset.UTC);
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private String startMockServer(int[] statusSequence, AtomicInteger calls, AtomicReference<String> lastBody, long delayMs) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/send", exchange -> {
            int idx = calls.getAndIncrement();
            try (InputStream is = exchange.getRequestBody()) {
                lastBody.set(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
            if (delayMs > 0) {
                try { Thread.sleep(delayMs); } catch (InterruptedException ignored) {}
            }
            int code = statusSequence[Math.min(idx, statusSequence.length - 1)];
            byte[] resp = (code == 200 ? "OK" : "ERR").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(code, resp.length);
            exchange.getResponseBody().write(resp);
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/send";
    }

    @Test
    @DisplayName("Default EMAIL_PROVIDER is smtp and delegates to JavaMailEmailServiceAdapter")
    void defaultProviderIsSmtp() {
        JavaMailEmailServiceAdapter smtp = mock(JavaMailEmailServiceAdapter.class);
        EmailDispatchGateway gw = new EmailDispatchGateway(
                smtp, null, new MockEnvironment().withProperty("spring.profiles.active", "dev"),
                "smtp", 150, "", "http://localhost:3000",
                "", "", "", "", "", FIXED_CLOCK, 1000, 1000, 10L
        );
        assertThat(gw.getProvider()).isEqualTo("smtp");
        String status = gw.dispatchEmail("evt-1", "tenant@example.com", "An", "Tieu de", "Noi dung", "<p>Hi</p>", "http://localhost:3000");
        assertThat(status).isEqualTo("SENT");
        verify(smtp, times(1)).sendEmail("tenant@example.com", "Tieu de", "Noi dung", "<p>Hi</p>");
    }

    @Test
    @DisplayName("EmailJS sends exact payload, retries 429/503, does NOT retry 400, deduplicates & enforces monthly cap")
    void emailJsPayloadRetryDedupAndCap() throws Exception {
        AtomicInteger calls = new AtomicInteger(0);
        AtomicReference<String> lastBody = new AtomicReference<>();
        String url = startMockServer(new int[]{429, 503, 200, 400}, calls, lastBody, 0);

        JavaMailEmailServiceAdapter smtp = mock(JavaMailEmailServiceAdapter.class);
        MockEnvironment devEnv = new MockEnvironment();
        devEnv.setActiveProfiles("dev");

        EmailDispatchGateway gw = new EmailDispatchGateway(
                smtp, null, devEnv, "emailjs", 1, "testbox@example.com", "http://localhost:3000",
                url, "svc_test", "tpl_test", "pub_test", "priv_test", FIXED_CLOCK, 1000, 1000, 5L
        );

        // 429 -> 503 -> 200 succeeds on attempt 3 with dev redirect + masked original recipient
        String res1 = gw.dispatchEmail("evt-dup", "alice@cozynest.vn", "Alice", "Lich hen", "Plain", "<p>HTML</p>", "http://localhost:3000/a");
        assertThat(res1).isEqualTo("SENT");
        assertThat(calls.get()).isEqualTo(3);
        assertThat(lastBody.get()).contains("\"service_id\":\"svc_test\"");
        assertThat(lastBody.get()).contains("\"template_id\":\"tpl_test\"");
        assertThat(lastBody.get()).contains("\"to_email\":\"testbox@example.com\"");
        assertThat(lastBody.get()).contains("[DEV, nguoi nhan goc: al***@cozynest.vn] Lich hen");
        assertThat(lastBody.get()).contains("\"message_html\":\"<p>HTML</p>\"");

        // Duplicate eventKey is skipped without HTTP call
        String dup = gw.dispatchEmail("evt-dup", "alice@cozynest.vn", "Alice", "Lich hen", "Plain", "<p>HTML</p>", "http://localhost:3000/a");
        assertThat(dup).isEqualTo("SKIPPED");
        assertThat(calls.get()).isEqualTo(3);

        // Monthly cap = 1 reached -> next distinct event is SKIPPED
        String capped = gw.dispatchEmail("evt-2", "bob@cozynest.vn", "Bob", "Cap test", "Plain", "<p>HTML</p>", "http://localhost:3000/b");
        assertThat(capped).isEqualTo("SKIPPED");
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("EmailJS does NOT retry HTTP 400 and isolates failure without throwing")
    void emailJsNoRetryOn400() throws Exception {
        AtomicInteger calls = new AtomicInteger(0);
        AtomicReference<String> lastBody = new AtomicReference<>();
        String url = startMockServer(new int[]{400}, calls, lastBody, 0);

        EmailDispatchGateway gw = new EmailDispatchGateway(
                mock(JavaMailEmailServiceAdapter.class), null, new MockEnvironment(), "emailjs", 150, "", "http://localhost:3000",
                url, "svc_test", "tpl_test", "pub_test", "priv_test", FIXED_CLOCK, 1000, 1000, 5L
        );

        String status = gw.dispatchEmail("evt-400", "u@example.com", "U", "Sub", "Msg", "<p>Msg</p>", "http://localhost:3000");
        assertThat(status).isEqualTo("FAILED");
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("EmailJS handles read timeout cleanly without throwing")
    void emailJsTimeoutHandled() throws Exception {
        AtomicInteger calls = new AtomicInteger(0);
        AtomicReference<String> lastBody = new AtomicReference<>();
        String url = startMockServer(new int[]{200}, calls, lastBody, 400);

        EmailDispatchGateway gw = new EmailDispatchGateway(
                mock(JavaMailEmailServiceAdapter.class), null, new MockEnvironment(), "emailjs", 150, "", "http://localhost:3000",
                url, "svc_test", "tpl_test", "pub_test", "priv_test", FIXED_CLOCK, 100, 80, 1L
        );

        String status = gw.dispatchEmail("evt-to", "u@example.com", "U", "Sub", "Msg", "<p>Msg</p>", "http://localhost:3000");
        assertThat(status).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("Fail-fast on startup when MAIL_REDIRECT_TO is set in prod or EmailJS keys missing")
    void failFastChecks() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");

        assertThatThrownBy(() -> new EmailDispatchGateway(
                mock(JavaMailEmailServiceAdapter.class), null, prodEnv, "smtp", 150, "dev@example.com", "http://localhost:3000",
                "", "", "", "", "", FIXED_CLOCK, 1000, 1000, 5L
        )).isInstanceOf(IllegalStateException.class).hasMessageContaining("MAIL_REDIRECT_TO");

        assertThatThrownBy(() -> new EmailDispatchGateway(
                mock(JavaMailEmailServiceAdapter.class), null, new MockEnvironment(), "emailjs", 150, "", "http://localhost:3000",
                "http://127.0.0.1:9999/send", "svc_only", "", "", "", FIXED_CLOCK, 1000, 1000, 5L
        )).isInstanceOf(IllegalStateException.class).hasMessageContaining("EMAILJS_TEMPLATE_ID_NOTIFY");
    }

    @Test
    @DisplayName("PII masking hides email local-part")
    void piiMasking() {
        assertThat(EmailDispatchGateway.maskEmail("cuongdfle@gmail.com")).isEqualTo("cu***@gmail.com");
        assertThat(EmailDispatchGateway.maskEmail("a@b.vn")).isEqualTo("*@b.vn");
    }
}
