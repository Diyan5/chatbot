package org.chatbot.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void testHandleExceptionReturnsBadRequest() {
        RuntimeException ex = new RuntimeException("Error occurred");
        ResponseEntity<?> response = handler.handleException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(((Map<?, ?>) response.getBody()).get("message").toString().contains("Error occurred"));
        assertEquals("error", ((Map<?, ?>) response.getBody()).get("status"));
    }
}
