package dev.reagentic.common.security;

/**
 * Raised when a transfer request lacks a valid agent-issued authorization.
 * Maps to HTTP 403 in the payment-service web layer.
 */
public class TransferForbiddenException extends RuntimeException {
    public TransferForbiddenException(String message) {
        super(message);
    }
}