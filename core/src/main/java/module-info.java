module dev.lukebemish.opensesame.core {
    requires jdk.unsupported;
    requires org.objectweb.asm;
    requires org.objectweb.asm.commons;
    requires static org.jetbrains.annotations;

    exports dev.lukebemish.opensesame.runtime;
    exports dev.lukebemish.opensesame.annotations;
    exports dev.lukebemish.opensesame.annotations.extend;

    uses dev.lukebemish.opensesame.runtime.RuntimeRemapper;
}