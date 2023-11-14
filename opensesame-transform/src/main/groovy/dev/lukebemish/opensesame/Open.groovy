package dev.lukebemish.opensesame

import groovy.transform.CompileStatic
import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Retention(RetentionPolicy.SOURCE)
@Target([ElementType.METHOD])
@GroovyASTTransformationClass([
        'dev.lukebemish.opensesame.transform.OpenTransformation'
])
@interface Open {
    String name()
    String targetName() default ""
    Class<?> targetClass() default Void
    Class<? extends Closure<Class<?>>> targetProvider() default { }
    Type type()

    @CompileStatic
    enum Type {
        STATIC(false, false),
        VIRTUAL(true, false),
        SPECIAL(true, false),
        GET_STATIC(false, true),
        GET_INSTANCE(true, true),
        SET_STATIC(false, true),
        SET_INSTANCE(true, true),
        CONSTRUCT(false, false),

        final boolean takesInstance
        final boolean field

        Type(boolean takesInstance, boolean field) {
            this.takesInstance = takesInstance
            this.field = field
        }
    }
}