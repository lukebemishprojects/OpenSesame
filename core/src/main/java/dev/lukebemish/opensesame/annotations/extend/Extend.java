package dev.lukebemish.opensesame.annotations.extend;

import dev.lukebemish.opensesame.runtime.ErrorProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Extend {
    String targetName() default "";

    Class<?> targetClass() default void.class;

    Class<?> targetProvider() default ErrorProvider.class;
}
