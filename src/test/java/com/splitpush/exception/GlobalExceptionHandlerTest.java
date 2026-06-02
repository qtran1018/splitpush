package com.splitpush.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.persistence.EntityNotFoundException;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void entityNotFound_returns404() {
        ResponseEntity<Map<String, String>> res =
                handler.handleNotFound(new EntityNotFoundException("Trip group not found"));
        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
        assertEquals("Trip group not found", res.getBody().get("error"));
    }

    @Test
    void noSuchElement_returns404() {
        ResponseEntity<Map<String, String>> res =
                handler.handleNoSuchElement(new NoSuchElementException("User not found"));
        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
    }

    @Test
    void accessDenied_returns403() {
        ResponseEntity<Map<String, String>> res =
                handler.handleAccessDenied(new AccessDeniedException("Forbidden"));
        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
        assertEquals("Access denied.", res.getBody().get("error"));
    }

    @Test
    void illegalArgument_returns400() {
        ResponseEntity<Map<String, String>> res =
                handler.handleIllegalArgument(new IllegalArgumentException("Invalid input"));
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertEquals("Invalid input", res.getBody().get("error"));
    }

    @Test
    void runtimeException_returns400WithMessage() {
        ResponseEntity<Map<String, String>> res =
                handler.handleRuntime(new RuntimeException("Expense amount must be positive"));
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertEquals("Expense amount must be positive", res.getBody().get("error"));
    }

    @Test
    void genericException_returns500() {
        ResponseEntity<Map<String, String>> res =
                handler.handleGeneric(new Exception("Database error"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getStatusCode());
        assertEquals("An unexpected error occurred.", res.getBody().get("error"));
    }

    @Test
    void nullMessage_doesNotThrow() {
        ResponseEntity<Map<String, String>> res =
                handler.handleGeneric(new Exception((String) null));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getStatusCode());
        assertNotNull(res.getBody().get("error"));
    }
}
