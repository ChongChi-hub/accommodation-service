package vn.cozynest.communication.infrastructure.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vn.cozynest.communication.application.port.EmailServicePort;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Primary
@Component
public class EmailDispatchGateway implements EmailServicePort {

    private static final Logger log = LoggerFactory.getLogger(EmailDispatchGateway.class);
    private static final Set<String> VALID_PROVIDERS = Set.of("smtp", "emailjs", "log");
    private static final int MAX_PER_RECIPIENT_10M = 5;
    private static final long RATE_WINDOW_MS = 10 * 60 * 1000L;

    private final String provider;
    private final int monthlyCap;
    private final String redirectTo;
    private final String webBaseUrl;
    private final JavaMailEmailServiceAdapter smtpAdapter;
    private final EmailJsMailTransport emailJsTransport;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    private final Map<String, Deque<Long>> recipientHits = new ConcurrentHashMap<>();
    private final Set<String> sentEventKeys = ConcurrentHashMap.newKeySet();
    private final Map<String, AtomicInteger> inMemoryMonthlyCounts = new ConcurrentHashMap<>();

    @Autowired
    public EmailDispatchGateway(
            JavaMailEmailServiceAdapter smtpAdapter,
            @Autowired(required = false) JdbcTemplate jdbcTemplate,
            @Autowired(required = false) Environment env,
            @Value("${mail.provider:smtp}") String provider,
            @Value("${mail.monthly-cap:150}") int monthlyCap,
            @Value("${mail.redirect-to:}") String redirectTo,
            @Value("${app.web.base-url:http://localhost:3000}") String webBaseUrl,
            @Value("${mail.emailjs.endpoint:https://api.emailjs.com/api/v1.0/email/send}") String emailJsEndpoint,
            @Value("${mail.emailjs.service-id:}") String emailJsServiceId,
            @Value("${mail.emailjs.template-id-notify:}") String emailJsTemplateId,
            @Value("${mail.emailjs.public-key:}") String emailJsPublicKey,
            @Value("${mail.emailjs.private-key:}") String emailJsPrivateKey
    ) {
        this(
                smtpAdapter, jdbcTemplate, env, provider, monthlyCap, redirectTo, webBaseUrl,
                emailJsEndpoint, emailJsServiceId, emailJsTemplateId, emailJsPublicKey, emailJsPrivateKey,
                Clock.systemUTC(), 3000, 5000, 500L
        );
    }

    public EmailDispatchGateway(
            JavaMailEmailServiceAdapter smtpAdapter,
            JdbcTemplate jdbcTemplate,
            Environment env,
            String provider,
            int monthlyCap,
            String redirectTo,
            String webBaseUrl,
            String emailJsEndpoint,
            String emailJsServiceId,
            String emailJsTemplateId,
            String emailJsPublicKey,
            String emailJsPrivateKey,
            Clock clock,
            int connectTimeoutMs,
            int readTimeoutMs,
            long baseBackoffMs
    ) {
        this.provider = (provider == null || provider.isBlank()) ? "smtp" : provider.trim().toLowerCase();
        if (!VALID_PROVIDERS.contains(this.provider)) {
            throw new IllegalStateException("Unsupported EMAIL_PROVIDER: " + this.provider + ". Allowed: " + VALID_PROVIDERS);
        }
        this.monthlyCap = monthlyCap > 0 ? monthlyCap : 150;
        this.redirectTo = redirectTo != null ? redirectTo.trim() : "";
        this.webBaseUrl = (webBaseUrl != null && !webBaseUrl.isBlank()) ? webBaseUrl : "http://localhost:3000";
        this.smtpAdapter = smtpAdapter;
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock != null ? clock : Clock.systemUTC();

        boolean isProd = env != null && env.matchesProfiles("prod", "production");
        if (isProd && !this.redirectTo.isBlank()) {
            throw new IllegalStateException("MAIL_REDIRECT_TO is strictly forbidden in production profile");
        }

        if ("emailjs".equals(this.provider)) {
            if (isBlank(emailJsServiceId) || isBlank(emailJsTemplateId) || isBlank(emailJsPublicKey) || isBlank(emailJsPrivateKey)) {
                throw new IllegalStateException(
                        "EMAIL_PROVIDER=emailjs requires EMAILJS_SERVICE_ID, EMAILJS_TEMPLATE_ID_NOTIFY, EMAILJS_PUBLIC_KEY, and EMAILJS_PRIVATE_KEY"
                );
            }
            this.emailJsTransport = new EmailJsMailTransport(
                    emailJsEndpoint, emailJsServiceId, emailJsTemplateId, emailJsPublicKey, emailJsPrivateKey,
                    this.webBaseUrl, connectTimeoutMs, readTimeoutMs, baseBackoffMs
            );
        } else {
            this.emailJsTransport = null;
        }
    }

    @Override
    public void sendEmail(String toEmail, String subject, String textContent, String htmlContent) {
        dispatchEmail(null, toEmail, null, subject, textContent, htmlContent, webBaseUrl);
    }

