module dev.lukebemish.opensesame.groovy {
    requires dev.lukebemish.opensesame.compile;
    requires org.apache.groovy;
    requires static com.google.auto.service;
    requires static org.jetbrains.annotations;

    exports dev.lukebemish.opensesame.compile.groovy;
    exports dev.lukebemish.opensesame.annotations.groovy;
}