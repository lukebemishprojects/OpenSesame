package dev.lukebemish.opensesame.runtime;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.invoke.*;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.*;

/**
 * A series of metafactories that generate call sites for otherwise inaccessible members of other classes.
 */
public final class OpeningMetafactory {

    /**
     * Invoke a static method
     */
    public static final int STATIC_TYPE = 0;
    /**
     * Invoke an instance method virtually
     */
    public static final int VIRTUAL_TYPE = 1;
    /**
     * Invoke an instance method non-virtually
     */
    public static final int SPECIAL_TYPE = 2;
    /**
     * Get a static field
     */
    public static final int STATIC_GET_TYPE = 3;
    /**
     * Get an instance field
     */
    public static final int INSTANCE_GET_TYPE = 4;
    /**
     * Set a static field
     */
    public static final int STATIC_SET_TYPE = 5;
    /**
     * Set an instance field
     */
    public static final int INSTANCE_SET_TYPE = 6;
    /**
     * Invoke a constructor
     */
    public static final int CONSTRUCT_TYPE = 7;
    /**
     * Create an array
     */
    public static final int ARRAY_TYPE = 8;

    /**
     * Creates a call site of a member of a class in a "safe" fashion (obeying module boundaries).
     * @param caller the lookup of the caller
     * @param targetMethodName the name of the member to open
     * @param factoryType the type of the call site to create
     * @param classProvider a method handle that takes a classloader and returns the class to open
     * @param accessTypeProvider a method handle that takes a classloader and returns a type matching the member to
     *                           open, which should be convertible to the {@code factoryType}
     * @param type the type of member to look up, as determined by the constants in this class
     * @return the call site
     * @throws OpeningException if the member could not be found or could not be opened
     */
    @SuppressWarnings("unused")
    public static CallSite invoke(MethodHandles.Lookup caller, String targetMethodName, MethodType factoryType, MethodHandle classProvider, MethodHandle accessTypeProvider, int type) {
        return invoke0(caller, targetMethodName, factoryType, classProvider, accessTypeProvider, type, false);
    }

    /**
     * Creates a call site of a member of a class in an unsafe fashion, breaking through module boundaries
     * @param caller the lookup of the caller
     * @param targetMethodName the name of the member to open
     * @param factoryType the type of the call site to create
     * @param classProvider a method handle that takes a classloader and returns the class to open
     * @param accessTypeProvider a method handle that takes a classloader and returns a type matching the member to
     *                           open, which should be convertible to the {@code factoryType}
     * @param type the type of member to look up, as determined by the constants in this class
     * @return the call site
     * @throws OpeningException if the member could not be found or could not be opened
     */
    @SuppressWarnings("unused")
    public static CallSite invokeUnsafe(MethodHandles.Lookup caller, String targetMethodName, MethodType factoryType, MethodHandle classProvider, MethodHandle accessTypeProvider, int type) {
        try {
            return invoke0(caller, targetMethodName, factoryType, classProvider, accessTypeProvider, type, true);
        } catch (RuntimeException e) {
            var exception = new OpeningException(e);
            if (LOOKUP_PROVIDER_EXCEPTION != null) {
                exception.addSuppressed(LOOKUP_PROVIDER_EXCEPTION);
            }
            throw exception;
        }
    }

    /**
     * Creates a call site of a class known ahead of time in a "safe" fashion (obeying module boundaries).
     * @param caller the lookup of the caller
     * @param name the name of the member to open
     * @param factoryType the type of the call site
     * @param holdingClass the class that holds the member
     * @param type the type of member to look up, as determined by the constants in this class
     * @return the call site
     * @throws OpeningException if the member could not be found or could not be opened
     */
    @SuppressWarnings("unused")
    public static CallSite invokeKnown(MethodHandles.Lookup caller, String name, MethodType factoryType, Class<?> holdingClass, int type) {
        return invoke1(caller, name, factoryType, factoryType, holdingClass, type, false);
    }

    /**
     * Creates a call site of a class known ahead of time in an unsafe fashion, breaking through module boundaries
     * @param caller the lookup of the caller
     * @param name the name of the member to open
     * @param factoryType the type of the call site
     * @param holdingClass the class that holds the member
     * @param type the type of member to look up, as determined by the constants in this class
     * @return the call site
     * @throws OpeningException if the member could not be found or could not be opened
     */
    @SuppressWarnings("unused")
    public static CallSite invokeKnownUnsafe(MethodHandles.Lookup caller, String name, MethodType factoryType, Class<?> holdingClass, int type) {
        return invoke1(caller, name, factoryType, factoryType, holdingClass, type, true);
    }
    private OpeningMetafactory() {}

