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
    private static final String MIXIN_CLASS_NAME = "dev.lukebemish.opensesame.mixin.mixin.MadeMutable";

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
        ServiceLoader.load(MutableLineProvider.class, classLoader).forEach(provider -> {
            var lines = provider.lines();
            for (var line : lines) {
                if (line.isBlank())
                    continue;
                var parts = line.split(" ");
                var className = parts[0];
                if (parts.length == 1) {
                    deFinalClasses.add(OpeningMetafactory.remapClass(className, classLoader));
                } else if (parts.length == 3) {
                    var name = parts[1];
                    var desc = parts[2];
                    var type = Type.getType(desc);
                    if (type.getSort() == Type.METHOD) {
                        deFinalMethods.computeIfAbsent(className, k -> List.of()).add(OpeningMetafactory.remapMethod(name, desc, className, classLoader));
                    } else if (type.getSort() == Type.OBJECT) {
                        deFinalFields.computeIfAbsent(className, k -> List.of()).add(OpeningMetafactory.remapField(name, desc, className, classLoader));
                    }
                } else {
                    throw new RuntimeException("Invalid definal line: " + line);
                }
                var providerPath = provider.getClass().getName();
                var info = ClassInfo.forName(className);
                if (info != null) {
                    var isPublic = (info.getAccess() & Opcodes.ACC_PUBLIC) != 0;
                    var isClass = (info.getAccess() & Opcodes.ACC_INTERFACE) == 0;
                    var mixinPath = providerPath + "$mixins." + className.replace('/', '$') + "$" + (isPublic ? "public" : "private") + (isClass ? "class" : "interface");
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
        if (deFinalClasses.contains(mixinClassName)) {
            targetClass.access &= ~Opcodes.ACC_FINAL;
            if (targetClass.permittedSubclasses != null) {
                targetClass.permittedSubclasses.clear();
            }
        } else if (deFinalMethods.containsKey(mixinClassName)) {
            var methods = deFinalMethods.get(mixinClassName);
            for (var method : targetClass.methods) {
                if (methods.contains(method.name + " " + method.desc)) {
                    method.access &= ~Opcodes.ACC_FINAL;
                }
            }
        } else if (deFinalFields.containsKey(mixinClassName)) {
            var fields = deFinalFields.get(mixinClassName);
            for (var field : targetClass.fields) {
                if (fields.contains(field.name + " " + field.desc)) {
                    field.access &= ~Opcodes.ACC_FINAL;
                }
            }
        }
    }
}
