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
    private final Map<String, ForClass> forClasses = new HashMap<>();

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
        List<OpenSesameMixinProvider> providers = new ArrayList<>();
        ServiceLoader.load(OpenSesameMixinProvider.class, classLoader).forEach(providers::add);
        collectLegacyProviders(classLoader, providers);
        final Set<String> targetClasses = new HashSet<>();
        final Set<String> deFinalClasses = new HashSet<>();
        final Map<String, List<String>> deFinalMethods = new HashMap<>();
        final Map<String, List<String>> deFinalFields = new HashMap<>();
        final Set<String> exposeClasses = new HashSet<>();
        final Map<String, List<String>> exposeToOverrideMethods = new HashMap<>();
        final Map<String, List<String>> exposeToOverrideFields = new HashMap<>();
        for (var provider : providers) {
            for (var line : provider.unFinal()) {
                extractActions(line, classLoader, mixins, targetClasses, deFinalClasses, deFinalMethods, deFinalFields, true, "unFinal");
            }
            for (var line : provider.exposeToOverride()) {
                extractActions(line, classLoader, mixins, targetClasses, exposeClasses, exposeToOverrideMethods, exposeToOverrideFields, false, "exposeToOverride");
            }
        }
        for (var target : targetClasses) {
            var forClass = new ForClass(
                    deFinalClasses.contains(target),
                    exposeClasses.contains(target),
                    Set.copyOf(deFinalFields.getOrDefault(target, List.of())),
                    Set.copyOf(deFinalMethods.getOrDefault(target, List.of())),
                    Set.copyOf(exposeToOverrideMethods.getOrDefault(target, List.of()))
            );
            forClasses.put(target, forClass);
        }
        return mixins;
    }

    @SuppressWarnings("removal")
    private static void collectLegacyProviders(ClassLoader classLoader, List<OpenSesameMixinProvider> providers) {
        ServiceLoader.load(UnFinalLineProvider.class, classLoader).forEach(provider -> providers.add(provider.makeToOpen()));
    }

    private static void extractActions(String line, ClassLoader classLoader, List<String> mixins, Set<String> targetClasses, Set<String> classes, Map<String, List<String>> methods, Map<String, List<String>> fields, boolean allowFields, String searchingName) {
        if (line.isBlank())
            return;
        var parts = line.split("\\.");
        var packageName = parts[0].replace("/", ".");
        var className = parts[1].replace("/", ".");
        var remappedClassName = OpeningMetafactory.remapClass(className, classLoader);
        if (parts.length == 2) {
            classes.add(remappedClassName);
        } else if (parts.length == 4) {
            var name = parts[2];
            var desc = parts[3];
            var type = Type.getType(desc);
            if (type.getSort() == Type.METHOD) {
                methods.computeIfAbsent(remappedClassName, k -> new ArrayList<>()).add(OpeningMetafactory.remapMethod(name, desc, className, classLoader));
            } else if (type.getSort() == Type.OBJECT) {
                if (!allowFields) {
                    throw new RuntimeException("Invalid " + searchingName + " line: " + line);
                }
                fields.computeIfAbsent(remappedClassName, k -> new ArrayList<>()).add(OpeningMetafactory.remapField(name, desc, className, classLoader));
            }
        } else {
            throw new RuntimeException("Invalid " + searchingName + " line: " + line);
        }
        var info = ClassInfo.forName(remappedClassName);
        if (info != null) {
            var isPublic = info.isPublic();
            var isClass = !info.isInterface();
            var mixinPath = packageName + "." + (isPublic ? "public" : "private") + (isClass ? "class" : "interface");
            mixins.add(mixinPath);
            targetClasses.add(remappedClassName);
        }
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        var forClass = forClasses.get(targetClassName);
        if (forClass != null) {
            if (forClass.unFinalClass) {
                targetClass.access &= ~Opcodes.ACC_FINAL;
                targetClass.access &= ~Opcodes.ACC_RECORD;
                targetClass.permittedSubclasses = null;
                targetClass.recordComponents = null;
            }
            if (!forClass.unFinalMethods.isEmpty()) {
                for (var method : targetClass.methods) {
                    if (forClass.unFinalMethods.contains(method.name + "." + method.desc)) {
                        method.access &= ~Opcodes.ACC_FINAL;
                    }
                }
            }
            if (!forClass.unFinalFields.isEmpty()) {
                for (var field : targetClass.fields) {
                    if (forClass.unFinalFields.contains(field.name)) {
                        field.access &= ~Opcodes.ACC_FINAL;
                    }
                }
            }

            if (forClass.exposeClass) {
                targetClass.access |= Opcodes.ACC_PUBLIC;
            }

            if (!forClass.exposeToOverrideMethods.isEmpty()) {
                for (var method : targetClass.methods) {
                    if ((method.access & Opcodes.ACC_STATIC) == 0 && (method.access & Opcodes.ACC_PUBLIC) == 0 && forClass.exposeToOverrideMethods.contains(method.name)) {
                        method.access &= ~Opcodes.ACC_PRIVATE;
                        method.access |= Opcodes.ACC_PROTECTED;
                    }
                }
            }
        }
    }

    private record ForClass(
            boolean unFinalClass,
            boolean exposeClass,
            Set<String> unFinalFields,
            Set<String> unFinalMethods,
            Set<String> exposeToOverrideMethods
    ) {}
}
