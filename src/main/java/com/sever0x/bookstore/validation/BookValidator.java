package com.sever0x.bookstore.validation;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class BookValidator {

    public ValidationResult validateRequest(String title, String author, String isbn, int quantity) {
        ValidationResult result = new ValidationResult();

        if (!StringUtils.hasText(title)) {
            result.addError("Title must not be blank");
        }

        if (!StringUtils.hasText(author)) {
            result.addError("Author must not be blank");
        }

        if (!StringUtils.hasText(isbn) || !isValidISBN(isbn)) {
            result.addError("ISBN must be a valid 13-digit code");
        }

        if (quantity < 0) {
            result.addError("Quantity must be zero or positive");
        }

        return result;
    }

    private boolean isValidISBN(String isbn) {
        return isbn.matches("^\\d{13}$");
    }

    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}