    public String dispatchEmail(
            String eventKey,
            String toEmail,
            String toName,
            String subject,
            String textContent,
            String htmlContent,
            String actionUrl
    ) {
        if (isBlank(toEmail) || !toEmail.contains("@")) {
            return "SKIPPED";
        }
        String monthKey = YearMonth.from(Instant.now(clock).atZone(ZoneOffset.UTC)).toString();
        String maskedOriginal = maskEmail(toEmail);
        String effectiveTo = toEmail.trim();
        String effectiveSubject = subject != null ? subject : "Thông báo Tổ Ấm";

        if (!redirectTo.isBlank()) {
            effectiveTo = redirectTo;
            effectiveSubject = "[DEV, nguoi nhan goc: " + maskedOriginal + "] " + effectiveSubject;
        }

        String dedupKey = !isBlank(eventKey) ? eventKey + ":" + toEmail.trim().toLowerCase() : null;
        if (dedupKey != null && isAlreadySent(dedupKey)) {
            log.info("Skipping duplicate email eventKey={} recipient={}", eventKey, maskedOriginal);
            return "SKIPPED";
        }

        if (getMonthlySentCount(monthKey) >= monthlyCap) {
            log.warn("Monthly email cap ({}) reached for month={}. Skipping email to {}", monthlyCap, monthKey, maskedOriginal);
            recordOutbox(null, maskedOriginal, effectiveSubject, "SKIPPED", "MONTHLY_CAP_EXCEEDED", monthKey);
            return "SKIPPED";
        }

        if (!allowRecipientRate(toEmail.trim().toLowerCase())) {
            log.warn("Recipient rate limit reached for {}. Skipping email.", maskedOriginal);
            recordOutbox(null, maskedOriginal, effectiveSubject, "SKIPPED", "RECIPIENT_RATE_LIMITED", monthKey);
            return "SKIPPED";
        }

        try {
            switch (provider) {
                case "emailjs" -> emailJsTransport.send(effectiveTo, toName, effectiveSubject, textContent, htmlContent, actionUrl);
                case "log" -> log.info("[MAIL-LOG] to={} subject='{}'", maskEmail(effectiveTo), effectiveSubject);
                default -> smtpAdapter.sendEmail(effectiveTo, effectiveSubject, textContent, htmlContent);
            }
            if (dedupKey != null) {
                sentEventKeys.add(dedupKey);
            }
            inMemoryMonthlyCounts.computeIfAbsent(monthKey, k -> new AtomicInteger(0)).incrementAndGet();
            recordOutbox(dedupKey, maskedOriginal, effectiveSubject, "SENT", null, monthKey);
            return "SENT";
        } catch (Exception ex) {
            log.error("Email dispatch failed via provider={} to={}: {}", provider, maskedOriginal, ex.getMessage());
            recordOutbox(null, maskedOriginal, effectiveSubject, "FAILED", truncate(ex.getMessage(), 480), monthKey);
            return "FAILED";
        }
    }

    public String getProvider() {
        return provider;
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        String prefix = local.length() <= 1 ? "*" : local.substring(0, Math.min(2, local.length())) + "***";
        return prefix + domain;
    }

    private boolean isAlreadySent(String dedupKey) {
        if (sentEventKeys.contains(dedupKey)) return true;
        if (jdbcTemplate == null) return false;
        try {
            Number n = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM email_outbox WHERE event_key = ? AND status = 'SENT'",
                    Number.class, dedupKey
            );
            return n != null && n.longValue() > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private int getMonthlySentCount(String monthKey) {
        if (jdbcTemplate != null) {
            try {
                Number n = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM email_outbox WHERE month_key = ? AND status = 'SENT'",
                        Number.class, monthKey
                );
                if (n != null) return n.intValue();
            } catch (Exception ex) {
                // fallback to in-memory counter
            }
        }
        return inMemoryMonthlyCounts.getOrDefault(monthKey, new AtomicInteger(0)).get();
    }

    private void recordOutbox(String eventKey, String maskedRecipient, String subject, String status, String reason, String monthKey) {
        if (jdbcTemplate == null) return;
        try {
            jdbcTemplate.update(
                    "INSERT INTO email_outbox (id, event_key, provider, recipient_masked, subject, status, reason, month_key, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(3))",
                    UUID.randomUUID().toString(), eventKey, provider, maskedRecipient,
                    truncate(subject, 250), status, reason, monthKey
            );
        } catch (Exception ex) {
            log.warn("Could not persist email_outbox record (status={}): {}", status, ex.getMessage());
        }
    }

    private boolean allowRecipientRate(String normalizedEmail) {
        long now = clock.millis();
        Deque<Long> q = recipientHits.computeIfAbsent(normalizedEmail, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() >= RATE_WINDOW_MS) {
                q.pollFirst();
            }
            if (q.size() >= MAX_PER_RECIPIENT_10M) {
                return false;
            }
            q.addLast(now);
            return true;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
