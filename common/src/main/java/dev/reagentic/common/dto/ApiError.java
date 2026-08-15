package dev.reagentic.common.dto;

/**
 * Standard error envelope returned by every service.
 */
public record ApiError(int status, String code, String message, long timestamp) {
    public ApiError(int status, String code, String message) {
        this(status, code, message, System.currentTimeMillis());
    }
}
