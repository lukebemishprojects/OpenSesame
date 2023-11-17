module dev.lukebemish.opensesame.compile {
    requires transitive dev.lukebemish.opensesame.annotations;
    requires static org.jetbrains.annotations;

    exports dev.lukebemish.opensesame.compile to
            dev.lukebemish.opensesame.groovy,
            dev.lukebemish.opensesame.javac;
}