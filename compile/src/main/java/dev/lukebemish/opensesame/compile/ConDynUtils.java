package dev.lukebemish.opensesame.compile;

import dev.lukebemish.opensesame.runtime.OpeningMetafactory;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.function.Function;

public class ConDynUtils<T, CD, H> {
    private final TypeProvider<T, CD, H> typeProvider;

    public ConDynUtils(TypeProvider<T, CD, H> typeProvider) {
        this.typeProvider = typeProvider;
    }

    public CD invoke(String descriptor, Object handle, Object... args) {
        Object[] fullArgs = new Object[args.length+1];
        System.arraycopy(args, 0, fullArgs, 1, args.length);
        fullArgs[0] = handle;
        return typeProvider.constantDynamic(
                "invoke",
                descriptor,
                typeProvider.handle(
                        Opcodes.H_INVOKESTATIC,
                        typeProvider.internalName(ConstantBootstraps.class),
                        "invoke",
                        MethodType.methodType(Object.class, MethodHandles.Lookup.class, String.class, Class.class, MethodHandle.class, Object[].class).descriptorString(),
                        false
                ),
                fullArgs
        );
    }

    public CD booleanConstant(boolean bool) {
        return typeProvider.constantDynamic(
                // booleans are fucky in ConstantDynamics. Here's an alternative...
                bool ? "TRUE" : "FALSE",
                Boolean.class.descriptorString(),
                typeProvider.handle(
                        Opcodes.H_INVOKESTATIC,
                        typeProvider.internalName(ConstantBootstraps.class),
                        "getStaticFinal",
                        MethodType.methodType(Object.class, MethodHandles.Lookup.class, String.class, Class.class, Class.class).descriptorString(),
                        false
                ),
                typeProvider.type(Boolean.class)
        );
    }

    public CD conDynMethodType(Object returnType, List<Object> parameterTypes) {
        var fixedArityMethodType = invoke(
                MethodHandle.class.descriptorString(),
                typeProvider.handle(
                    Opcodes.H_INVOKEVIRTUAL,
                    typeProvider.internalName(MethodHandle.class),
                    "asCollector",
                    MethodType.methodType(MethodHandle.class, Class.class, int.class).descriptorString(),
                    false
                ),
                typeProvider.handle(
                    Opcodes.H_INVOKESTATIC,
                    typeProvider.internalName(MethodType.class),
                    "methodType",
                    MethodType.methodType(MethodType.class, Class.class, Class[].class).descriptorString(),
                    false
                ),
                typeProvider.type(Class[].class),
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
                typeProvider.handle(
                        Opcodes.H_INVOKESTATIC,
                        typeProvider.internalName(MethodHandles.class),
                        "filterArguments",
                        MethodType.methodType(MethodHandle.class, MethodHandle.class, int.class, MethodHandle[].class).descriptorString(),
                        false
                ),
                args
        );

        Object[] parameterPermutations = new Object[parameterTypes.size() + 3];
        parameterPermutations[0] = manyClassloaderArg;
        parameterPermutations[1] = typeProvider.methodType(MethodType.class.descriptorString(), ClassLoader.class.descriptorString());

        for (int i = 0; i < parameterTypes.size() + 1; i++) {
            parameterPermutations[i + 2] = 0;
        }

        return invoke(
                MethodHandle.class.descriptorString(),
                typeProvider.handle(
                        Opcodes.H_INVOKESTATIC,
                        typeProvider.internalName(MethodHandles.class),
                        "permuteArguments",
                        MethodType.methodType(MethodHandle.class, MethodHandle.class, MethodType.class, int[].class).descriptorString(),
                        false
                ),
                parameterPermutations
        );
    }

    public CD conDynFromFunction(String targetFunctionClassName) {
        String internalName = typeProvider.internalName(targetFunctionClassName);

        var functionCtor = typeProvider.handle(
                Opcodes.H_NEWINVOKESPECIAL,
                internalName,
                "<init>",
                MethodType.methodType(void.class).descriptorString(),
                false
        );

        var asFunction = invoke(
                MethodHandle.class.descriptorString(),
                typeProvider.handle(
                        Opcodes.H_INVOKEVIRTUAL,
                        typeProvider.internalName(MethodHandle.class),
                        "asType",
                        MethodType.methodType(MethodHandle.class, MethodType.class).descriptorString(),
                        false
                ),
                functionCtor,
                typeProvider.methodType(Function.class.descriptorString())
        );

        var fetchClass = invoke(
                MethodHandle.class.descriptorString(),

                typeProvider.handle(
                        Opcodes.H_INVOKESTATIC,
                        typeProvider.internalName(MethodHandles.class),
                        "collectArguments",
                        MethodType.methodType(MethodHandle.class, MethodHandle.class, int.class, MethodHandle.class).descriptorString(),
                        false
                ),
                typeProvider.handle(
                        Opcodes.H_INVOKEINTERFACE,
                        typeProvider.internalName(Function.class),
                        "apply",
                        MethodType.methodType(Object.class, Object.class).descriptorString(),
                        true
                ),
                0,
                asFunction
        );

        return invoke(
                MethodHandle.class.descriptorString(),
                typeProvider.handle(
                        Opcodes.H_INVOKEVIRTUAL,
                        typeProvider.internalName(MethodHandle.class),
                        "asType",
                        MethodType.methodType(MethodHandle.class, MethodType.class).descriptorString(),
                        false
                ),
                fetchClass,
                typeProvider.methodType(Class.class.descriptorString(), ClassLoader.class.descriptorString())
        );
    }

