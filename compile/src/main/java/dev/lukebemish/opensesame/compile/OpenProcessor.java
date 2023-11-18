package dev.lukebemish.opensesame.compile;

import dev.lukebemish.opensesame.annotations.Coerce;
import dev.lukebemish.opensesame.annotations.Open;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;

public interface OpenProcessor<T, A, M> {
    TypeProvider<T, ?, ?> types();
    ConDynUtils<T, ?, ?> conDynUtils();

    Object typeProviderFromAnnotation(A annotation, M method, Class<?> annotationType);

    record Opening<T>(T factoryType, Object targetProvider, Object methodTypeProvider, Open.Type type, String name, boolean unsafe) {}

    record MethodParameter<T,A>(T type, @Nullable A annotation) {}

    @Nullable A annotation(M method, Class<?> type);
    List<MethodParameter<T,A>> parameters(M method, @Nullable Class<?> type);

    Open.Type type(A annotation);
    String name(A annotation);
    boolean unsafe(A annotation);

    T returnType(M method);
    boolean isStatic(M method);
    String methodName(M method);
    T declaringClass(M method);

    default Opening<T> opening(M method) {
        A annotation = annotation(method, Open.class);

        Object targetClassHandle = typeProviderFromAnnotation(annotation, method, Open.class);

        final String name = name(annotation);
        final Open.Type type = type(annotation);

        var parameters = parameters(method, Coerce.class);
        String[] parameterDescs = new String[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
            parameterDescs[i] = types().descriptor(parameters.get(i).type());
        }

        T asmDescType = types().methodType(types().descriptor(returnType(method)), parameterDescs);
        Object returnType = conDynUtils().conDynFromClass(types().returnType(asmDescType));
        List<Object> parameterTypes = new ArrayList<>(parameters.size());
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

        var coercion = annotation(method, Coerce.class);
        if (coercion != null) {
            returnType = typeProviderFromAnnotation(coercion, method, Coerce.class);
        }

        if (type == Open.Type.CONSTRUCT) {
            returnType = targetClassHandle;
        }

        if (type == Open.Type.ARRAY) {
            returnType = conDynUtils().invoke(
                    MethodHandle.class.descriptorString(),
                    types().handle(
                            Opcodes.H_INVOKEVIRTUAL,
                            types().internalName(Class.class),
                            "arrayType",
                            types().descriptor(types().methodType(Class.class.descriptorString())),
                            false
                    ),
                    targetClassHandle
            );
            if (parameterTypes.size() != 1) {
                throw new RuntimeException("Array constructor must have exactly one parameter");
            }
            parameterTypes.set(0, conDynUtils().conDynFromClass(types().type(int.class)));
        }

        return new Opening<>(
                asmDescType,
                targetClassHandle,
                conDynUtils().conDynMethodType(returnType, parameterTypes),
                type,
                name,
                unsafe(annotation)
        );
    }
}
