package dev.lukebemish.opensesame.runtime;

/**
 * Represents an exception thrown while attempting to open access to a member
 */
public class OpeningException extends RuntimeException {
    public OpeningException(Throwable throwable) {
        super(throwable);
    }

    public OpeningException(String message) {
        super(message);
    }

    public OpeningException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
