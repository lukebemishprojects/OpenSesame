package dev.lukebemish.opensesame.compile;

public interface TypeProvider<T, CD, H> {
    H handle(int tag, String owner, String name, String descriptor, boolean isInterface);
    CD constantDynamic(String name, String descriptor, H bootstrapMethod, Object... bootstrapMethodArguments);
    T type(Class<?> clazz);
    T type(String descriptor);
    default T methodType(String returnDescriptor, String... parameterDescriptors) {
        return type("("+String.join("", parameterDescriptors)+")"+returnDescriptor);
    }
    String descriptor(T type);
    default String internalName(Class<?> clazz) {
        return internalName(clazz.getName());
    }
    default String internalName(String name) {
        return name.replace('.', '/');
    }
    String internalName(T type);

    boolean isPrimitiveOrVoid(T type);

    T returnType(T methodType);
    T[] parameterTypes(T methodType);

    default T makeArray(T type) {
        return type("["+descriptor(type));
    }
}
