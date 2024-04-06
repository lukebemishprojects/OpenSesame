package dev.lukebemish.opensesame.mixin.plugin;

public interface OpenSesameMixinProvider {
    default String[] unFinal() {
        return new String[0];
    }

    default String[] exposeClasses() {
        return new String[0];
    }
}
