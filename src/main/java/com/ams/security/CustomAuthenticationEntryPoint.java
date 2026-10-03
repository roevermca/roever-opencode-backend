package com.ams.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.ams.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        if (authException instanceof org.springframework.security.authentication.DisabledException) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponse errorResponse = new ErrorResponse(false, "User account is inactive. Please contact administrator.");
            errorResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            errorResponse.setError(HttpStatus.UNAUTHORIZED.getReasonPhrase());
            errorResponse.setPath(request.getRequestURI());
            objectMapper.writeValue(response.getWriter(), errorResponse);
            return;
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String message = (String) request.getAttribute("AUTH_ERROR_MESSAGE");
        if (message == null || message.isBlank()) {
            message = authException != null && authException.getMessage() != null
                    ? authException.getMessage()
                    : "Unauthorized: Missing or invalid authentication token";
        }

        ErrorResponse errorResponse = new ErrorResponse(false, message);
        errorResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
        errorResponse.setError(HttpStatus.UNAUTHORIZED.getReasonPhrase());
        errorResponse.setPath(request.getRequestURI());

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
