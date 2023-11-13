package dev.lukebemish.opensesame

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Retention(RetentionPolicy.SOURCE)
@Target([ElementType.METHOD, ElementType.PARAMETER])
@interface Coerce {
    String value()
}