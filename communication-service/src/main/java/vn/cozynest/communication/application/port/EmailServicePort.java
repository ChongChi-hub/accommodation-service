package vn.cozynest.communication.application.port;

public interface EmailServicePort {
    void sendEmail(String toEmail, String subject, String textContent, String htmlContent);
}
