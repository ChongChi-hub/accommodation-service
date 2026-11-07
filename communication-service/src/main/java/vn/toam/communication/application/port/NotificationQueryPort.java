package vn.toam.communication.application.port;

import vn.toam.communication.domain.model.Notification;
import java.util.List;

public interface NotificationQueryPort {
    List<Notification> getUserNotifications(String userId);
}
