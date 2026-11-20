package vn.cozynest.identity.interfaces.rest;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LandlordKycDateParsingTest {

    @Test
    void parsesIsoAndVietnameseDateFormatsCleanly() {
        assertThat(LandlordKycController.parseFlexibleDate("2006-07-10", "Ngay sinh"))
                .isEqualTo(LocalDate.of(2006, 7, 10));
        assertThat(LandlordKycController.parseFlexibleDate(" 2006-07-10 ", "Ngay sinh"))
                .isEqualTo(LocalDate.of(2006, 7, 10));
        assertThat(LandlordKycController.parseFlexibleDate("10/07/2006", "Ngay sinh"))
                .isEqualTo(LocalDate.of(2006, 7, 10));
        assertThat(LandlordKycController.parseFlexibleDate("10-07-2006", "Ngay sinh"))
                .isEqualTo(LocalDate.of(2006, 7, 10));
    }

    @Test
    void rejectsBlankOrMalformedDatesWithClearMessage() {
        assertThatThrownBy(() -> LandlordKycController.parseFlexibleDate(" ", "Ngay sinh"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ngay sinh");
        assertThatThrownBy(() -> LandlordKycController.parseFlexibleDate("invalid-date", "Ngay sinh"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("yyyy-MM-dd");
    }
}

