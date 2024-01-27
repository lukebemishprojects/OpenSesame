package dev.lukebemish.opensesame.annotations.extend;

import dev.lukebemish.opensesame.annotations.Coerce;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows a method in an interface marked with {@link Extend} to override a method in the class being extended. The
 * annotated method must have the same signature as the method being overridden - you may use {@link Coerce} if
 * inaccessible types are used. The method should be a default method, and should have a different name from the method
 * being overridden.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Overrides {
    /**
     * {@return the name of the method being overridden}
     */
    String value();
}
