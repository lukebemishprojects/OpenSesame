package dev.lukebemish.opensesame.annotations;

import dev.lukebemish.opensesame.runtime.ErrorProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Replace the body of the annotated method with a call to the target method, constructor, or field operation.
 * Specifies the target class with {@link #targetClass()}, {@link #targetName()}, {@link #targetProvider()}, or a
 * language-specific implementation resolved at compile time, meaning any number of these may be specified. The
 * descriptor that the generated invoker will look for is determined by the descriptor of the annotated method, except
 * where {@link Coerce} is used to specify a different type for the parameter or return type. The annotated method must
 * be static, unless the member targeted is an instance method or field on a class that can be coerced to this type.
 * The method body of the annotated method may be anything, as it will be replaced at compile time - it is recommended to
 * make it throw an exception to ensure that it is obvious if the processor was not applied.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Open {
    /**
     * {@return the name of the target method, constructor, or field operation}
     */
    String name() default "";

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
     * {@return the type of member of the target class to invoke}
     */
    Type type();

    /**
     * {@return whether this invocation should be done unsafely, breaking module boundaries with native access or {@link sun.misc.Unsafe}}
     */
    boolean unsafe() default false;

    /**
     * Different types of members that can be invoked. The orginals of this enum match the constants in {@link dev.lukebemish.opensesame.runtime.OpeningMetafactory}
     */
    enum Type {
        /**
         * Invoke a static method
         */
        STATIC,
        /**
         * Invoke an instance method virtually.
         */
        VIRTUAL,
        /**
         * Invoke an instance method non-virtually.
         */
        SPECIAL,
        /**
         * Get a static field
         */
        GET_STATIC,
        /**
         * Get an instance field
         */
        GET_INSTANCE,
        /**
         * Set a static field
         */
        SET_STATIC,
        /**
         * Set an instance field
         */
        SET_INSTANCE,
        /**
         * Invoke a constructor
         */
        CONSTRUCT,
        /**
         * Create an array
         */
        ARRAY
    }
}
