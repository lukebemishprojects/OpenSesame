package dev.lukebemish.opensesame.annotations.extend;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method in an interface marked with {@link Extend} as a constructor for the new subclass. The method's signature
 * must match a constructor in the target class, or have no parameters if the target is an interface. {@link dev.lukebemish.opensesame.annotations.Coerce}
 * may be used if the types are inaccessible. The annotated method must be static, and must return exactly the type of the interface.
 * The method body of the annotated method may be anything, as it will be replaced at compile time - it is recommended to
 * make it throw an exception to ensure that it is obvious if the processor was not applied.
 * <p>Optionally, the normal constructor parameters may be preceded by one
 * or more {@link Field} parameters; the corresponding fields of the generated class will be set after the constructor
 * of the superclass is called, in the constructor of the generated class corresponding to this method.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Constructor {
}
