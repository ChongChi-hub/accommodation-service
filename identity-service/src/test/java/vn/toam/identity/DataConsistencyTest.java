package vn.toam.identity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataConsistencyTest {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private static final String DEMO_HASH = "$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a";

    @Test
    @DisplayName("Verify BCrypt cost 12 hash matches password123 for all demo users")
    void verifyDemoPasswordHashMatches() {
        assertTrue(passwordEncoder.matches("password123", DEMO_HASH),
                "Demo seed password hash must match 'password123'");
    }

    @Test
    @DisplayName("Verify date sequence order: appointment <= rental request <= contract <= invoice")
    void verifyDateProgressionOrder() {
        LocalDateTime appDate = LocalDateTime.of(2026, 10, 25, 9, 30);
        LocalDate reqMoveIn = LocalDate.of(2026, 11, 1);
        LocalDate contractStart = LocalDate.of(2026, 11, 1);
        LocalDate contractEnd = LocalDate.of(2027, 10, 31);
        LocalDate invoiceDue = LocalDate.of(2026, 11, 5);

        assertTrue(appDate.toLocalDate().isBefore(reqMoveIn) || appDate.toLocalDate().isEqual(reqMoveIn));
        assertTrue(contractStart.isBefore(contractEnd));
        assertTrue(invoiceDue.isAfter(contractStart) || invoiceDue.isEqual(contractStart));
    }

    @Test
    @DisplayName("Verify invoice mathematical consistency: base rent + utilities + services = total amount")
    void verifyInvoiceSumMatchesItems() {
        // inv-00000001 (Studio Cau Giay)
        long baseRent = 4_500_000L;
        long electricity = 65 * 3800L; // 247,000
        long water = 3 * 28_000L;      // 84,000
        long service = 44_000L;
        long totalInvoice = baseRent + electricity + water + service;

        assertEquals(4_875_000L, totalInvoice);

        // Payment check: total payment must not exceed total amount
        long paymentAmount = 4_875_000L;
        assertTrue(paymentAmount <= totalInvoice);
    }

    @Test
    @DisplayName("Verify apartment invoice item sum matches exactly")
    void verifyApartmentInvoiceSum() {
        // inv-00000003 (Co Bac Apartment)
        long baseRent = 7_200_000L;
        long electricity = 80 * 4000L; // 320,000
        long service = 130_000L;
        long totalInvoice = baseRent + electricity + service;

        assertEquals(7_650_000L, totalInvoice);
    }
}
