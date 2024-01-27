package dev.lukebemish.opensesame.annotations.extend;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a constructor in an interface marked with {@link Extend} as the constructor for new subclass. The constructor's signature
 * must match a constructor in the target class, or the no-arg constructor if the target is an interface. {@link dev.lukebemish.opensesame.annotations.Coerce}
 * may be used if the types are inaccessible. Optionally, the normal constructor parameters may be preceded by one
 * or more {@link Field} parameters; the corresponding fields of the generated class will be set after the constructor
 * of the superclass is called, in the constructor of the generated class corresponding to this method. The annotated
 * method must be static.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Constructor {
}
