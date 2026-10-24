package dev.reagentic.auth.service;

public class PasswordChangeException extends RuntimeException {

    private final String code;

    public PasswordChangeException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
