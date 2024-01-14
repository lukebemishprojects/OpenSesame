package dev.lukebemish.opensesame.compile;

import dev.lukebemish.opensesame.annotations.Open;
import dev.lukebemish.opensesame.runtime.ClassProvider;
import dev.lukebemish.opensesame.runtime.OpeningMetafactory;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.*;
import java.util.List;
import java.util.function.Function;

public class ConDynUtils<T, CD, H> {
    private final TypeProvider<T, CD, H> types;

    public ConDynUtils(TypeProvider<T, CD, H> types) {
        this.types = types;
    }

    public record TypedDynamic<CD, T>(CD constantDynamic, @Nullable T type) {}

    public CD invoke(String descriptor, Object handle, Object... args) {
        Object[] fullArgs = new Object[args.length+1];
        System.arraycopy(args, 0, fullArgs, 1, args.length);
        fullArgs[0] = handle;
        return types.constantDynamic(
                "invoke",
                descriptor,
                types.handle(
                        Opcodes.H_INVOKESTATIC,
                        types.internalName(ConstantBootstraps.class),
                        "invoke",
                        MethodType.methodType(Object.class, MethodHandles.Lookup.class, String.class, Class.class, MethodHandle.class, Object[].class).descriptorString(),
                        false
                ),
                fullArgs
        );
    }

    public CD booleanConstant(boolean bool) {
        return types.constantDynamic(
                // booleans are fucky in ConstantDynamics. Here's an alternative...
                bool ? "TRUE" : "FALSE",
                Boolean.class.descriptorString(),
                types.handle(
                        Opcodes.H_INVOKESTATIC,
                        types.internalName(ConstantBootstraps.class),
                        "getStaticFinal",
                        MethodType.methodType(Object.class, MethodHandles.Lookup.class, String.class, Class.class, Class.class).descriptorString(),
                        false
                ),
                types.type(Boolean.class)
        );
    }

    public CD conDynMethodType(Object returnType, List<Object> parameterTypes) {
        var fixedArityMethodType = invoke(
                MethodHandle.class.descriptorString(),
                types.handle(
                    Opcodes.H_INVOKEVIRTUAL,
                    types.internalName(MethodHandle.class),
                    "asCollector",
                    MethodType.methodType(MethodHandle.class, Class.class, int.class).descriptorString(),
                    false
                ),
                types.handle(
                    Opcodes.H_INVOKESTATIC,
                    types.internalName(MethodType.class),
                    "methodType",
                    MethodType.methodType(MethodType.class, Class.class, Class[].class).descriptorString(),
                    false
                ),
                types.type(Class[].class),
                parameterTypes.size()
        );

        Object[] args = new Object[parameterTypes.size() + 3];
        args[0] = fixedArityMethodType;
        args[1] = 0;
        args[2] = returnType;
        for (int i = 0; i < parameterTypes.size(); i++) {
            args[i+3] = parameterTypes.get(i);
        }

        var manyClassloaderArg = invoke(
                MethodHandle.class.descriptorString(),
                types.handle(
                        Opcodes.H_INVOKESTATIC,
                        types.internalName(MethodHandles.class),
                        "filterArguments",
                        MethodType.methodType(MethodHandle.class, MethodHandle.class, int.class, MethodHandle[].class).descriptorString(),
                        false
                ),
                args
        );

        Object[] parameterPermutations = new Object[parameterTypes.size() + 3];
        parameterPermutations[0] = manyClassloaderArg;
        parameterPermutations[1] = types.methodType(MethodType.class.descriptorString(), ClassLoader.class.descriptorString());

        for (int i = 0; i < parameterTypes.size() + 1; i++) {
            parameterPermutations[i + 2] = 0;
        }

        return invoke(
                MethodHandle.class.descriptorString(),
                types.handle(
                        Opcodes.H_INVOKESTATIC,
                        types.internalName(MethodHandles.class),
                        "permuteArguments",
                        MethodType.methodType(MethodHandle.class, MethodHandle.class, MethodType.class, int[].class).descriptorString(),
                        false
                ),
                parameterPermutations
        );
    }

