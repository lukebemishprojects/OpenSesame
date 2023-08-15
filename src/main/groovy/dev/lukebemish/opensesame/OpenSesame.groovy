package dev.lukebemish.opensesame


import groovy.transform.CompileStatic
import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Retention(RetentionPolicy.SOURCE)
@CompileStatic
@Target([ElementType.METHOD])
@GroovyASTTransformationClass(['dev.lukebemish.opensesame.transform.OpenSesameASTTransformation', 'dev.lukebemish.opensesame.transform.OpenSesameWriterTransformation'])
@interface OpenSesame {
    Class<?>[] value();
}