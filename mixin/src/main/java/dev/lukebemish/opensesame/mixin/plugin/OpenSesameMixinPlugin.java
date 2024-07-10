package dev.lukebemish.opensesame.mixin.plugin;

import dev.lukebemish.opensesame.runtime.OpeningMetafactory;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

public class OpenSesameMixinPlugin implements IMixinConfigPlugin {
    private static final String EXPOSE_LOOKUP_FIELD = "$$dev$lukebemish$opensesame$$LOOKUP";

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

            if (forClass.exposeClass || !forClass.exposeToOverrideMethods.isEmpty()) {
                if (targetClass.fields.stream().noneMatch(it -> it.name.equals(EXPOSE_LOOKUP_FIELD))) {
                    targetClass.visitField(
                            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                            EXPOSE_LOOKUP_FIELD,
                            MethodHandles.Lookup.class.descriptorString(),
                            null,
                            null
                    );
                    MethodNode clinit = targetClass.methods.stream().filter(it -> it.name.equals("<clinit>")).findAny().orElseGet(() -> {
                        MethodNode it = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
                        it.instructions.add(new InsnNode(Opcodes.RETURN));
                        targetClass.methods.add(it);
                        return it;
                    });
                    InsnList setup = new InsnList();
                    setup.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(MethodHandles.class), "lookup", MethodType.methodType(MethodHandles.Lookup.class).descriptorString(), false));
                    setup.add(new FieldInsnNode(Opcodes.PUTSTATIC, targetClass.name, EXPOSE_LOOKUP_FIELD, MethodHandles.Lookup.class.descriptorString()));

                    clinit.instructions.insert(setup);
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