    public CD conDynFromClass(String targetClassName) {
        String internalName = typeProvider.internalName(targetClassName);
        T targetTypeType = typeProvider.typeFromInternalName(internalName);
        Object targetType = targetTypeType;

        if (typeProvider.isPrimitiveOrVoid(targetTypeType)) {
            targetType = typeProvider.constantDynamic(
                    typeProvider.descriptor(targetTypeType),
                    Class.class.descriptorString(),
                    typeProvider.handle(
                            Opcodes.H_INVOKESTATIC,
                            typeProvider.internalName(ConstantBootstraps.class),
                            "primitiveClass",
                            MethodType.methodType(Class.class, MethodHandles.Lookup.class, String.class, Class.class).descriptorString(),
                            false
                    )
            );
        }

        return invoke(
                MethodHandle.class.descriptorString(),
                typeProvider.handle(
                        Opcodes.H_INVOKESTATIC,
                        typeProvider.internalName(MethodHandles.class),
                        "dropArguments",
                        MethodType.methodType(MethodHandle.class, MethodHandle.class, int.class, Class[].class).descriptorString(),
                        false
                ),
                invoke(
                        MethodHandle.class.descriptorString(),
                        typeProvider.handle(
                                Opcodes.H_INVOKESTATIC,
                                typeProvider.internalName(MethodHandles.class),
                                "constant",
                                MethodType.methodType(MethodHandle.class, Class.class, Object.class).descriptorString(),
                                false
                        ),
                        typeProvider.type(Class.class),
                        targetType
                ),
                0,
                typeProvider.type(ClassLoader.class)
        );
    }

    public CD conDynFromName(String targetName) {
        var classLookupFromNameAndClassloader = invoke(
                MethodHandle.class.descriptorString(),
                typeProvider.handle(
                        Opcodes.H_INVOKESTATIC,
                        typeProvider.internalName(MethodHandles.class),
                        "insertArguments",
                        MethodType.methodType(MethodHandle.class, MethodHandle.class, int.class, Object[].class).descriptorString(),
                        false
                ),
                typeProvider.handle(
                        Opcodes.H_INVOKESTATIC,
                        typeProvider.internalName(Class.class),
                        "forName",
                        MethodType.methodType(Class.class, String.class, boolean.class, ClassLoader.class).descriptorString(),
                        false
                ),
                1,
                booleanConstant(false)
        );

        var remapper = typeProvider.handle(
                Opcodes.H_INVOKESTATIC,
                typeProvider.internalName(OpeningMetafactory.class),
                "remapClass",
                MethodType.methodType(String.class, String.class, ClassLoader.class).descriptorString(),
                false
        );

        var twoClassloaderArguments = invoke(
                MethodHandle.class.descriptorString(),
                typeProvider.handle(
                        Opcodes.H_INVOKESTATIC,
                        typeProvider.internalName(MethodHandles.class),
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
                typeProvider.handle(
                        Opcodes.H_INVOKESTATIC,
                        typeProvider.internalName(MethodHandles.class),
                        "permuteArguments",
                        MethodType.methodType(MethodHandle.class, MethodHandle.class, MethodType.class, int[].class).descriptorString(),
                        false
                ),
                twoClassloaderArguments,
                typeProvider.methodType(Class.class.descriptorString(), String.class.descriptorString(), ClassLoader.class.descriptorString()),
                0,
                1,
                1
        );

        return invoke(
                MethodHandle.class.descriptorString(),
                typeProvider.handle(
                        Opcodes.H_INVOKESTATIC,
                        typeProvider.internalName(MethodHandles.class),
                        "insertArguments",
                        MethodType.methodType(MethodHandle.class, MethodHandle.class, int.class, Object[].class).descriptorString(),
                        false
                ),
                remappingLookup,
                0,
                targetName
        );
    }
}
