package vn.cozynest.booking.application.port;

import vn.cozynest.booking.domain.model.AppointmentEventOutbox;

public interface NotificationDispatchPort {
    boolean dispatch(AppointmentEventOutbox event);
}

