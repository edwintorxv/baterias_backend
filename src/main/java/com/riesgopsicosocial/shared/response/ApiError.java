package com.riesgopsicosocial.shared.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ApiError {

    private boolean success;

    private String message;

    private String errorCode;

    private List<String> details;

    private LocalDateTime timestamp;

}
