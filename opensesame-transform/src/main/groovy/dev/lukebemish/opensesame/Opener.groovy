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
        'dev.lukebemish.opensesame.transform.OpenerTransformation'
])
@interface Opener {
    String name()
    String target()
    String desc()
    String[] aliases() default []
    Type type()
    String module() default ""

    @CompileStatic
    enum Type {
        STATIC(false, false),
        VIRTUAL(true, false),
        SPECIAL(true, false),
        PUT_STATIC(false, true),
        GET_STATIC(false, true),
        PUT_INSTANCE(true, true),
        GET_INSTANCE(true, true),
        CONSTRUCT(false, false),

        final boolean takesInstance
        final boolean field

        Type(boolean takesInstance, boolean field) {
            this.takesInstance = takesInstance
            this.field = field
        }
    }
}