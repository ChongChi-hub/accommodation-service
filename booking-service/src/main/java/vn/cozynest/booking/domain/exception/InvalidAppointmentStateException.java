package vn.cozynest.booking.domain.exception;

import vn.cozynest.booking.domain.model.AppointmentStatus;

public class InvalidAppointmentStateException extends IllegalStateException {

    private final AppointmentStatus currentStatus;
    private final AppointmentStatus targetStatus;

    public InvalidAppointmentStateException(AppointmentStatus currentStatus, AppointmentStatus targetStatus) {
        super(String.format("Khong the chuyen trang thai lich hen tu %s sang %s", currentStatus, targetStatus));
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public AppointmentStatus getCurrentStatus() {
        return currentStatus;
    }

    public AppointmentStatus getTargetStatus() {
        return targetStatus;
    }
}
