package dev.reagentic.ai.agent;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-held approval sessions. When a plan contains steps that require
 * approval, the ai-agent persists the server-generated plan (with server-derived
 * arguments) keyed by an opaque id and returns that id to the client. An approval
 * echo is only honored if it references a stored session owned by the same caller;
 * the client can choose WHETHER to approve, never WHAT gets executed.
 */
@Component
public class PendingApprovalStore {

    static final long TTL_MILLIS = 10 * 60 * 1000L;
    private static final int MAX_ENTRIES = 500;

    public record Resolved(Plan plan, List<String> pendingStepIds) {
    }

    private record Entry(Plan plan, String subject, List<String> pendingStepIds, long createdAt) {
        boolean expired(long now) {
            return now - createdAt > TTL_MILLIS;
        }
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public String put(Plan plan, String subject, List<String> pendingStepIds) {
        if (entries.size() >= MAX_ENTRIES) {
            entries.entrySet().removeIf(e -> e.getValue().expired(System.currentTimeMillis()));
        }
        String id = UUID.randomUUID().toString();
        entries.put(id, new Entry(plan, subject, List.copyOf(pendingStepIds), System.currentTimeMillis()));
        return id;
    }

    public Resolved resolve(String approvalId, String subject) {
        if (approvalId == null || approvalId.isBlank()) {
            throw new ApprovalException(ApprovalException.Kind.INVALID,
                    "approval requires a server-issued approvalId");
        }
        Entry e = entries.get(approvalId);
        if (e == null || e.expired(System.currentTimeMillis())) {
            entries.remove(approvalId);
            throw new ApprovalException(ApprovalException.Kind.EXPIRED,
                    "approval session expired - request a new plan");
        }
        if (!e.subject().equals(subject)) {
            throw new ApprovalException(ApprovalException.Kind.FORBIDDEN,
                    "approval session belongs to another user");
        }
        return new Resolved(e.plan(), e.pendingStepIds());
    }
}