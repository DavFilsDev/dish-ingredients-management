package org.zenith.dishIngredients.exception;

/**
 * Exception personnalisée pour les ressources non trouvées
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceType, int id) {
        super(String.format("%s.id=%d is not found", resourceType, id));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}