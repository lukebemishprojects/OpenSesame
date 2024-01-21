package dev.lukebemish.opensesame.compile;

import dev.lukebemish.opensesame.annotations.Coerce;
import dev.lukebemish.opensesame.annotations.Open;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

public interface Processor<T, A, M> {
    TypeProvider<T, ?, ?> types();
    ConDynUtils<T, ?, ?> conDynUtils();

    ConDynUtils.TypedDynamic<?, T> typeProviderFromAnnotation(A annotation, Object context, Class<?> annotationType);

    record CoercedDescriptor<T>(List<ConDynUtils.TypedDynamic<?, T>> parameterTypes, ConDynUtils.TypedDynamic<?, T> returnType) {}
    record Opening<T>(T factoryType, Object targetProvider, Object methodTypeProvider, @Nullable T targetType, @Nullable T returnType, List<@Nullable T> parameterTypes, Open.Type type, String name, boolean unsafe) {}
    record ExtendFieldInfo<T>(String name, T type, boolean isFinal, List<String> setters, List<String> getters) {
        public ExtendFieldInfo(String name, T type, boolean isFinal) {
            this(name, type, isFinal, new ArrayList<>(), new ArrayList<>());
        }
    }
    record ExtendOverrideInfo(String name, Object interfaceTypeHandle, String originalName, Object originalTypeHandle) {}
    record ExtendCtorInfo(Object ctorType, Object superCtorType, List<String> fields) {}

    record MethodParameter<T,A>(T type, @Nullable A annotation) {}

    @Nullable A annotation(M method, Class<?> type);
    List<MethodParameter<T,A>> parameters(M method, @Nullable Class<?> type);

    Open.Type type(A annotation);
    @Nullable String name(A annotation);
    boolean unsafe(A annotation);

    T returnType(M method);
    boolean isStatic(M method);
    String methodName(M method);
    T declaringClass(M method);

    default CoercedDescriptor<T> coercedDescriptor(M method) {
        var parameters = parameters(method, Coerce.class);
        String[] parameterDescs = new String[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
            parameterDescs[i] = types().descriptor(parameters.get(i).type());
        }

        T asmDescType = types().methodType(types().descriptor(returnType(method)), parameterDescs);
        ConDynUtils.TypedDynamic<?, T> returnType = conDynUtils().conDynFromClass(types().returnType(asmDescType));
        List<ConDynUtils.TypedDynamic<?, T>> parameterTypes = new ArrayList<>(parameters.size());
        for (var parameter : parameters) {
            parameterTypes.add(
                    conDynUtils().conDynFromClass(parameter.type())
            );
        }

        for (int i = 0; i < parameters.size(); i++) {
            var parameter = parameters.get(i);
            var coercion = parameter.annotation();
            if (coercion != null) {
                parameterTypes.set(i, typeProviderFromAnnotation(coercion, method, Coerce.class));
            }
        }

        var coercion = annotation(method, Coerce.class);
        if (coercion != null) {
            returnType = typeProviderFromAnnotation(coercion, method, Coerce.class);
        }

        return new CoercedDescriptor<>(parameterTypes, returnType);
    }

    default T methodType(M method) {
        var parameters = parameters(method, null);
        String[] parameterDescs = new String[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
            parameterDescs[i] = types().descriptor(parameters.get(i).type());
        }

        return types().methodType(types().descriptor(returnType(method)), parameterDescs);
    }

    default Opening<T> opening(M method) {
        A annotation = annotation(method, Open.class);

        ConDynUtils.TypedDynamic<?, T> targetClassHandle = typeProviderFromAnnotation(annotation, method, Open.class);

        String name = name(annotation);
        if (name == null || name.isEmpty()) {
            name = "$dev$lukebemish$opensesame$$unspecified";
        }
        final Open.Type type = type(annotation);

        CoercedDescriptor<T> descriptor = coercedDescriptor(method);
        List<ConDynUtils.TypedDynamic<?, T>> parameterTypes = descriptor.parameterTypes();
        ConDynUtils.TypedDynamic<?, T> returnType = descriptor.returnType();

        var parameters = parameters(method, Coerce.class);

        T asmDescType = methodType(method);

        if (!isStatic(method)) {
            var takesInstance = (type == Open.Type.GET_INSTANCE || type == Open.Type.SET_INSTANCE || type == Open.Type.VIRTUAL || type == Open.Type.SPECIAL);

            if (!takesInstance) {
                throw new RuntimeException("Method " + methodName(method) + " is not static, but "+Open.class.getSimpleName()+" expects a static context");
            }

            String[] newTypes = new String[parameters.size() + 1];
            for (int i = 0; i < parameters.size(); i++) {
                newTypes[i + 1] = types().descriptor(parameters.get(i).type());
            }
            newTypes[0] = types().descriptor(declaringClass(method));

            asmDescType = types().methodType(
                    types().descriptor(types().returnType(asmDescType)),
                    newTypes
            );

            parameterTypes.add(0, targetClassHandle);
        }

        if (type == Open.Type.CONSTRUCT) {
            returnType = targetClassHandle;
        }

        if (type == Open.Type.ARRAY) {
            returnType = new ConDynUtils.TypedDynamic<>(conDynUtils().invoke(
                    MethodHandle.class.descriptorString(),
                    types().handle(
                            Opcodes.H_INVOKESTATIC,
                            types().internalName(MethodHandles.class),
                            "filterReturnValue",
                            MethodType.methodType(MethodHandle.class, MethodHandle.class, MethodHandle.class).descriptorString(),
                            false
                    ),
                    targetClassHandle.constantDynamic(),
                    types().handle(
                            Opcodes.H_INVOKEVIRTUAL,
                            types().internalName(Class.class),
                            "arrayType",
                            MethodType.methodType(Class.class).descriptorString(),
                            false
                    )
            ), types().makeArray(targetClassHandle.type()));
            if (parameterTypes.size() != 1) {
                throw new RuntimeException("Array constructor must have exactly one parameter");
            }
            parameterTypes.set(0, conDynUtils().conDynFromClass(types().type(int.class)));
        }

        return new Opening<>(
                asmDescType,
                targetClassHandle.constantDynamic(),
                conDynUtils().conDynMethodType(returnType.constantDynamic(), parameterTypes.stream().map(t -> (Object) t.constantDynamic()).toList()),
                targetClassHandle.type(),
                returnType.type(),
                parameterTypes.stream().map(ConDynUtils.TypedDynamic::type).toList(),
                type,
                name,
                unsafe(annotation)
        );
    }
}
