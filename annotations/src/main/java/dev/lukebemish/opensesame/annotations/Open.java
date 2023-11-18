package dev.lukebemish.opensesame.annotations;

import dev.lukebemish.opensesame.runtime.ErrorFunction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Replace the body of the annotated method with a call to the target method, constructor, or field operation.
 * Specifies the target class with {@link #targetClass()}, {@link #targetName()}, {@link #targetProvider()}, or a
 * language-specific implementation resolved at compile time, meaning at most one of these may be specified. The
 * descriptor that the generated invoker will look for is determined by the descriptor of the annotated method, except
 * where {@link Coerce} is used to specify a different type for the parameter or return type. The annotated method must
 * be static, unless the member targeted is an instance method or field on a class that can be coerced to this type.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Open {
    /**
     * @return the name of the target method, constructor, or field operation
     */
    String name();

    /**
     * @return the internal name of the target class
     */
    String targetName() default "";

    /**
     * @return the target class
     */
    Class<?> targetClass() default Void.class;

    /**
     * @return a class implementing {@link java.util.function.Function Function&lt;ClassLoader, Class&lt;?&gt;&gt;} with a no-arg constructor that returns the target class
     */
    Class<?> targetProvider() default ErrorFunction.class;

    /**
     * @return the type of member o the target class to invoke
     */
    Type type();

    /**
     * @return whether this invocation should be done unsafely, breaking module boundaries
     */
    boolean unsafe() default false;

    /**
     * Different types of members that can be invoked
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
