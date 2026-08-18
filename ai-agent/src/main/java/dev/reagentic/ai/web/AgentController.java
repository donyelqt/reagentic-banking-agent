package dev.reagentic.ai.web;

import dev.reagentic.ai.agent.AgentResponse;
import dev.reagentic.ai.agent.AgentService;
import dev.reagentic.ai.agent.ChatRequest;
import dev.reagentic.ai.agent.ClassifyRequest;
import dev.reagentic.ai.agent.ClassifyResponse;
import dev.reagentic.ai.agent.TransactionClassificationService;
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
        return agentService.chat(req, token, roleOf(auth));
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

    public static class MissingTokenException extends RuntimeException {
    }
}
