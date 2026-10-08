package com.fulfillx.orderservice.order;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.fulfillx.orderservice.security.EmailAlreadyUsedException;
import com.fulfillx.orderservice.security.InvalidCredentialsException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(OrderNotFoundException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> handleInvalidCredentials(InvalidCredentialsException ex) {
        return Map.of("error", "Invalid email or password");
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleEmailTaken(EmailAlreadyUsedException ex) {
        return Map.of("error", "Email already registered");
    }
}