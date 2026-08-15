package dev.reagentic.ai.web;

import dev.reagentic.ai.agent.AgentResponse;
import dev.reagentic.ai.agent.AgentService;
import dev.reagentic.ai.agent.ChatRequest;
import dev.reagentic.common.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    public AgentResponse chat(@RequestHeader("Authorization") String authHeader,
                              @RequestBody ChatRequest req) {
        String token = JwtUtil.bearer(authHeader);
        if (token == null || token.isBlank()) {
            throw new MissingTokenException();
        }
        return agentService.chat(req, token);
    }

    @ExceptionHandler(MissingTokenException.class)
    public ResponseEntity<Void> missing() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    public static class MissingTokenException extends RuntimeException {
    }
}
