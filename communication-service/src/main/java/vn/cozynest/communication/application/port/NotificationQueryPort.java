package vn.cozynest.communication.application.port;

import vn.cozynest.communication.domain.model.Notification;
import java.util.List;

public interface NotificationQueryPort {
    List<Notification> getUserNotifications(String userId);
}
