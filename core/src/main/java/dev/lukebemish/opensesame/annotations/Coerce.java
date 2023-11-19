package dev.lukebemish.opensesame.annotations;

import dev.lukebemish.opensesame.runtime.ErrorProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Changes generation of the signature to search for when using {@link Open}, when used to annotate a method return type or parameter.
 * Specifies the target class with {@link #targetClass()}, {@link #targetName()}, {@link #targetProvider()}, or a
 * language-specific implementation resolved at compile time, meaning any number of these may be specified.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.PARAMETER})
public @interface Coerce {
    /**
     * {@return the internal name or descriptor of the target class, or the name ot be passed to {@link #targetProvider()}}
     */
    String targetName() default "";

    /**
     * {@return the target class}
     */
    Class<?> targetClass() default void.class;

    /**
     * {@return a class implementing {@link dev.lukebemish.opensesame.runtime.ClassProvider} with a no-arg constructor
     *         that returns the target class. This function will be passed {@link #targetName()} if it is specified, or
     *         null otherwise. On groovy, may be a closure}
     */
    Class<?> targetProvider() default ErrorProvider.class;
}
