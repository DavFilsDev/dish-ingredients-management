package org.zenith.dishIngredients.dto;

import java.time.Instant;

public class ErrorResponseDTO {
    private final int status;
    private final String message;
    private final String path;
    private final Instant timestamp;

    public ErrorResponseDTO(int status, String message, String path) {
        this.status = status;
        this.message = message;
        this.path = path;
        this.timestamp = Instant.now();
    }

    // Getters
    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}