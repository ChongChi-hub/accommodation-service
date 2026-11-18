package vn.cozynest.communication.application.exception;

public class ChatRateLimitException extends RuntimeException {
    public ChatRateLimitException(String message) {
        super(message);
    }
}
