package dev.lukebemish.opensesame.runtime;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

final class MinimalConDynUtils {
    private MinimalConDynUtils() {}

    static ConstantDynamic conDynFromClass(Type targetTypeType) {
        Object targetType = targetTypeType;
        if (targetTypeType.getSort() <= Type.DOUBLE) {
            targetType = new ConstantDynamic(
                    targetTypeType.getDescriptor(),
                    Class.class.descriptorString(),
                    new Handle(
                            Opcodes.H_INVOKESTATIC,
                            Type.getInternalName(ConstantBootstraps.class),
                            "primitiveClass",
                            MethodType.methodType(Class.class, MethodHandles.Lookup.class, String.class, Class.class).descriptorString(),
                            false
                    )
            );
        }

        return invoke(
                MethodHandle.class.descriptorString(),
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        Type.getInternalName(MethodHandles.class),
                        "dropArguments",
                        MethodType.methodType(MethodHandle.class, MethodHandle.class, int.class, Class[].class).descriptorString(),
                        false
                ),
                invoke(
                        MethodHandle.class.descriptorString(),
                        new Handle(
                                Opcodes.H_INVOKESTATIC,
                                Type.getInternalName(MethodHandles.class),
                                "constant",
                                MethodType.methodType(MethodHandle.class, Class.class, Object.class).descriptorString(),
                                false
                        ),
                        Type.getType(Class.class),
                        targetType
                ),
                0,
                Type.getType(ClassLoader.class)
        );
    }

    static ConstantDynamic invoke(String descriptor, Object handle, Object... args) {
        Object[] fullArgs = new Object[args.length+1];
        System.arraycopy(args, 0, fullArgs, 1, args.length);
        fullArgs[0] = handle;
        return new ConstantDynamic(
                "invoke",
                descriptor,
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        Type.getInternalName(ConstantBootstraps.class),
                        "invoke",
                        MethodType.methodType(Object.class, MethodHandles.Lookup.class, String.class, Class.class, MethodHandle.class, Object[].class).descriptorString(),
                        false
                ),
                fullArgs
        );
    }

    static Object conDynMethodType(Object returnType, List<?> parameterTypes) {
        var fixedArityMethodType = invoke(
                MethodHandle.class.descriptorString(),
                new Handle(
                        Opcodes.H_INVOKEVIRTUAL,
                        Type.getInternalName(MethodHandle.class),
                        "asCollector",
                        MethodType.methodType(MethodHandle.class, Class.class, int.class).descriptorString(),
                        false
                ),
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        Type.getInternalName(MethodType.class),
                        "methodType",
                        MethodType.methodType(MethodType.class, Class.class, Class[].class).descriptorString(),
                        false
                ),
                Type.getType(Class[].class),
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
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        Type.getInternalName(MethodHandles.class),
                        "filterArguments",
                        MethodType.methodType(MethodHandle.class, MethodHandle.class, int.class, MethodHandle[].class).descriptorString(),
                        false
                ),
                args
        );

        Object[] parameterPermutations = new Object[parameterTypes.size() + 3];
        parameterPermutations[0] = manyClassloaderArg;
        parameterPermutations[1] = Type.getMethodType(Type.getType(MethodType.class), Type.getType(ClassLoader.class));

        for (int i = 0; i < parameterTypes.size() + 1; i++) {
            parameterPermutations[i + 2] = 0;
        }

        return invoke(
                MethodHandle.class.descriptorString(),
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        Type.getInternalName(MethodHandles.class),
                        "permuteArguments",
                        MethodType.methodType(MethodHandle.class, MethodHandle.class, MethodType.class, int[].class).descriptorString(),
                        false
                ),
                parameterPermutations
        );
    }
}