    private static final class ClassLoaderKey extends WeakReference<ClassLoader> {
        final int hashCode;

        public ClassLoaderKey(ClassLoader referent, ReferenceQueue<? super ClassLoader> q) {
            super(referent, q);
            this.hashCode = System.identityHashCode(referent);
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof ClassLoaderKey key) && hashCode == key.hashCode && get() == key.get();
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    private static final Map<ClassLoaderKey, List<RuntimeRemapper>> REMAPPER_LOOKUP = new HashMap<>();
    private static final ReferenceQueue<ClassLoader> REMAPPER_LOOKUP_QUEUE = new ReferenceQueue<>();
    private static final LookupProvider LOOKUP_PROVIDER_UNSAFE;
    private static final LookupProvider LOOKUP_PROVIDER_SAFE = new LookupProviderFallback();
    private static final Exception LOOKUP_PROVIDER_EXCEPTION;

    static {
        Exception LOOKUP_PROVIDER_EXCEPTION1;
        LookupProvider LOOKUP_PROVIDER1;
        try {
            LOOKUP_PROVIDER1 = new LookupProviderUnsafe();
            LOOKUP_PROVIDER_EXCEPTION1 = null;
        } catch (Exception e) {
            LOOKUP_PROVIDER_EXCEPTION1 = e;
            LOOKUP_PROVIDER1 = new LookupProviderFallback();
        }
        LOOKUP_PROVIDER_EXCEPTION = LOOKUP_PROVIDER_EXCEPTION1;
        LOOKUP_PROVIDER_UNSAFE = LOOKUP_PROVIDER1;
    }

    private synchronized static List<RuntimeRemapper> getRemapper(ClassLoader classLoader) {
        ClassLoaderKey ref;
        while ((ref = (ClassLoaderKey) REMAPPER_LOOKUP_QUEUE.poll()) != null) {
            REMAPPER_LOOKUP.remove(ref);
        }
        return REMAPPER_LOOKUP.computeIfAbsent(new ClassLoaderKey(classLoader, REMAPPER_LOOKUP_QUEUE), k ->
                ServiceLoader.load(RuntimeRemapper.class, classLoader).stream().map(ServiceLoader.Provider::get).toList()
        );
    }

    private static CallSite invoke0(MethodHandles.Lookup caller, String targetMethodName, MethodType factoryType, MethodHandle classProvider, MethodHandle accessTypeProvider, int type, boolean unsafe) {
        Class<?> holdingClass;
        MethodType accessType;
        try {
            holdingClass = (Class<?>) classProvider.invoke(caller.lookupClass().getClassLoader());
            accessType = (MethodType) accessTypeProvider.invoke(caller.lookupClass().getClassLoader());
        } catch (Throwable throwable) {
            throw new OpeningException(throwable);
        }
        return invoke1(caller, targetMethodName, factoryType, accessType, holdingClass, type, unsafe);
    }

    private static CallSite invoke1(MethodHandles.Lookup caller, String name, MethodType factoryType, MethodType accessType, Class<?> holdingClass, int type, boolean unsafe) {
        MethodHandles.Lookup lookup;
        try {
            if (unsafe) {
                lookup = LOOKUP_PROVIDER_UNSAFE.openingLookup(caller, holdingClass);
            } else {
                lookup = LOOKUP_PROVIDER_SAFE.openingLookup(caller, holdingClass);
            }
        } catch (IllegalAccessException e) {
            throw new OpeningException("Issue creating lookup", e);
        }
        if (type < STATIC_GET_TYPE) {
            name = remapMethod(name, accessType, holdingClass, caller.lookupClass().getClassLoader());
        } else if (type < CONSTRUCT_TYPE) {
            Class<?> fieldType;
            if (type == STATIC_GET_TYPE || type == INSTANCE_GET_TYPE) {
                fieldType = accessType.returnType();
            } else if (type == STATIC_SET_TYPE) {
                fieldType = accessType.parameterType(0);
            } else {
                fieldType = accessType.parameterType(1);
            }
            name = remapField(name, fieldType, holdingClass, caller.lookupClass().getClassLoader());
        }
        var handle = makeHandle(lookup, name, factoryType, accessType, holdingClass, type);
        return new ConstantCallSite(handle);
    }

