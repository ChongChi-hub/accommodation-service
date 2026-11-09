package vn.cozynest.communication.application.exception;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException(String id) {
        super("Không tìm thấy thông báo: " + id);
    }
}

