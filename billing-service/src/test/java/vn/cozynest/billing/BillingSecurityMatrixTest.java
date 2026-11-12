package vn.cozynest.billing;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import vn.cozynest.billing.interfaces.rest.*;

import static org.assertj.core.api.Assertions.assertThat;

class BillingSecurityMatrixTest {

    @Test
    void enforcesPreAuthorizeRbacOnCriticalFintechControllers() throws NoSuchMethodException {
        PreAuthorize makerChecker = MakerCheckerController.class.getAnnotation(PreAuthorize.class);
        assertThat(makerChecker).isNotNull();
        assertThat(makerChecker.value()).isEqualTo("hasRole('ADMIN')");

        PreAuthorize riskAdmin = AdminRiskController.class.getAnnotation(PreAuthorize.class);
        assertThat(riskAdmin).isNotNull();
        assertThat(riskAdmin.value()).isEqualTo("hasRole('ADMIN')");

        PreAuthorize payout = PayoutController.class.getAnnotation(PreAuthorize.class);
        assertThat(payout).isNotNull();
        assertThat(payout.value()).contains("LANDLORD");

        PreAuthorize accounts = LedgerController.class.getMethod("getAllAccounts").getAnnotation(PreAuthorize.class);
        assertThat(accounts).isNotNull();
        assertThat(accounts.value()).isEqualTo("hasRole('ADMIN')");

        PreAuthorize reconcile = PaymentController.class.getMethod("reconcilePayments").getAnnotation(PreAuthorize.class);
        assertThat(reconcile).isNotNull();
        assertThat(reconcile.value()).isEqualTo("hasRole('ADMIN')");

        PreAuthorize dashAdmin = BillingDashboardController.class.getMethod("getAdminStats").getAnnotation(PreAuthorize.class);
        assertThat(dashAdmin).isNotNull();
        assertThat(dashAdmin.value()).isEqualTo("hasRole('ADMIN')");

        PreAuthorize refundApprove = RefundController.class.getMethod("approveRefund", String.class, org.springframework.security.core.Authentication.class)
                .getAnnotation(PreAuthorize.class);
        assertThat(refundApprove).isNotNull();
        assertThat(refundApprove.value()).isEqualTo("hasRole('ADMIN')");

        PreAuthorize issueCredit = InvoiceController.class.getMethod("issueCreditNote", String.class,
                vn.cozynest.billing.application.dto.InvoiceDto.CreditNoteCommand.class,
                org.springframework.security.core.Authentication.class).getAnnotation(PreAuthorize.class);
        assertThat(issueCredit).isNotNull();
        assertThat(issueCredit.value()).contains("LANDLORD");
    }
}
