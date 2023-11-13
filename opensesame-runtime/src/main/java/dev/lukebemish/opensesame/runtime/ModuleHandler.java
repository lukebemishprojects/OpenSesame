package dev.lukebemish.opensesame.runtime;

interface ModuleHandler {
    boolean openModule(Module to, Module target, String className);
}
