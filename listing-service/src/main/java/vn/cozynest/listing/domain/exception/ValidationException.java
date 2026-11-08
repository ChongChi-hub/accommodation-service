package vn.cozynest.listing.domain.exception;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class ValidationException extends RuntimeException {

    private final List<FieldError> errors;

    public ValidationException(String message, List<FieldError> errors) {
        super(message);
        this.errors = errors != null ? errors : Collections.emptyList();
    }

    public ValidationException(String field, String message) {
        super(message);
        this.errors = List.of(new FieldError(field, message));
    }

    @Getter
    public static class FieldError {
        private final String field;
        private final String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }
    }
}

