package dev.lukebemish.opensesame.annotations.extend;

import dev.lukebemish.opensesame.runtime.ErrorProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an interface as an "extension" interface. Extension interfaces allow you to subclass classes or implement interfaces.
 * You should not implement an extension interface directly, and extension interfaces cannot
 * extend other extension interfaces. Extension interfaces will have a corresponding hidden class generated at runtime.
 * <p>An extension interface must be visible from its target class, meaning it almost certainly must be public.
 * OpenSesame will attempt to handle module boundaries and create any missing edges in the module graph if it is able to.
 * @see Constructor
 * @see Overrides
 * @see Field
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Extend {
    /**
     * {@return the internal name or descriptor of the target class, or the name to be passed to {@link #targetProvider()}}
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

    /**
     * Determines whether the extension can occur over module boundaries with {@link sun.misc.Unsafe}. Note that this
     * is required to be {@code true} in order to extend non-public classes in another module.
     * @return whether the extension should be done unsafely, breaking module boundaries
     */
    boolean unsafe();
}
