package vn.cozynest.billing.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import vn.cozynest.billing.application.dto.SaveBankAccountRequest;
import vn.cozynest.billing.application.dto.UserBankAccountDto;
import vn.cozynest.billing.domain.model.UserBankAccount;
import vn.cozynest.billing.domain.repository.UserBankAccountRepository;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BankAccountSecurityAndPayoutTest {

    private UserBankAccountRepository repository;
    private BankAccountService service;

    @BeforeEach
    void setUp() {
        repository = mock(UserBankAccountRepository.class);
        service = new BankAccountService(repository);
    }

    @Test
    @DisplayName("saveOrUpdate generates VietQR compact URL and saves uppercase normalized values")
    void saveOrUpdate_generatesVietQrUrl() {
        when(repository.findByUserId("usr-123")).thenReturn(Optional.empty());
        when(repository.save(any(UserBankAccount.class))).thenAnswer(i -> i.getArgument(0));

        SaveBankAccountRequest req = new SaveBankAccountRequest(
                "vcb",
                "Vietcombank",
                "104 270 1900",
                "le thanh cuong",
                null
        );

        UserBankAccountDto res = service.saveOrUpdate("usr-123", req);

        assertThat(res.userId()).isEqualTo("usr-123");
        assertThat(res.bankCode()).isEqualTo("VCB");
        assertThat(res.accountNumber()).isEqualTo("1042701900");
        assertThat(res.accountHolder()).isEqualTo("LE THANH CUONG");
        assertThat(res.qrCodeUrl()).contains("https://img.vietqr.io/image/VCB-1042701900-compact.png");

        ArgumentCaptor<UserBankAccount> captor = ArgumentCaptor.forClass(UserBankAccount.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAccountNumber()).isEqualTo("1042701900");
        assertThat(captor.getValue().isMatched()).isTrue();
    }

    @Test
    @DisplayName("getByUserId returns empty when not found and mapped DTO when found")
    void getByUserId_mapsCorrectly() {
        when(repository.findByUserId("usr-not-found")).thenReturn(Optional.empty());
        assertThat(service.getByUserId("usr-not-found")).isEmpty();

        UserBankAccount entity = new UserBankAccount(
                "acc-1", "usr-1", "MB", "MBBank", "0987654321", "CUONG LE",
                "https://img.vietqr.io/image/MB-0987654321-compact.png", "MATCHED",
                Instant.now(), Instant.now()
        );
        when(repository.findByUserId("usr-1")).thenReturn(Optional.of(entity));

        Optional<UserBankAccountDto> opt = service.getByUserId("usr-1");
        assertThat(opt).isPresent();
        assertThat(opt.get().bankCode()).isEqualTo("MB");
        assertThat(opt.get().accountNumber()).isEqualTo("0987654321");
    }

    @Test
    @DisplayName("requireValidBankAccount throws IllegalStateException when user has not provided bank account and QR")
    void requireValidBankAccount_throwsWhenMissing() {
        when(repository.findByUserId("usr-missing")).thenReturn(Optional.empty());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.requireValidBankAccount("usr-missing", "Khách thuê"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bắt buộc phải cung cấp Số tài khoản ngân hàng");
    }
}
