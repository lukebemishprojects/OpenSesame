module dev.lukebemish.opensesame.runtime {
    requires static org.jetbrains.annotations;
    requires jdk.unsupported;

    exports dev.lukebemish.opensesame.runtime;

    uses dev.lukebemish.opensesame.runtime.RuntimeRemapper;
}