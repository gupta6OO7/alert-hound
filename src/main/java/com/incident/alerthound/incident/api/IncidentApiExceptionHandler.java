package com.incident.alerthound.incident.api;

import com.incident.alerthound.incident.service.IncidentNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class IncidentApiExceptionHandler {

    @ExceptionHandler(IncidentNotFoundException.class)
    public ResponseEntity<?> handleNotFound(IncidentNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("not_found", exception.getMessage()));
    }

    private record ErrorResponse(String code, String message) {
    }
}
