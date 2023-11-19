package dev.lukebemish.opensesame.compile.javac;

public @interface OpenSesameGenerated {
    Type value();

    enum Type {
        OPEN
    }
}
