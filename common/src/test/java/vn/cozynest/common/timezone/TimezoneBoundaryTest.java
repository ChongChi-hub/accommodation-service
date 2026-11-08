package vn.cozynest.common.timezone;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class TimezoneBoundaryTest {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final ZoneId UTC_ZONE = ZoneId.of("UTC");

    @Test
    @DisplayName("Verify UTC Instant midnight boundary: 23:59:59Z + 1s -> 00:00:00Z next day")
    void testUtcMidnightBoundary() {
        Instant instantBeforeMidnight = Instant.parse("2026-11-08T23:59:59Z");
        Instant instantAtMidnight = instantBeforeMidnight.plus(1, ChronoUnit.SECONDS);

        assertThat(instantAtMidnight).isEqualTo(Instant.parse("2026-11-09T00:00:00Z"));

        ZonedDateTime utcZdtBefore = instantBeforeMidnight.atZone(UTC_ZONE);
        ZonedDateTime utcZdtAfter = instantAtMidnight.atZone(UTC_ZONE);

        assertThat(utcZdtBefore.toLocalDate()).isEqualTo(LocalDate.of(2026, 11, 8));
        assertThat(utcZdtAfter.toLocalDate()).isEqualTo(LocalDate.of(2026, 11, 9));
        assertThat(utcZdtAfter.getHour()).isZero();
        assertThat(utcZdtAfter.getMinute()).isZero();
        assertThat(utcZdtAfter.getSecond()).isZero();
    }

    @Test
    @DisplayName("Verify Vietnam Local Time midnight boundary aligns precisely with UTC 17:00:00Z")
    void testVietnamLocalTimeMidnightBoundary() {
        // Vietnam is UTC+7. Midnight in VN (2026-11-09 00:00:00+07:00) corresponds to 2026-11-08 17:00:00 UTC
        Instant vnMidnightInstant = Instant.parse("2026-11-08T17:00:00Z");
        ZonedDateTime vnZdt = vnMidnightInstant.atZone(VN_ZONE);

        assertThat(vnZdt.toLocalDate()).isEqualTo(LocalDate.of(2026, 11, 9));
        assertThat(vnZdt.getHour()).isZero();
        assertThat(vnZdt.getMinute()).isZero();
        assertThat(vnZdt.getSecond()).isZero();

        // One second before VN midnight is 23:59:59 on the previous calendar date in Vietnam
        Instant oneSecondBefore = vnMidnightInstant.minus(1, ChronoUnit.SECONDS);
        ZonedDateTime vnBeforeZdt = oneSecondBefore.atZone(VN_ZONE);

        assertThat(vnBeforeZdt.toLocalDate()).isEqualTo(LocalDate.of(2026, 11, 8));
        assertThat(vnBeforeZdt.getHour()).isEqualTo(23);
        assertThat(vnBeforeZdt.getMinute()).isEqualTo(59);
        assertThat(vnBeforeZdt.getSecond()).isEqualTo(59);
    }

    @Test
    @DisplayName("Verify monotonic timestamp continuity across midnight without backward jumps")
    void testMonotonicContinuityAcrossMidnight() {
        Instant current = Instant.parse("2026-11-08T23:59:50Z");
        for (int i = 1; i <= 20; i++) {
            Instant next = current.plusSeconds(i);
            assertThat(next).isAfter(current);
        }
    }
}

