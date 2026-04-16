package com.eduardo.accounts_service.dto;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public record ErrorResponseDto(
        String apiPath,
        HttpStatus statusCode,
        String errorMessage,
        LocalDateTime errorTime
) {
}
