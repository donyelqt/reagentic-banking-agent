package dev.reagentic.auth.web;

import dev.reagentic.common.dto.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    @Test
    void malformedJsonBodyMapsToBadRequest() {
        ResponseEntity<ApiError> response = new GlobalExceptionHandler()
                .handleUnreadableBody(new HttpMessageNotReadableException("malformed"));

        assertEquals(400, response.getStatusCode().value());
        ApiError body = response.getBody();
        assertNotNull(body);
        assertEquals("INVALID_BODY", body.code());
    }
}
