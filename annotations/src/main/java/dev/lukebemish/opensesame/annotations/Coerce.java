package dev.lukebemish.opensesame.annotations;

import dev.lukebemish.opensesame.runtime.ErrorFunction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.PARAMETER})
public @interface Coerce {
    String targetName() default "";
    Class<?> targetClass() default Void.class;
    Class<?> targetProvider() default ErrorFunction.class;
}
