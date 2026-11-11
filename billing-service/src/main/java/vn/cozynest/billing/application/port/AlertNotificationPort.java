package vn.cozynest.billing.application.port;

public interface AlertNotificationPort {
    boolean sendAlert(String title, String message);
}

