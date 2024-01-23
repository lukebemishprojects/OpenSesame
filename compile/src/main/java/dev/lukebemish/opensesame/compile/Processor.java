package dev.lukebemish.opensesame.compile;

import dev.lukebemish.opensesame.annotations.Coerce;
import dev.lukebemish.opensesame.annotations.Open;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    record ExtendOverrideInfo<T>(String interfaceName, ConDynUtils.TypedDynamic<?, T> interfaceReturn, List<ConDynUtils.TypedDynamic<?, T>> interfaceParams, String originalName, ConDynUtils.TypedDynamic<?, T> originalReturn, List<ConDynUtils.TypedDynamic<?, T>> originalParams) { }
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

    interface ClassAccumulator {
        FieldMaker visitField(int access, String name, String descriptor, String signature, Object value);
        MethodMaker visitMethod(int access, String name, String descriptor, String signature, String[] exceptions);
    }

    interface FieldMaker {
        void visitEnd();
    }

    interface MethodMaker {
        void visitEnd();
        void visitCode();
        void visitInsn(int opcode);
        void visitVarInsn(int opcode, int var);
        void visitFieldInsn(int opcode, String owner, String name, String descriptor);
        void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface);
        void visitLdcInsn(Object value);
        void visitMaxs(int maxStack, int maxLocals);

        void visitTypeInsn(int opcode, String type);
    }

    interface MethodNameMapper<T> {
        String remapMethodName(T type, String name, T returnType, List<T> parameterTypes);
    }

    String EXTEND_INFO_GENERATED = "$$dev$lukebemish$opensesame$$extendInfo";
    String EXTEND_GENERATED_CLASS = "$$dev$lukebemish$opensesame$$extendGENERATED";

    default void extensionBytecode(ClassAccumulator visitor, List<ExtendCtorInfo> ctors, ConDynUtils.TypedDynamic<?, T> extendTargetClassHandle, Map<String, ExtendFieldInfo<T>> fields, boolean generateClassInit, List<ExtendOverrideInfo<T>> overrides, T originalExtensionType, T holdingType, MethodNameMapper<T> mapper) {
        for (var field : fields.values()) {
            if (field.isFinal() && !field.setters().isEmpty()) {
                throw new RuntimeException("@Field "+field.name()+" is final, but has setters");
            }
        }
        var classHolderField = visitor.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_FINAL, EXTEND_GENERATED_CLASS, Class.class.arrayType().descriptorString(), null, null);
        classHolderField.visitEnd();
        var setter = visitor.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, EXTEND_GENERATED_CLASS, MethodType.methodType(void.class, Class.class).descriptorString(), null, null);
        setter.visitCode();
        setter.visitFieldInsn(Opcodes.GETSTATIC, types().internalName(holdingType), EXTEND_GENERATED_CLASS, Class.class.arrayType().descriptorString());
        setter.visitInsn(Opcodes.DUP);
        setter.visitInsn(Opcodes.MONITORENTER);
        setter.visitInsn(Opcodes.ICONST_0);
        setter.visitVarInsn(Opcodes.ALOAD, 0);
        setter.visitInsn(Opcodes.AASTORE);
        setter.visitFieldInsn(Opcodes.GETSTATIC, types().internalName(holdingType), EXTEND_GENERATED_CLASS, Class.class.arrayType().descriptorString());
        setter.visitInsn(Opcodes.MONITOREXIT);
        setter.visitInsn(Opcodes.RETURN);
        setter.visitMaxs(3, 1);
        setter.visitEnd();
        var getter = visitor.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, EXTEND_GENERATED_CLASS, MethodType.methodType(Class.class).descriptorString(), null, null);
        getter.visitCode();
        getter.visitFieldInsn(Opcodes.GETSTATIC, types().internalName(holdingType), EXTEND_GENERATED_CLASS, Class.class.arrayType().descriptorString());
        getter.visitInsn(Opcodes.DUP);
        getter.visitInsn(Opcodes.MONITORENTER);
        getter.visitInsn(Opcodes.ICONST_0);
        getter.visitInsn(Opcodes.AALOAD);
        getter.visitFieldInsn(Opcodes.GETSTATIC, types().internalName(holdingType), EXTEND_GENERATED_CLASS, Class.class.arrayType().descriptorString());
        getter.visitInsn(Opcodes.MONITOREXIT);
        getter.visitInsn(Opcodes.ARETURN);
        getter.visitMaxs(3, 0);
        getter.visitEnd();
        var info = visitor.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, EXTEND_INFO_GENERATED, MethodType.methodType(List.class, ClassLoader.class).descriptorString(), null, null);
        info.visitCode();

        // 3 - ctor
        // 4 - override
        // 5 - field
        //   +2 - building arraylist, dup arraylist
        // +1 - root arraylist
        // +1 - building arraylist
        // +2 - dup root, dup building
        int maxStack = 11;

        newArrayList(info);

        // fields:
        info.visitInsn(Opcodes.DUP);
        newArrayList(info);
        // add all the fields
        for (var field : fields.values()) {
            info.visitInsn(Opcodes.DUP);
            // Field list format: String name, Class<?> fieldType, Boolean isFinal, List<String> setters, List<String> getters
            info.visitLdcInsn(field.name());
            info.visitLdcInsn(field.type());
            info.visitLdcInsn(field.isFinal());
            info.visitMethodInsn(Opcodes.INVOKESTATIC, types().internalName(Boolean.class), "valueOf", MethodType.methodType(Boolean.class, boolean.class).descriptorString(), false);

            // add setters through ArrayList
            newArrayList(info);
            for (var sName : field.setters()) {
                info.visitInsn(Opcodes.DUP);
                info.visitLdcInsn(sName);
                addToList(info);
            }

            // add getters through ArrayList
            newArrayList(info);
            for (var gName : field.getters()) {
                info.visitInsn(Opcodes.DUP);
                info.visitLdcInsn(gName);
                addToList(info);
            }

            info.visitMethodInsn(Opcodes.INVOKESTATIC, types().internalName(List.class), "of", MethodType.methodType(List.class, Object.class, Object.class, Object.class, Object.class, Object.class).descriptorString(), true);
            addToList(info);
        }
        addToList(info);

        // overrides:
        info.visitInsn(Opcodes.DUP);
        newArrayList(info);
        for (var override : overrides) {
            info.visitInsn(Opcodes.DUP);
            var interfaceName = mapper.remapMethodName(originalExtensionType, override.interfaceName(), override.interfaceReturn().type(), override.interfaceParams().stream().map(ConDynUtils.TypedDynamic::type).toList());
            info.visitLdcInsn(interfaceName);
            var interfaceType = conDynUtils().conDynMethodType(override.interfaceReturn().constantDynamic(), override.interfaceParams().stream().<Object>map(ConDynUtils.TypedDynamic::constantDynamic).toList());
            info.visitLdcInsn(interfaceType);
            var originalName = mapper.remapMethodName(extendTargetClassHandle.type(), override.originalName(), override.originalReturn().type(), override.originalParams().stream().map(ConDynUtils.TypedDynamic::type).toList());
            info.visitLdcInsn(originalName);
            var originalType = conDynUtils().conDynMethodType(override.originalReturn().constantDynamic(), override.originalParams().stream().<Object>map(ConDynUtils.TypedDynamic::constantDynamic).toList());
            info.visitLdcInsn(originalType);
            info.visitMethodInsn(Opcodes.INVOKESTATIC, types().internalName(List.class), "of", MethodType.methodType(List.class, Object.class, Object.class, Object.class, Object.class).descriptorString(), true);
            addToList(info);
        }
        addToList(info);

        // ctors:
        info.visitInsn(Opcodes.DUP);
        newArrayList(info);
        for (var ctor : ctors) {
            info.visitInsn(Opcodes.DUP);
            info.visitLdcInsn(ctor.ctorType());
            info.visitLdcInsn(ctor.superCtorType());

            newArrayList(info);
            for (var field : ctor.fields()) {
                info.visitInsn(Opcodes.DUP);
                info.visitLdcInsn(field);
                addToList(info);
            }

            info.visitMethodInsn(Opcodes.INVOKESTATIC, types().internalName(List.class), "of", MethodType.methodType(List.class, Object.class, Object.class, Object.class).descriptorString(), true);
            addToList(info);
        }
        addToList(info);

        info.visitInsn(Opcodes.ARETURN);
        info.visitMaxs(maxStack, 1);
        info.visitEnd();
        if (generateClassInit) {
            var clinit = visitor.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            clinit.visitCode();
            clinit.visitInsn(Opcodes.RETURN);
            clinit.visitMaxs(1, 0);
            clinit.visitEnd();
        }
    }

    default void extensionClassInitSetup(MethodMaker methodMaker, T type) {
        methodMaker.visitInsn(Opcodes.ICONST_1);
        methodMaker.visitTypeInsn(Opcodes.ANEWARRAY, types().internalName(Class.class));
        methodMaker.visitFieldInsn(Opcodes.PUTSTATIC, types().internalName(type), EXTEND_GENERATED_CLASS, Class.class.arrayType().descriptorString());
    }

    private void newArrayList(MethodMaker info) {
        info.visitTypeInsn(Opcodes.NEW, types().internalName(ArrayList.class));
        info.visitInsn(Opcodes.DUP);
        info.visitMethodInsn(Opcodes.INVOKESPECIAL, types().internalName(ArrayList.class), "<init>", "()V", false);
    }

    private void addToList(MethodMaker info) {
        info.visitMethodInsn(Opcodes.INVOKEINTERFACE, types().internalName(List.class), "add", MethodType.methodType(boolean.class, Object.class).descriptorString(), true);
        info.visitInsn(Opcodes.POP);
    }
}
