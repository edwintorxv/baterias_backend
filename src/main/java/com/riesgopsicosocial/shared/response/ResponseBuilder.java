package com.riesgopsicosocial.shared.response;

import java.time.LocalDateTime;
import java.util.List;

public final class ResponseBuilder {

    private ResponseBuilder() {
    }

    public static <T> ApiResponse<T> success(String message, T data) {

        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ApiError error(String message, String errorCode, List<String> details) {
        return ApiError.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }

}