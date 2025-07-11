import dev.lukebemish.opensesame.natives.ModuleRequirementShim;
import dev.lukebemish.opensesame.runtime.RuntimeRemapper;

module dev.lukebemish.opensesame.natives {
    requires dev.lukebemish.opensesame.core;
    requires static org.jetbrains.annotations;

    opens dev.lukebemish.opensesame.natives to dev.lukebemish.opensesame.core;
    exports dev.lukebemish.opensesame.natives to dev.lukebemish.opensesame.core;

    provides RuntimeRemapper with ModuleRequirementShim;
}
