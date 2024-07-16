module dev.lukebemish.opensesame.natives {
    requires static org.jetbrains.annotations;

    opens dev.lukebemish.opensesame.natives to dev.lukebemish.opensesame.core;
    exports dev.lukebemish.opensesame.natives to dev.lukebemish.opensesame.core;
}
