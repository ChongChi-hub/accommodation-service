package vn.cozynest.communication.domain.repository;

public interface ChatSummaryRepository {
    long countUnreadMessagesForUser(String userId);
}

