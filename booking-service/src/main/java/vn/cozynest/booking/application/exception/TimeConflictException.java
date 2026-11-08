package vn.cozynest.booking.application.exception;

import java.time.Instant;

public class TimeConflictException extends RuntimeException {
    private final String conflictingAppointmentId;
    private final String conflictingRoomTitle;
    private final Instant conflictingStart;
    private final Instant conflictingEnd;

    public TimeConflictException(String conflictingAppointmentId, String conflictingRoomTitle,
                                 Instant conflictingStart, Instant conflictingEnd) {
        super("Khung gio hen bi trung voi lich hen khac da xac nhan: " + conflictingAppointmentId);
        this.conflictingAppointmentId = conflictingAppointmentId;
        this.conflictingRoomTitle = conflictingRoomTitle;
        this.conflictingStart = conflictingStart;
        this.conflictingEnd = conflictingEnd;
    }

    public String getConflictingAppointmentId() { return conflictingAppointmentId; }
    public String getConflictingRoomTitle() { return conflictingRoomTitle; }
    public Instant getConflictingStart() { return conflictingStart; }
    public Instant getConflictingEnd() { return conflictingEnd; }
}
