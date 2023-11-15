package dev.lukebemish.opensesame.annotations;

import dev.lukebemish.opensesame.runtime.ErrorFunction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Changes generation of the signature to search for when using {@link Open}, when used to annotate a method return type or parameter.
 * Specifies the target class with {@link #targetClass()}, {@link #targetName()}, {@link #targetProvider()}, or a
 * language-specific implementation resolved at compile time, meaning at most one of these may be specified.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.PARAMETER})
public @interface Coerce {
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
}
