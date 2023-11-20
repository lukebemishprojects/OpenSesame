package dev.lukebemish.opensesame.compile;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.CLASS)
public @interface OpenSesameGenerated {
    Class<?> value();
}
