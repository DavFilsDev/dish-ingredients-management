package org.zenith.dishIngredients.dto;

import java.time.Instant;

public class ErrorResponseDTO {

    private int status;
    private String message;
    private Instant timestamp;
    private String path;

    public ErrorResponseDTO() {
        this.timestamp = Instant.now();
    }

    public ErrorResponseDTO(int status, String message, String path) {
        this.status = status;
        this.message = message;
        this.timestamp = Instant.now();
        this.path = path;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}