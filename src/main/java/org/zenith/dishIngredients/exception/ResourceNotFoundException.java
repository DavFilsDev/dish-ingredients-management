package org.zenith.dishIngredients.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceType, Object identifier) {
        super(String.format("%s with %s is not found", resourceType, identifier));
    }
}