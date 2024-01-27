package dev.lukebemish.opensesame.annotations.extend;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a getter, setter, or {@link Constructor} parameter in an interface marked with {@link Extend} as a field. The field will
 * have the type determined by the annotated method or parameter, and the name determined by the value of this annotation.
 * All field annotations in the same interface with the same name must have the same type.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.PARAMETER})
public @interface Field {
    /**
     * {@return the name of the field}
     */
    String value();

    /**
     * Mark a {@link Field} as final. Final fields may not have setters.
     */
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    @interface Final {}
}
