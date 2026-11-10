package vn.cozynest.identity.infrastructure.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.cozynest.identity.application.port.SmsSenderPort;

@Component
public class MockSmsSenderAdapter implements SmsSenderPort {
    private static final Logger log = LoggerFactory.getLogger(MockSmsSenderAdapter.class);

    @Override
    public void sendOtpSms(String phoneNumber, String otp) {
        // Mask phone number for logging compliance (no PII in logs)
        String masked = phoneNumber != null && phoneNumber.length() >= 6
                ? phoneNumber.substring(0, 3) + "***" + phoneNumber.substring(phoneNumber.length() - 3)
                : "***";
        log.info("[SMS-GATEWAY-SANDBOX] Sending OTP to {}: [MASKED_SECURE_TOKEN]", masked);
    }
}

