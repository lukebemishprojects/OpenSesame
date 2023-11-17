package dev.lukebemish.opensesame.compile.groovy;

import dev.lukebemish.opensesame.compile.ConDynUtils;
import dev.lukebemish.opensesame.compile.TypeProvider;
import groovyjarjarasm.asm.ConstantDynamic;
import groovyjarjarasm.asm.Handle;
import groovyjarjarasm.asm.Type;

class GroovyASMTypeProvider implements TypeProvider<Type, ConstantDynamic, Handle> {
    public static final GroovyASMTypeProvider INSTANCE = new GroovyASMTypeProvider();
    public static final ConDynUtils<Type, ConstantDynamic, Handle> CON_DYN_UTILS = new ConDynUtils<>(INSTANCE);

    private GroovyASMTypeProvider() {}

    @Override
    public Handle handle(int tag, String owner, String name, String descriptor, boolean isInterface) {
        return new Handle(
                tag,
                owner,
                name,
                descriptor,
                isInterface
        );
    }

    @Override
    public ConstantDynamic constantDynamic(String name, String descriptor, Handle bootstrapMethod, Object... bootstrapMethodArguments) {
        return new ConstantDynamic(
                name,
                descriptor,
                bootstrapMethod,
                bootstrapMethodArguments
        );
    }

    @Override
    public Type type(Class<?> clazz) {
        return Type.getType(clazz);
    }

    @Override
    public Type type(String descriptor) {
        return Type.getType(descriptor);
    }

    @Override
    public String descriptor(Type type) {
        return type.getDescriptor();
    }

    @Override
    public String internalName(Type type) {
        return type.getInternalName();
    }

    @Override
    public boolean isPrimitiveOrVoid(Type type) {
        return type.getSort() <= Type.DOUBLE;
    }

    @Override
    public Type returnType(Type methodType) {
        return methodType.getReturnType();
    }

    @Override
    public Type[] parameterTypes(Type methodType) {
        return methodType.getArgumentTypes();
    }
}