    public TypedDynamic<CD, T> conDynFromFunction(T targetFunction, String targetName) {
        var lookup = invoke(
                MethodHandles.Lookup.class.descriptorString(),
                types.handle(
                        Opcodes.H_INVOKESTATIC,
                        types.internalName(MethodHandles.class),
                        "lookup",
                        MethodType.methodType(MethodHandles.Lookup.class).descriptorString(),
                        false
                )
        );

        var functionCtorCallsite = invoke(
                CallSite.class.descriptorString(),
                types.handle(
                        Opcodes.H_INVOKESTATIC,
                        types.internalName(OpeningMetafactory.class),
                        "invokeKnown",
                        MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, Class.class, int.class).descriptorString(),
                        false
                ),
                lookup,
                "<init>",
                types.methodType(types.descriptor(targetFunction)),
                targetFunction,
                Open.Type.CONSTRUCT.ordinal()
        );

        var functionCtor = invoke(
                MethodHandle.class.descriptorString(),
                types.handle(
                        Opcodes.H_INVOKEVIRTUAL,
                        types.internalName(CallSite.class),
                        "getTarget",
                        MethodType.methodType(MethodHandle.class).descriptorString(),
                        false
                ),
                functionCtorCallsite
        );

        functionCtor = invoke(
                MethodHandle.class.descriptorString(),
                types.handle(
                        Opcodes.H_INVOKEVIRTUAL,
                        types.internalName(MethodHandle.class),
                        "asType",
                        MethodType.methodType(MethodHandle.class, MethodType.class).descriptorString(),
                        false
                ),
                functionCtor,
                types.methodType(ClassProvider.class.descriptorString())
        );

        var fetchClass = invoke(
                MethodHandle.class.descriptorString(),
                types.handle(
                        Opcodes.H_INVOKESTATIC,
                        types.internalName(MethodHandles.class),
                        "collectArguments",
                        MethodType.methodType(MethodHandle.class, MethodHandle.class, int.class, MethodHandle.class).descriptorString(),
                        false
                ),
                types.handle(
                        Opcodes.H_INVOKEINTERFACE,
                        types.internalName(ClassProvider.class),
                        "provide",
                        MethodType.methodType(Class.class, ClassLoader.class, String.class).descriptorString(),
                        true
                ),
                0,
                functionCtor
        );

