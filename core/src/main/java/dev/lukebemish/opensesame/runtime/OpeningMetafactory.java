package dev.lukebemish.opensesame.runtime;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.InstructionAdapter;

import java.lang.invoke.*;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
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
            name = remapMethod(name, accessType.descriptorString(), holdingClass.getName(), caller.lookupClass().getClassLoader());
        } else if (type < CONSTRUCT_TYPE) {
            Class<?> fieldType;
            if (type == STATIC_GET_TYPE || type == INSTANCE_GET_TYPE) {
                fieldType = accessType.returnType();
            } else if (type == STATIC_SET_TYPE) {
                fieldType = accessType.parameterType(0);
            } else {
                fieldType = accessType.parameterType(1);
            }
            name = remapField(name, fieldType.descriptorString(), holdingClass.getName(), caller.lookupClass().getClassLoader());
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

    /**
     * Remap a class name.
     * @param targetMethodName the field name to remap
     * @param methodDescriptor the descriptor of the field to remap
     * @param holdingClassName the class name of the class that holds the field
     * @param classLoader the classloader the caller of {@link OpeningMetafactory} is using
     * @return the remapped class name, or the original if no remapping was found
     */
    public static String remapMethod(String targetMethodName, String methodDescriptor, String holdingClassName, ClassLoader classLoader) {
        List<RuntimeRemapper> remappers = getRemapper(classLoader);
        for (var remapper : remappers) {
            String remapMethodName = remapper.remapMethodName(holdingClassName, targetMethodName, methodDescriptor);
            if (remapMethodName != null) {
                return remapMethodName;
            }
        }
        return targetMethodName;
    }

    /**
     * Remap a class name.
     * @param targetFieldName the field name to remap
     * @param fieldDescriptor the descriptor of the field to remap
     * @param holdingClassName the class name of the class that holds the field
     * @param classLoader the classloader the caller of {@link OpeningMetafactory} is using
     * @return the remapped class name, or the original if no remapping was found
     */
    public static String remapField(String targetFieldName, String fieldDescriptor, String holdingClassName, ClassLoader classLoader) {
        List<RuntimeRemapper> remappers = getRemapper(classLoader);
        for (var remapper : remappers) {
            String remapFieldName = remapper.remapFieldName(holdingClassName, targetFieldName, fieldDescriptor);
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

    @SuppressWarnings("unused")
    public static CallSite makeOpenClass(MethodHandles.Lookup caller, String constructionMethodName, MethodType factoryType, MethodHandle targetClassGetter, MethodHandle classFieldPutter, MethodHandle classFieldGetter, MethodHandle infoGetter) {
        return makeOpenClass(caller, constructionMethodName, factoryType, targetClassGetter, classFieldPutter, classFieldGetter, infoGetter, false);
    }

    @SuppressWarnings("unused")
    public static CallSite makeOpenClassUnsafe(MethodHandles.Lookup caller, String constructionMethodName, MethodType factoryType, MethodHandle targetClassGetter, MethodHandle classFieldPutter, MethodHandle classFieldGetter, MethodHandle infoGetter) {
        return makeOpenClass(caller, constructionMethodName, factoryType, targetClassGetter, classFieldPutter, classFieldGetter, infoGetter, true);
    }

    private static CallSite makeOpenClass(MethodHandles.Lookup caller, String constructionMethodName, MethodType factoryType, MethodHandle targetClassGetter, MethodHandle classFieldPutter, MethodHandle classFieldGetter, MethodHandle infoGetter, boolean unsafe) {
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
        // check for interfaces
        for (Class<?> iFace : holdingClass.getInterfaces()) {
            if (!iFace.equals(Extension.class)) {
                if (Extension.class.isAssignableFrom(iFace)) {
                    throw new OpeningException("Extension interface "+holdingClass.getName()+" implements an interface "+iFace.getName()+" that is an extension, which is not allowed");
                }
            }
        }
        if (!factoryType.returnType().equals(holdingClass)) {
            throw new OpeningException("Factory type return type must be the same as the holding class");
        }
        MethodHandles.Lookup lookup;
        try {
            if (unsafe) {
                lookup = LOOKUP_PROVIDER_UNSAFE.openingLookup(caller, targetClass);
            } else {
                if (targetClass.getModule() != holdingClass.getModule()) {
                    throw new OpeningException("Holding interface and class to extend must be in the same module if `unsafe` is false");
                }
                lookup = LOOKUP_PROVIDER_SAFE.openingLookup(caller, targetClass);
            }
        } catch (IllegalAccessException e) {
            throw new OpeningException("Issue creating lookup", e);
        }

        Class<?> generatedClass;
        try {
            generatedClass = (Class<?>) classFieldGetter.invokeExact();
            if (generatedClass == null) {
                generatedClass = generateClass(caller, lookup, targetClass, constructionMethodName, holdingClass, fields, overrides, ctors);
                classFieldPutter.invokeExact(generatedClass);
            }
            MethodHandle ctor = lookup.findConstructor(generatedClass, factoryType.changeReturnType(void.class)).asType(factoryType);
            return new ConstantCallSite(ctor);
        } catch (Throwable e) {
            throw new OpeningException("Could not get existing generated subclass", e);
        }
    }

    private static Class<?> generateClass(MethodHandles.Lookup originalLookup, MethodHandles.Lookup lookup, Class<?> targetClass, String constructionMethodName, Class<?> holdingClass, List<List<Object>> fields, List<List<Object>> overrides, List<List<Object>> ctors) {
        var classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        var isInterface = targetClass.isInterface();
        var superClass = isInterface ? Object.class : targetClass;
        var interfaces = isInterface ? new String[] {Type.getInternalName(targetClass), Type.getInternalName(holdingClass)} : new String[] {Type.getInternalName(holdingClass)};

        var targetModule = targetClass.getModule();
        var holdingModule = holdingClass.getModule();
        boolean allVisible = (targetClass.getModifiers() & Opcodes.ACC_PUBLIC) != 0;
        if (targetModule != holdingModule && holdingModule != null && targetModule != null && (!targetModule.isExported(targetClass.getPackageName(), holdingModule) || !holdingModule.canRead(targetModule))) {
            allVisible = false;
        }
        for (var override : overrides) {
            var overrideName = (String) override.get(2);
            MethodType overrideType;
            try {
                overrideType = (MethodType) ((MethodHandle) override.get(3)).invoke(holdingClass.getClassLoader());
            } catch (Throwable e) {
                throw new OpeningException(e);
            }
            overrideName = remapMethod(overrideName, overrideType.descriptorString(), targetClass.getName(), holdingClass.getClassLoader());
            try {
                Method originalMethod = targetClass.getDeclaredMethod(overrideName, overrideType.parameterArray());
                if ((originalMethod.getModifiers() & Opcodes.ACC_PUBLIC) == 0 && (originalMethod.getModifiers() & Opcodes.ACC_PROTECTED) == 0) {
                    allVisible = false;
                }
                if ((Arrays.stream(originalMethod.getParameterTypes()).anyMatch(it -> (it.getModifiers() & Opcodes.ACC_PUBLIC) == 0))) {
                    allVisible = false;
                }
                if ((originalMethod.getReturnType().getModifiers() & Opcodes.ACC_PUBLIC) == 0) {
                    allVisible = false;
                }
            } catch (NoSuchMethodException e) {
                allVisible = false;
            }
        }
        if (!isInterface) {
            for (var ctor : ctors) {
                MethodType superType;
                try {
                    superType = (MethodType) ((MethodHandle) ctor.get(1)).invoke(holdingClass.getClassLoader());
                } catch (Throwable e) {
                    throw new OpeningException(e);
                }

                try {
                    Constructor<?> originalCtor = targetClass.getDeclaredConstructor(superType.parameterArray());
                    if ((originalCtor.getModifiers() & Opcodes.ACC_PUBLIC) == 0 && (originalCtor.getModifiers() & Opcodes.ACC_PROTECTED) == 0) {
                        allVisible = false;
                    }
                } catch (NoSuchMethodException e) {
                    allVisible = false;
                }
            }
        }
        String generatedClassName = Type.getInternalName(allVisible ? holdingClass : targetClass) + "$" + Type.getInternalName(holdingClass).replace('/','$') + "$" + constructionMethodName;

        classWriter.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, generatedClassName, null, Type.getInternalName(superClass), interfaces);

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
                Arrays.stream(holdingClass.getDeclaredMethods())
                        .filter(m -> m.getName().equals(setter) && m.getParameterCount() == 1 && m.getReturnType().equals(void.class) && m.getParameterTypes()[0].equals(fieldType))
                        .findFirst()
                        .orElseThrow(() -> new OpeningException("Could not find interface setter method to overload with name "+setter+", type "+fieldType));
                var methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, setter, Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(fieldType)), null, null);
                methodVisitor.visitCode();
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitVarInsn(Type.getType(fieldType).getOpcode(Opcodes.ILOAD), 1);
                methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, generatedClassName, fieldName, Type.getDescriptor(fieldType));
                methodVisitor.visitInsn(Opcodes.RETURN);
                methodVisitor.visitMaxs(2, 2);
                methodVisitor.visitEnd();
            }
            for (var getter : getters) {
                Arrays.stream(holdingClass.getDeclaredMethods())
                        .filter(m -> m.getName().equals(getter) && m.getParameterCount() == 0 && m.getReturnType().equals(fieldType))
                        .findFirst()
                        .orElseThrow(() -> new OpeningException("Could not find interface getter method to overload with name "+getter+", type "+fieldType));
                var methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, getter, Type.getMethodDescriptor(Type.getType(fieldType)), null, null);
                methodVisitor.visitCode();
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitFieldInsn(Opcodes.GETFIELD, generatedClassName, fieldName, Type.getDescriptor(fieldType));
                methodVisitor.visitInsn(Type.getType(fieldType).getOpcode(Opcodes.IRETURN));
                methodVisitor.visitMaxs(1, 1);
                methodVisitor.visitEnd();
            }
        }

        for (var override : overrides) {
            var name = (String) override.get(0);
            var overrideName = (String) override.get(2);
            MethodType interfaceType;
            MethodType overrideType;
            try {
                interfaceType = (MethodType) ((MethodHandle) override.get(1)).invoke(holdingClass.getClassLoader());
                overrideType = (MethodType) ((MethodHandle) override.get(3)).invoke(holdingClass.getClassLoader());
            } catch (Throwable e) {
                throw new OpeningException(e);
            }
            overrideName = remapMethod(overrideName, overrideType.descriptorString(), targetClass.getName(), holdingClass.getClassLoader());
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
            int j = 1;
            for (int i = 0; i < overrideType.parameterCount(); i++) {
                var t = Type.getType(overrideType.parameterType(i));
                methodVisitor.visitVarInsn(t.getOpcode(Opcodes.ILOAD), j);
                j += t.getSize();
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
                methodVisitor.visitInsn(Type.getType(overrideType.returnType()).getOpcode(Opcodes.IRETURN));
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
            int fieldsTotalSize = 0;
            for (String fieldName : fieldsToSet) {
                var fieldType = fieldTypes.get(fieldName);
                fieldsTotalSize += Type.getType(fieldType).getSize();
            }
            int j = 1 + fieldsTotalSize;
            for (int i = 0; i < superCtorCount; i++) {
                var ctorArgClass = ctorType.parameterType(i + fieldsToSet.size());
                var t = Type.getType(ctorArgClass);
                methodVisitor.visitVarInsn(t.getOpcode(Opcodes.ILOAD), j);
                var superArgClass = superType.parameterType(i);
                convertToType(methodVisitor, ctorArgClass, superArgClass);
                j += t.getSize();
            }
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(superClass), "<init>", superType.descriptorString(), false);
            j = 1;
            for (String fieldName : fieldsToSet) {
                var fieldType = fieldTypes.get(fieldName);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitVarInsn(Type.getType(fieldType).getOpcode(Opcodes.ILOAD), j);
                j += Type.getType(fieldType).getSize();
                methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, generatedClassName, fieldName, Type.getDescriptor(fieldType));
            }
            methodVisitor.visitInsn(Opcodes.RETURN);
            methodVisitor.visitMaxs(1, 1);
            methodVisitor.visitEnd();
        }
        classWriter.visitEnd();

        byte[] bytes = classWriter.toByteArray();

        try {
            var lookupIn = allVisible ? originalLookup.in(holdingClass) : lookup.in(targetClass);
            if (targetModule != holdingModule && targetModule != null && holdingModule != null && !targetModule.canRead(holdingModule)) {
                try {
                    MethodHandle handle = lookupIn.findVirtual(Module.class, "addReads", MethodType.methodType(Module.class, Module.class));
                    handle.invokeWithArguments(targetModule, holdingModule);
                } catch (Throwable e) {
                    throw new OpeningException("While opening, could not add read edge from "+targetModule+" to "+holdingModule, e);
                }
            }
            var openingModule = OpeningMetafactory.class.getModule();
            if (targetModule != openingModule && targetModule != null && openingModule != null && !targetModule.canRead(openingModule)) {
                try {
                    MethodHandle handle = lookupIn.findVirtual(Module.class, "addReads", MethodType.methodType(Module.class, Module.class));
                    handle.invokeWithArguments(targetModule, openingModule);
                } catch (Throwable e) {
                    throw new OpeningException("While opening, could not add read edge from "+targetModule+" to "+openingModule, e);
                }
            }
            return lookupIn.defineHiddenClass(bytes, false, MethodHandles.Lookup.ClassOption.NESTMATE).lookupClass();
        } catch (IllegalAccessException e) {
            throw new OpeningException("Issue creating hidden class", e);
        }
    }

    private static void convertToType(MethodVisitor methodVisitor, Class<?> ctorClass, Class<?> superClass) {
        if (superClass.isAssignableFrom(ctorClass)) {
            return;
        }
        Type from = Type.getType(ctorClass);
        Type to = Type.getType(superClass);
        var adapter = new InstructionAdapter(methodVisitor);
        if (ctorClass.isPrimitive() && superClass.isPrimitive()) {
            adapter.cast(from, to);
        } else if (ctorClass.isPrimitive()) {
            if (superClass.equals(Double.class)) {
                adapter.cast(from, Type.DOUBLE_TYPE);
                valueOf(methodVisitor, Double.class, double.class);
            } else if (superClass.equals(Float.class)) {
                adapter.cast(from, Type.FLOAT_TYPE);
                valueOf(methodVisitor, Float.class, float.class);
            } else if (superClass.equals(Integer.class)) {
                adapter.cast(from, Type.INT_TYPE);
                valueOf(methodVisitor, Integer.class, int.class);
            } else if (superClass.equals(Boolean.class)) {
                adapter.cast(from, Type.BOOLEAN_TYPE);
                valueOf(methodVisitor, Boolean.class, boolean.class);
            } else if (superClass.equals(Character.class)) {
                adapter.cast(from, Type.CHAR_TYPE);
                valueOf(methodVisitor, Character.class, char.class);
            } else if (superClass.equals(Byte.class)) {
                adapter.cast(from, Type.BYTE_TYPE);
                valueOf(methodVisitor, Byte.class, byte.class);
            } else if (superClass.equals(Short.class)) {
                adapter.cast(from, Type.SHORT_TYPE);
                valueOf(methodVisitor, Short.class, short.class);
            } else if (superClass.equals(Long.class)) {
                adapter.cast(from, Type.LONG_TYPE);
                valueOf(methodVisitor, Long.class, long.class);
            }
        } else if (superClass.isPrimitive()) {
            if (ctorClass.equals(Character.class)) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Character.class), "charValue", "()C", false);
                adapter.cast(Type.CHAR_TYPE, to);
            } else if (ctorClass.equals(Boolean.class)) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Boolean.class), "booleanValue", "()Z", false);
                adapter.cast(Type.BOOLEAN_TYPE, to);
            } else if (Number.class.isAssignableFrom(ctorClass)) {
                String name;
                String desc;
                boolean fromInt = false;
                if (superClass.equals(int.class)) {
                    name = "int";
                    desc = "I";
                } else if (superClass.equals(double.class)) {
                    name = "double";
                    desc = "D";
                } else if (superClass.equals(float.class)) {
                    name = "float";
                    desc = "F";
                } else if (superClass.equals(byte.class)) {
                    name = "byte";
                    desc = "B";
                } else if (superClass.equals(short.class)) {
                    name = "short";
                    desc = "S";
                } else if (superClass.equals(long.class)) {
                    name = "long";
                    desc = "J";
                } else {
                    name = "int";
                    desc = "I";
                    fromInt = true;
                }
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(ctorClass), name+"Value", "()"+desc, false);
                if (fromInt) {
                    adapter.cast(Type.INT_TYPE, to);
                }
            }
        } else {
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, to.getInternalName());
        }
    }

    private static void valueOf(MethodVisitor methodVisitor, Class<?> clazz, Class<?> primitiveClass) {
        Type type = Type.getType(clazz);
        var methodType = MethodType.methodType(clazz, primitiveClass);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, type.getInternalName(), "valueOf", methodType.descriptorString(), false);
    }
}
