package dev.reagentic.ai.web;

import dev.reagentic.ai.agent.AgentResponse;
import dev.reagentic.ai.agent.AgentService;
import dev.reagentic.ai.agent.ApprovalException;
import dev.reagentic.ai.agent.ChatRequest;
import dev.reagentic.ai.agent.ClassifyRequest;
import dev.reagentic.ai.agent.ClassifyResponse;
import dev.reagentic.ai.agent.TransactionClassificationService;
import dev.reagentic.common.dto.ApiResponse;
import dev.reagentic.common.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;
    private final TransactionClassificationService classificationService;

    public AgentController(AgentService agentService, TransactionClassificationService classificationService) {
        this.agentService = agentService;
        this.classificationService = classificationService;
    }

    @PostMapping("/chat")
    public AgentResponse chat(@RequestHeader("Authorization") String authHeader,
                              Authentication auth,
                              @RequestBody ChatRequest req) {
        String token = JwtUtil.bearer(authHeader);
        if (token == null || token.isBlank()) {
            throw new MissingTokenException();
        }
        String subject = auth == null ? null : String.valueOf(auth.getPrincipal());
        return agentService.chat(req, token, roleOf(auth), subject);
    }

    @PostMapping("/classify")
    public ResponseEntity<?> classify(@RequestBody ClassifyRequest req) {
        String error = ClassifyRequest.validate(req);
        if (error != null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", 400, "code", "INVALID_CLASSIFY_REQUEST", "message", error));
        }
        return ResponseEntity.ok(classificationService.classify(req.transactions()));
    }

    @GetMapping("/reconcile/{accountId}")
    public ResponseEntity<?> reconcile(@RequestHeader("Authorization") String authHeader,
                                       Authentication auth,
                                       @PathVariable String accountId) {
        String token = JwtUtil.bearer(authHeader);
        if (token == null || token.isBlank()) {
            throw new MissingTokenException();
        }
        if (!"EMPLOYEE".equals(roleOf(auth))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("status", 403, "code", "FORBIDDEN", "message", "EMPLOYEE role required"));
        }
        try {
            Map<String, Object> result = agentService.reconcile(accountId, token, "EMPLOYEE");
            return ResponseEntity.ok(new ApiResponse<>(true, result, null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", 404, "code", "RECONCILE_FAILED", "message", e.getMessage()));
        }
    }

    private String roleOf(Authentication auth) {
        if (auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_EMPLOYEE".equals(a.getAuthority()))) {
            return "EMPLOYEE";
        }
        return "USER";
    }

    @ExceptionHandler(MissingTokenException.class)
    public ResponseEntity<Void> missing() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @ExceptionHandler(ApprovalException.class)
    public ResponseEntity<Map<String, Object>> approvalError(ApprovalException e) {
        int status = switch (e.getKind()) {
            case FORBIDDEN -> 403;
            case EXPIRED -> 410;
            case INVALID -> 400;
        };
        return ResponseEntity.status(status).body(Map.of(
                "status", status,
                "code", "APPROVAL_" + e.getKind(),
                "message", e.getMessage()));
    }

    public static class MissingTokenException extends RuntimeException {
    }
}
