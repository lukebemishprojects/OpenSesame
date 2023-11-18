module dev.lukebemish.opensesame.runtime {
    requires jdk.unsupported;
    requires static org.jetbrains.annotations;

    exports dev.lukebemish.opensesame.runtime;

    uses dev.lukebemish.opensesame.runtime.RuntimeRemapper;
}