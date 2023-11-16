package dev.lukebemish.opensesame.compile.groovy

import dev.lukebemish.opensesame.compile.ConDynUtils
import dev.lukebemish.opensesame.compile.TypeProvider
import groovy.transform.CompileStatic
import groovyjarjarasm.asm.ConstantDynamic
import groovyjarjarasm.asm.Handle
import groovyjarjarasm.asm.Type

@CompileStatic
class GroovyASMTypeProvider implements TypeProvider<Type, ConstantDynamic, Handle> {
    public static final GroovyASMTypeProvider INSTANCE = new GroovyASMTypeProvider()
    public static final ConDynUtils<Type, ConstantDynamic, Handle> CON_DYN_UTILS = new ConDynUtils<>(INSTANCE)

    private GroovyASMTypeProvider() {}

    @Override
    Handle handle(int tag, String owner, String name, String descriptor, boolean isInterface) {
        return new Handle(
                tag,
                owner,
                name,
                descriptor,
                isInterface
        )
    }

    @Override
    ConstantDynamic constantDynamic(String name, String descriptor, Handle bootstrapMethod, Object... bootstrapMethodArguments) {
        return new ConstantDynamic(
                name,
                descriptor,
                bootstrapMethod,
                bootstrapMethodArguments
        )
    }

    @Override
    Type type(Class<?> clazz) {
        return Type.getType(clazz)
    }

    @Override
    Type type(String descriptor) {
        return Type.getType(descriptor)
    }

    @Override
    String descriptor(Type type) {
        return type.getDescriptor()
    }

    @Override
    String internalName(Type type) {
        return type.internalName
    }

    @Override
    boolean isPrimitiveOrVoid(Type type) {
        return type.sort <= Type.DOUBLE
    }

    @Override
    Type returnType(Type methodType) {
        return methodType.returnType
    }

    @Override
    Type[] parameterTypes(Type methodType) {
        return methodType.argumentTypes
    }
}
