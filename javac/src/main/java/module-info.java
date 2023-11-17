module dev.lukebemish.opensesame.javac {
    requires dev.lukebemish.opensesame.compile;
    requires org.objectweb.asm;
    requires jdk.compiler;
    requires static com.google.auto.service;
    requires static org.jetbrains.annotations;

    exports dev.lukebemish.opensesame.compile.javac;
}