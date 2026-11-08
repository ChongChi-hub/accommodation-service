package vn.cozynest.booking.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ViewingAppointmentDomainTest {

    private ViewingAppointment createPendingAppointment(Instant start) {
        ViewingAppointment appt = new ViewingAppointment();
        appt.setId("test-appt-01");
        appt.setLandlordId("landlord-01");
        appt.setTenantId("tenant-01");
        appt.setRoomId("room-01");
        appt.setStatus(AppointmentStatus.PENDING);
        appt.setScheduledStart(start);
        appt.setScheduledEnd(start.plus(45, ChronoUnit.MINUTES));
        return appt;
    }

    @Test
    @DisplayName("confirm should transition PENDING to CONFIRMED and record confirmedAt")
    void testConfirmSuccess() {
        ViewingAppointment appt = createPendingAppointment(Instant.now().plus(1, ChronoUnit.DAYS));
        appt.confirm("landlord-01", "Dong y hen xem phong");

        assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(appt.getConfirmedAt()).isNotNull();
        assertThat(appt.getLandlordNote()).isEqualTo("Dong y hen xem phong");
    }

    @Test
    @DisplayName("confirm should throw exception when status is not PENDING")
    void testConfirmThrowsOnNonPending() {
        ViewingAppointment appt = createPendingAppointment(Instant.now().plus(1, ChronoUnit.DAYS));
        appt.setStatus(AppointmentStatus.CONFIRMED);

        assertThatThrownBy(() -> appt.confirm("landlord-01", null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("decline should transition PENDING to DECLINED and validate OTHER reason note")
    void testDeclineSuccess() {
        ViewingAppointment appt = createPendingAppointment(Instant.now().plus(1, ChronoUnit.DAYS));
        appt.decline("landlord-01", CancelReasonCode.ROOM_TAKEN, "Phong da co khach coc");

        assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.DECLINED);
        assertThat(appt.getCancelReasonCode()).isEqualTo(CancelReasonCode.ROOM_TAKEN);
        assertThat(appt.getCancelledBy()).isEqualTo(CancelledBy.LANDLORD);
    }

    @Test
    @DisplayName("decline with reason OTHER requires at least 10 chars note")
    void testDeclineOtherRequiresValidNote() {
        ViewingAppointment appt = createPendingAppointment(Instant.now().plus(1, ChronoUnit.DAYS));

        assertThatThrownBy(() -> appt.decline("landlord-01", CancelReasonCode.OTHER, "Ngan qua"))
                .isInstanceOf(IllegalArgumentException.class);

        appt.decline("landlord-01", CancelReasonCode.OTHER, "Ly do cu the chi tiet hon 10 ky tu");
        assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.DECLINED);
    }

    @Test
    @DisplayName("cancel should transition CONFIRMED to CANCELLED")
    void testCancelSuccess() {
        ViewingAppointment appt = createPendingAppointment(Instant.now().plus(1, ChronoUnit.DAYS));
        appt.setStatus(AppointmentStatus.CONFIRMED);

        appt.cancel("landlord-01", CancelledBy.LANDLORD, CancelReasonCode.BUSY, "Ban viec dot xuat");

        assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appt.getCancelledBy()).isEqualTo(CancelledBy.LANDLORD);
    }

    @Test
    @DisplayName("complete should transition CONFIRMED to COMPLETED or NO_SHOW when start is in past")
    void testCompleteSuccess() {
        ViewingAppointment appt = createPendingAppointment(Instant.now().minus(2, ChronoUnit.HOURS));
        appt.setStatus(AppointmentStatus.CONFIRMED);

        appt.complete("landlord-01", AppointmentStatus.COMPLETED, "Khach da den xem va ung y");
        assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(appt.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("complete should throw if appointment start is in future")
    void testCompleteThrowsIfFuture() {
        ViewingAppointment appt = createPendingAppointment(Instant.now().plus(2, ChronoUnit.HOURS));
        appt.setStatus(AppointmentStatus.CONFIRMED);

        assertThatThrownBy(() -> appt.complete("landlord-01", AppointmentStatus.COMPLETED, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("expire should transition past PENDING appointment to EXPIRED")
    void testExpireSuccess() {
        ViewingAppointment appt = createPendingAppointment(Instant.now().minus(1, ChronoUnit.HOURS));

        appt.expire();
        assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.EXPIRED);
        assertThat(appt.getCancelledBy()).isEqualTo(CancelledBy.SYSTEM);
    }

    @Test
    @DisplayName("terminal states cannot transition and throw InvalidAppointmentStateException")
    void testTerminalStatesCannotTransition() {
        AppointmentStatus[] terminals = {
                AppointmentStatus.DECLINED,
                AppointmentStatus.CANCELLED,
                AppointmentStatus.COMPLETED,
                AppointmentStatus.NO_SHOW,
                AppointmentStatus.EXPIRED
        };

        for (AppointmentStatus terminal : terminals) {
            ViewingAppointment appt = createPendingAppointment(Instant.now());
            appt.setStatus(terminal);

            assertThatThrownBy(() -> appt.confirm("landlord-01", null))
                    .isInstanceOf(vn.cozynest.booking.domain.exception.InvalidAppointmentStateException.class);

            assertThatThrownBy(() -> appt.decline("landlord-01", CancelReasonCode.ROOM_TAKEN, "note"))
                    .isInstanceOf(vn.cozynest.booking.domain.exception.InvalidAppointmentStateException.class);

            assertThatThrownBy(() -> appt.expire())
                    .isInstanceOf(vn.cozynest.booking.domain.exception.InvalidAppointmentStateException.class);
        }
    }
}