    private static MethodHandle makeHandle(MethodHandles.Lookup lookup, String name, MethodType factoryType, MethodType accessType, Class<?> holdingClass, int type) {
        try {
            var handle = switch (type) {
                case STATIC_TYPE -> lookup.findStatic(holdingClass, name, accessType);
                case VIRTUAL_TYPE -> lookup.findVirtual(holdingClass, name, accessType.dropParameterTypes(0, 1));
                case SPECIAL_TYPE -> lookup.findSpecial(holdingClass, name, accessType.dropParameterTypes(0, 1), holdingClass);
                case STATIC_GET_TYPE -> lookup.findStaticGetter(holdingClass, name, accessType.returnType());
                case INSTANCE_GET_TYPE -> lookup.findGetter(holdingClass, name, accessType.returnType());
                case STATIC_SET_TYPE -> lookup.findStaticSetter(holdingClass, name, accessType.parameterType(0));
                case INSTANCE_SET_TYPE -> lookup.findSetter(holdingClass, name, accessType.parameterType(1));
                case CONSTRUCT_TYPE -> lookup.findConstructor(holdingClass, accessType.changeReturnType(Void.TYPE));
                case ARRAY_TYPE -> MethodHandles.arrayConstructor(holdingClass.arrayType());
                default -> throw new OpeningException("Unexpected opening type: " + type);
            };
            return handle.asType(factoryType);
        } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException e) {
            throw new OpeningException("Issue creating method handle for `"+name+"`", e);
        }
    }

    private static String remapMethod(String targetMethodName, MethodType methodType, Class<?> holdingClass, ClassLoader classLoader) {
        List<RuntimeRemapper> remappers = getRemapper(classLoader);
        for (var remapper : remappers) {
            String remapMethodName = remapper.remapMethodName(holdingClass, targetMethodName, methodType.parameterArray(), methodType.returnType());
            if (remapMethodName != null) {
                return remapMethodName;
            }
        }
        return targetMethodName;
    }

    private static String remapField(String targetFieldName, Class<?> targetType, Class<?> holdingClass, ClassLoader classLoader) {
        List<RuntimeRemapper> remappers = getRemapper(classLoader);
        for (var remapper : remappers) {
            String remapFieldName = remapper.remapFieldName(holdingClass, targetFieldName, targetType);
            if (remapFieldName != null) {
                return remapFieldName;
            }
        }
        return targetFieldName;
    }

    /**
     * Remap a class name.
     * @param className the class name to remap
     * @param classLoader the classloader the caller of {@link OpeningMetafactory} is using
     * @return the remapped class name, or the original if no remapping was found
     */
    @SuppressWarnings("unused")
    public static String remapClass(String className, ClassLoader classLoader) {
        List<RuntimeRemapper> remappers = getRemapper(classLoader);
        for (var remapper : remappers) {
            String remapClassName = remapper.remapClassName(className);
            if (remapClassName != null) {
                return remapClassName;
            }
        }
        return className;
    }

    public static CallSite makeOpenClass(MethodHandles.Lookup caller, String constructionMethodName, MethodType factoryType, MethodHandle targetClassGetter, MethodHandle classFieldPutter, MethodHandle classFieldGetter, MethodHandle infoGetter) {
        Class<?> targetClass;
        // Total format: fields, overrides, ctors
        // Field list format: String name, Class<?> fieldType, Boolean isFinal, List<String> setters, List<String> getters
        List<List<Object>> fields;
        // Override list format: String name, MethodHandle interface, String overrideName, MethodHandle toOverride
        List<List<Object>> overrides;
        // Ctor list format: MethodHandle type, MethodHandle superType, List<String> fields
        List<List<Object>> ctors;
        try {
            targetClass = (Class<?>) targetClassGetter.invoke(caller.lookupClass().getClassLoader());
            //noinspection unchecked
            List<List<List<Object>>> all = (List<List<List<Object>>>) infoGetter.invoke(caller.lookupClass().getClassLoader());
            fields = all.get(0);
            overrides = all.get(1);
            ctors = all.get(2);
        } catch (Throwable e) {
            throw new OpeningException(e);
        }
        Class<?> holdingClass = caller.lookupClass();
        if (!factoryType.returnType().equals(holdingClass)) {
            throw new OpeningException("Factory type return type must be the same as the holding class");
        }
        MethodHandles.Lookup lookup;
        try {
            lookup = LOOKUP_PROVIDER_UNSAFE.openingLookup(caller, targetClass);
        } catch (IllegalAccessException e) {
            throw new OpeningException("Issue creating lookup", e);
        }

        Class<?> generatedClass;
        try {
            generatedClass = (Class<?>) classFieldGetter.invokeExact();
            if (generatedClass == null) {
                generatedClass = generateClass(lookup, targetClass, constructionMethodName, holdingClass, fields, overrides, ctors);
                classFieldPutter.invokeExact(generatedClass);
            }
            MethodHandle ctor = lookup.findConstructor(generatedClass, factoryType.changeReturnType(void.class)).asType(factoryType);
            return new ConstantCallSite(ctor);
        } catch (Throwable e) {
            throw new OpeningException("Could not get existing generated subclass", e);
        }
    }

