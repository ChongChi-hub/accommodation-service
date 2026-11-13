package vn.cozynest.billing.infrastructure.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.cozynest.billing.application.port.PaymentGatewayPort;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class SandboxVietQrGatewayAdapter implements PaymentGatewayPort {

    private final String gatewayBaseUrl;
    private final String bankBin;
    private final String accountNumber;
    private final String accountName;
    private final boolean killSwitch;

    public SandboxVietQrGatewayAdapter(
            @Value("${fintech.gateway.qr-base-url:https://img.vietqr.io/image}") String gatewayBaseUrl,
            @Value("${fintech.gateway.bank-bin:970422}") String bankBin,
            @Value("${fintech.gateway.virtual-account:SANDBOX0001}") String accountNumber,
            @Value("${fintech.gateway.account-name:COZYNEST%20SANDBOX}") String accountName,
            @Value("${fintech.gateway.kill-switch:false}") boolean killSwitch
    ) {
        this.gatewayBaseUrl = gatewayBaseUrl;
        this.bankBin = bankBin;
        this.accountNumber = accountNumber;
        this.accountName = accountName;
        this.killSwitch = killSwitch;
    }

    @Override
    public String buildPaymentInstructionUrl(String paymentCode, long amountVnd, String orderType) {
        if (killSwitch) {
            throw new IllegalStateException("Cong thanh toan dang tam khoa bao tri (kill-switch active)");
        }
        String encodedInfo = URLEncoder.encode(paymentCode, StandardCharsets.UTF_8);
        String encodedName = URLEncoder.encode(accountName, StandardCharsets.UTF_8).replace("+", "%20");
        return String.format("%s/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                gatewayBaseUrl, bankBin, accountNumber, amountVnd, encodedInfo, encodedName);
    }

    @Override
    public boolean isKillSwitchActive() {
        return killSwitch;
    }
}

