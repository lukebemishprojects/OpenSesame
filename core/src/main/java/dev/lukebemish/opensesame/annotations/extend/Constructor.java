package dev.lukebemish.opensesame.annotations.extend;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a constructor in an interface marked with {@link Extend} as the constructor for new subclass. The constructor
 * must match a constructor in the target class, or the no-arg constructor if the target is an interface. {@link dev.lukebemish.opensesame.annotations.Coerce}
 * may be used if the types are inaccessible. Optionally, the normal constructor parameters may be preceded by one
 * or more {@link Field} parameters; these will be set after the super constructor is called.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Constructor {
}