    private static Class<?> generateClass(MethodHandles.Lookup lookup, Class<?> targetClass, String constructionMethodName, Class<?> holdingClass, List<List<Object>> fields, List<List<Object>> overrides, List<List<Object>> ctors) {
        var classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        String generatedClassName = Type.getInternalName(targetClass) + "$" + Type.getInternalName(holdingClass).replace('/','$') + "$" + constructionMethodName;
        classWriter.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, generatedClassName, null, Type.getInternalName(targetClass), new String[] {Type.getInternalName(holdingClass)});

        Map<String, Class<?>> fieldTypes = new HashMap<>();

        for (var field : fields) {
            String fieldName = (String) field.get(0);
            var fieldType = (Class<?>) field.get(1);
            boolean isFinal = (boolean) field.get(2);
            fieldTypes.put(fieldName, fieldType);
            classWriter.visitField(Opcodes.ACC_PRIVATE | (isFinal ? Opcodes.ACC_FINAL : 0), fieldName, Type.getDescriptor(fieldType), null, null).visitEnd();
            //noinspection unchecked
            List<String> setters = (List<String>) field.get(3);
            //noinspection unchecked
            List<String> getters = (List<String>) field.get(4);
            for (var setter : setters) {
                Arrays.stream(holdingClass.getMethods())
                        .filter(m -> m.getName().equals(setter) && m.getParameterCount() == 1 && m.getReturnType().equals(void.class) && m.getParameterTypes()[0].equals(fieldType))
                        .findFirst()
                        .orElseThrow(() -> new OpeningException("Could not find interface method to overload with name "+setter+", type "+fieldType));
                var methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, setter, Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(fieldType)), null, null);
                methodVisitor.visitCode();
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitVarInsn(Type.getType(fieldType).getOpcode(Opcodes.ALOAD), 1);
                methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, generatedClassName, fieldName, Type.getDescriptor(fieldType));
                methodVisitor.visitInsn(Opcodes.RETURN);
                methodVisitor.visitMaxs(2, 2);
                methodVisitor.visitEnd();
            }
            for (var getter : getters) {
                Arrays.stream(holdingClass.getDeclaredMethods())
                        .filter(m -> m.getName().equals(getter) && m.getParameterCount() == 0 && m.getReturnType().equals(fieldType))
                        .findFirst()
                        .orElseThrow(() -> new OpeningException("Could not find interface method to overload with name "+getter+", type "+fieldType));
                var methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, getter, Type.getMethodDescriptor(Type.getType(fieldType)), null, null);
                methodVisitor.visitCode();
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitFieldInsn(Opcodes.GETFIELD, generatedClassName, fieldName, Type.getDescriptor(fieldType));
                methodVisitor.visitInsn(Type.getType(fieldType).getOpcode(Opcodes.ARETURN));
                methodVisitor.visitMaxs(1, 1);
                methodVisitor.visitEnd();
            }
        }

        for (var override : overrides) {
            var name = (String) override.get(0);
            MethodType interfaceType;
            try {
                interfaceType = (MethodType) ((MethodHandle) override.get(1)).invoke(holdingClass.getClassLoader());
            } catch (Throwable e) {
                throw new OpeningException(e);
            }
            var overrideName = (String) override.get(2);
            var overrideType = (MethodType) override.get(3);
            Arrays.stream(holdingClass.getDeclaredMethods())
                    .filter(m -> m.getName().equals(name) && m.getParameterCount() == interfaceType.parameterCount() && m.getReturnType().equals(interfaceType.returnType()) && Arrays.equals(m.getParameterTypes(), interfaceType.parameterArray()))
                    .filter(Method::isDefault)
                    .findFirst()
                    .orElseThrow(() -> new OpeningException("Could not find interface method to bounce override to with name "+name+", type "+interfaceType));
            var parameterTypes = Arrays.stream(overrideType.parameterArray()).map(Type::getType).toArray(Type[]::new);
            var overrideDesc = Type.getMethodDescriptor(Type.getType(overrideType.returnType()), parameterTypes);
            var methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, overrideName, overrideDesc, null, null);
            methodVisitor.visitCode();
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            for (int i = 0; i < overrideType.parameterCount(); i++) {
                methodVisitor.visitVarInsn(Type.getType(overrideType.parameterType(i)).getOpcode(Opcodes.ALOAD), i+1);
            }
            var fullParameterTypes = new Type[overrideType.parameterCount() + 1];
            fullParameterTypes[0] = Type.getType(holdingClass);
            System.arraycopy(parameterTypes, 0, fullParameterTypes, 1, parameterTypes.length);
            var fullInterfaceParameterTypes = new Type[interfaceType.parameterCount() + 1];
            fullInterfaceParameterTypes[0] = Type.getType(holdingClass);
            System.arraycopy(Arrays.stream(interfaceType.parameterArray()).map(Type::getType).toArray(Type[]::new), 0, fullInterfaceParameterTypes, 1, interfaceType.parameterCount());
            methodVisitor.visitInvokeDynamicInsn(
                    name,
                    Type.getMethodDescriptor(Type.getType(overrideType.returnType()), fullParameterTypes),
                    new Handle(
                            Opcodes.H_INVOKESTATIC,
                            Type.getInternalName(OpeningMetafactory.class),
                            "invoke",
                            MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, MethodHandle.class, MethodHandle.class, int.class).toMethodDescriptorString(),
                            false
                    ),
                    MinimalConDynUtils.conDynFromClass(Type.getType(holdingClass)),
                    MinimalConDynUtils.conDynMethodType(MinimalConDynUtils.conDynFromClass(Type.getType(interfaceType.returnType())), Arrays.stream(fullInterfaceParameterTypes).map(MinimalConDynUtils::conDynFromClass).toList()),
                    VIRTUAL_TYPE
            );
            if (overrideType.returnType().equals(void.class)) {
                methodVisitor.visitInsn(Opcodes.RETURN);
            } else {
                methodVisitor.visitInsn(Type.getType(overrideType.returnType()).getOpcode(Opcodes.ARETURN));
            }
            methodVisitor.visitMaxs(1, 1);
            methodVisitor.visitEnd();
        }

        for (var ctor : ctors) {
            MethodType ctorType;
            MethodType superType;
            try {
                ctorType = (MethodType) ((MethodHandle) ctor.get(0)).invoke(holdingClass.getClassLoader());
                superType = (MethodType) ((MethodHandle) ctor.get(1)).invoke(holdingClass.getClassLoader());
            } catch (Throwable e) {
                throw new OpeningException(e);
            }
            //noinspection unchecked
            var fieldsToSet = (List<String>) ctor.get(2);

            var ctorDesc = ctorType.descriptorString();
            var methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", ctorDesc, null, null);
            methodVisitor.visitCode();
            var superCtorCount = superType.parameterCount();
            var remainingCount = ctorType.parameterCount() - fieldsToSet.size();
            if (superCtorCount != remainingCount) {
                throw new OpeningException("Super constructor parameter count does not match remaining parameter count");
            }
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            for (int i = 0; i < superCtorCount; i++) {
                methodVisitor.visitVarInsn(Type.getType(ctorType.parameterType(i + fieldsToSet.size() + 1)).getOpcode(Opcodes.ALOAD), i+1);
            }
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(targetClass), "<init>", superType.descriptorString(), false);
            for (int i = 0; i < fieldsToSet.size(); i++) {
                var fieldName = fieldsToSet.get(i);
                var fieldType = fieldTypes.get(fieldName);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitVarInsn(Type.getType(fieldType).getOpcode(Opcodes.ALOAD), i+1);
                methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, generatedClassName, fieldName, Type.getDescriptor(fieldType));
            }
            methodVisitor.visitInsn(Opcodes.RETURN);
            methodVisitor.visitMaxs(1, 1);
            methodVisitor.visitEnd();
        }
        classWriter.visitEnd();

        try {
            var targetModule = targetClass.getModule();
            var hostModule = holdingClass.getModule();
            if (targetModule != hostModule && targetModule != null && hostModule != null && !targetModule.canRead(hostModule)) {
                try {
                    MethodHandle handle = lookup.in(targetClass).findVirtual(Module.class, "addReads", MethodType.methodType(Module.class, Module.class));
                    handle.invokeWithArguments(targetModule, hostModule);
                } catch (Throwable e) {
                    throw new OpeningException("While opening, could not add read edge from "+targetModule+" to "+hostModule, e);
                }
            }
            return lookup.in(targetClass).defineHiddenClass(classWriter.toByteArray(), false, MethodHandles.Lookup.ClassOption.NESTMATE).lookupClass();
        } catch (IllegalAccessException e) {
            throw new OpeningException("Issue creating hidden class", e);
        }
    }
}
