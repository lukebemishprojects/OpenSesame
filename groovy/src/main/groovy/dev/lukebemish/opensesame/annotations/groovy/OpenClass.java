package dev.lukebemish.opensesame.annotations.groovy;

import groovy.transform.CompileStatic;
import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Opens members of the specified classes to be called within this method, even if they would otherwise be inaccessible.
 */
@Retention(RetentionPolicy.CLASS)
@CompileStatic
@Target(ElementType.METHOD)
@GroovyASTTransformationClass({
        "dev.lukebemish.opensesame.compile.groovy.OpenClassSetupTransformation",
        "dev.lukebemish.opensesame.compile.groovy.OpenClassWriterTransformation",
        "dev.lukebemish.opensesame.compile.groovy.OpenClassBeforeCheckingTransformation"
})
public @interface OpenClass {
    /**
     * The classes to open.
     */
    Class<?>[] value();
}
