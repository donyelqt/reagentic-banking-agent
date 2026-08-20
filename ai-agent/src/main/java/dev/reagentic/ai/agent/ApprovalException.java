package dev.reagentic.ai.agent;

public class ApprovalException extends RuntimeException {

    public enum Kind { INVALID, EXPIRED, FORBIDDEN }

    private final Kind kind;

    public ApprovalException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }
}