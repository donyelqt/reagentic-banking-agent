package dev.reagentic.payment.web;

import dev.reagentic.common.dto.ApiResponse;
import dev.reagentic.common.money.Money;
import dev.reagentic.payment.domain.Payment;
import dev.reagentic.payment.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public record TransferRequest(
            @NotBlank String sourceAccountId,
            @NotBlank String destinationAccountId,
            @NotBlank String amount,
            @NotBlank String idempotencyKey) {
    }

    public record PaymentView(String paymentId, String status, String source,
                              String destination, Money amount, String currency, String reason) {
    }

    @PostMapping("/transfer")
    public ApiResponse<PaymentView> transfer(@RequestHeader("Authorization") String authHeader,
                                             @Valid @RequestBody TransferRequest req) {
        Payment p = paymentService.transfer(authHeader, req.sourceAccountId(),
                req.destinationAccountId(), Money.of(req.amount()), req.idempotencyKey());
        return ApiResponse.ok(new PaymentView(p.getPaymentId(), p.getStatus().name(),
                p.getSourceAccountId(), p.getDestinationAccountId(),
                Money.of(p.getAmount()), p.getCurrency(), p.getReason()));
    }

    @ExceptionHandler(PaymentService.PaymentFailedException.class)
    public ResponseEntity<Map<String, Object>> failed(PaymentService.PaymentFailedException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 409);
        body.put("code", "PAYMENT_FAILED");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
}
