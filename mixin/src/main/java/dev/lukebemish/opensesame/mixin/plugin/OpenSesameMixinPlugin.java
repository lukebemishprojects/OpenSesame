package dev.lukebemish.opensesame.mixin.plugin;

import dev.lukebemish.opensesame.runtime.OpeningMetafactory;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;

import java.util.*;

public class OpenSesameMixinPlugin implements IMixinConfigPlugin {
    private final Set<String> deFinalClasses = new HashSet<>();
    private final Map<String, List<String>> deFinalMethods = new HashMap<>();
    private final Map<String, List<String>> deFinalFields = new HashMap<>();

    @Override
    public void onLoad(String mixinPackage) {

    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        List<String> mixins = new ArrayList<>();
        var classLoader = OpenSesameMixinPlugin.class.getClassLoader();
        ServiceLoader.load(UnFinalLineProvider.class, classLoader).forEach(provider -> {
            var lines = provider.lines();
            for (var line : lines) {
                if (line.isBlank())
                    continue;
                var parts = line.split(" ");
                var packageName = parts[0];
                var className = parts[1];
                var remappedClassName = OpeningMetafactory.remapClass(className, classLoader);
                if (parts.length == 2) {
                    deFinalClasses.add(remappedClassName);
                } else if (parts.length == 4) {
                    var name = parts[2];
                    var desc = parts[3];
                    var type = Type.getType(desc);
                    if (type.getSort() == Type.METHOD) {
                        deFinalMethods.computeIfAbsent(remappedClassName, k -> new ArrayList<>()).add(OpeningMetafactory.remapMethod(name, desc, className, classLoader));
                    } else if (type.getSort() == Type.OBJECT) {
                        deFinalFields.computeIfAbsent(remappedClassName, k -> new ArrayList<>()).add(OpeningMetafactory.remapField(name, desc, className, classLoader));
                    }
                } else {
                    throw new RuntimeException("Invalid definal line: " + line);
                }
                var info = ClassInfo.forName(remappedClassName);
                if (info != null) {
                    var isPublic = info.isPublic();
                    var isClass = !info.isInterface();
                    var mixinPath = packageName + "." + (isPublic ? "public" : "private") + (isClass ? "class" : "interface");
                    mixins.add(mixinPath);
                }
            }
        });
        return mixins;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (deFinalClasses.contains(targetClassName)) {
            targetClass.access &= ~Opcodes.ACC_FINAL;
            if (targetClass.permittedSubclasses != null) {
                targetClass.permittedSubclasses.clear();
            }
        }
        if (deFinalMethods.containsKey(targetClassName)) {
            var methods = deFinalMethods.get(targetClassName);
            for (var method : targetClass.methods) {
                if (methods.contains(method.name + " " + method.desc)) {
                    method.access &= ~Opcodes.ACC_FINAL;
                }
            }
        }
        if (deFinalFields.containsKey(targetClassName)) {
            var fields = deFinalFields.get(targetClassName);
            for (var field : targetClass.fields) {
                if (fields.contains(field.name)) {
                    field.access &= ~Opcodes.ACC_FINAL;
                }
            }
        }
    }
}
