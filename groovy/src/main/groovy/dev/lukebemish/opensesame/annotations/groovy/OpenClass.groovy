package dev.lukebemish.opensesame.annotations.groovy


import groovy.transform.CompileStatic
import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Opens members of the specified classes to be called within this method, even if they would otherwise be inaccessible.
 */
@Retention(RetentionPolicy.SOURCE)
@CompileStatic
@Target([ElementType.METHOD])
@GroovyASTTransformationClass([
        'dev.lukebemish.opensesame.transform.groovy.OpenClassSetupTransformation',
        'dev.lukebemish.opensesame.transform.groovy.OpenClassWriterTransformation',
        'dev.lukebemish.opensesame.transform.groovy.OpenClassBeforeCheckingTransformation'
])
@interface OpenClass {
    /**
     * The classes to open.
     */
    Class<?>[] value();
}