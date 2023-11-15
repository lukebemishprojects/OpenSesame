package dev.lukebemish.opensesame.annotations;

import dev.lukebemish.opensesame.runtime.ErrorFunction;
import groovy.transform.CompileStatic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Open {
    String name();
    String targetName() default "";
    Class<?> targetClass() default Void.class;
    Class<?> targetProvider() default ErrorFunction.class;
    Type type();

    @CompileStatic
    enum Type {
        STATIC(false, false),
        VIRTUAL(true, false),
        SPECIAL(true, false),
        GET_STATIC(false, true),
        GET_INSTANCE(true, true),
        SET_STATIC(false, true),
        SET_INSTANCE(true, true),
        CONSTRUCT(false, false);

        public final boolean takesInstance;
        public final boolean field;

        Type(boolean takesInstance, boolean field) {
            this.takesInstance = takesInstance;
            this.field = field;
        }
    }
}
