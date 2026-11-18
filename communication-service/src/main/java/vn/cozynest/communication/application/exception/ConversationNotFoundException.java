package vn.cozynest.communication.application.exception;

public class ConversationNotFoundException extends RuntimeException {
    public ConversationNotFoundException(String conversationId) {
        super("Khong tim thay cuoc tro chuyen: " + conversationId);
    }
}