        return new TypedDynamic<>(invoke(
                MethodHandle.class.descriptorString(),
                types.handle(
                        Opcodes.H_INVOKESTATIC,
                        types.internalName(MethodHandles.class),
                        "insertArguments",
                        MethodType.methodType(MethodHandle.class, MethodHandle.class, int.class, Object[].class).descriptorString(),
                        false
                ),
                fetchClass,
                1,
                targetName == null ? makeNull(types.type(String.class)) : targetName
        ), null);
    }

    public CD makeNull(T type) {
        return types.constantDynamic(
                "nullConstant",
                types.descriptor(type),
                types.handle(
                        Opcodes.H_INVOKESTATIC,
                        types.internalName(ConstantBootstraps.class),
                        "nullConstant",
                        MethodType.methodType(Object.class, MethodHandles.Lookup.class, String.class, Class.class).descriptorString(),
                        false
                )
        );
    }

    public TypedDynamic<CD, T> conDynFromClass(T targetTypeType) {
        Object targetType = targetTypeType;

        if (types.isPrimitiveOrVoid(targetTypeType)) {
            targetType = types.constantDynamic(
                    types.descriptor(targetTypeType),
                    Class.class.descriptorString(),
                    types.handle(
                            Opcodes.H_INVOKESTATIC,
                            types.internalName(ConstantBootstraps.class),
                            "primitiveClass",
                            MethodType.methodType(Class.class, MethodHandles.Lookup.class, String.class, Class.class).descriptorString(),
                            false
                    )
            );
        }

        return new TypedDynamic<>(invoke(
                MethodHandle.class.descriptorString(),
                types.handle(
                        Opcodes.H_INVOKESTATIC,
                        types.internalName(MethodHandles.class),
                        "dropArguments",
                        MethodType.methodType(MethodHandle.class, MethodHandle.class, int.class, Class[].class).descriptorString(),
                        false
                ),
                invoke(
                        MethodHandle.class.descriptorString(),
                        types.handle(
                                Opcodes.H_INVOKESTATIC,
                                types.internalName(MethodHandles.class),
                                "constant",
                                MethodType.methodType(MethodHandle.class, Class.class, Object.class).descriptorString(),
                                false
                        ),
                        types.type(Class.class),
                        targetType
                ),
                0,
                types.type(ClassLoader.class)
        ), targetTypeType);
    }

    public TypedDynamic<CD, T> conDynFromName(String targetName, Function<String, String> classNameRemapper) {
        String targetClassName;

        int arrayLevels = 0;
        while (targetName.startsWith("[")) {
            arrayLevels++;
            targetName = targetName.substring(1);
        }
        if (targetName.length() == 1) {
            if ("ZBCSIFJDV".contains(targetName)) {
                return conDynFromClass(types.type(targetName));
            }
        }
        if (targetName.contains(";")) {
            targetClassName = targetName.substring(1, targetName.length() - 1).replace('/', '.');
        } else {
            targetClassName = targetName;
        }

        var originalTargetClassName = targetClassName;
        targetClassName = classNameRemapper.apply(targetClassName.replace('.', '/')).replace('/', '.');

        var classLookupFromNameAndClassloader = invoke(
                MethodHandle.class.descriptorString(),
                types.handle(
                        Opcodes.H_INVOKESTATIC,
                        types.internalName(MethodHandles.class),
                        "insertArguments",
                        MethodType.methodType(MethodHandle.class, MethodHandle.class, int.class, Object[].class).descriptorString(),
                        false
                ),
                types.handle(
                        Opcodes.H_INVOKESTATIC,
                        types.internalName(Class.class),
                        "forName",
                        MethodType.methodType(Class.class, String.class, boolean.class, ClassLoader.class).descriptorString(),
                        false
                ),
                1,
                booleanConstant(false)
        );

        var remapper = types.handle(
                Opcodes.H_INVOKESTATIC,
                types.internalName(OpeningMetafactory.class),
                "remapClass",
                MethodType.methodType(String.class, String.class, ClassLoader.class).descriptorString(),
                false
        );

        var twoClassloaderArguments = invoke(
                MethodHandle.class.descriptorString(),
                types.handle(
                        Opcodes.H_INVOKESTATIC,
                        types.internalName(MethodHandles.class),
                        "collectArguments",
                        MethodType.methodType(MethodHandle.class, MethodHandle.class, int.class, MethodHandle.class).descriptorString(),
                        false
                ),
                classLookupFromNameAndClassloader,
                0,
                remapper
        );

        var remappingLookup = invoke(
                MethodHandle.class.descriptorString(),
                types.handle(
                        Opcodes.H_INVOKESTATIC,
                        types.internalName(MethodHandles.class),
                        "permuteArguments",
                        MethodType.methodType(MethodHandle.class, MethodHandle.class, MethodType.class, int[].class).descriptorString(),
                        false
                ),
                twoClassloaderArguments,
                types.methodType(Class.class.descriptorString(), String.class.descriptorString(), ClassLoader.class.descriptorString()),
                0,
                1,
                1
        );

        var classValue = invoke(
                MethodHandle.class.descriptorString(),
                types.handle(
                        Opcodes.H_INVOKESTATIC,
                        types.internalName(MethodHandles.class),
                        "insertArguments",
                        MethodType.methodType(MethodHandle.class, MethodHandle.class, int.class, Object[].class).descriptorString(),
                        false
                ),
                remappingLookup,
                0,
                targetClassName
        );

        for (int i = 0; i < arrayLevels; i++) {
            classValue = invoke(
                    MethodHandle.class.descriptorString(),
                    types.handle(
                            Opcodes.H_INVOKESTATIC,
                            types.internalName(MethodHandles.class),
                            "filterReturnValue",
                            MethodType.methodType(MethodHandle.class, MethodHandle.class, MethodHandle.class).descriptorString(),
                            false
                    ),
                    classValue,
                    types.handle(
                            Opcodes.H_INVOKEVIRTUAL,
                            types.internalName(Class.class),
                            "arrayType",
                            MethodType.methodType(Class.class).descriptorString(),
                            false
                    )
            );
        }

        return new TypedDynamic<>(classValue, types.type("L"+originalTargetClassName.replace('.', '/')+";"));
    }
}
