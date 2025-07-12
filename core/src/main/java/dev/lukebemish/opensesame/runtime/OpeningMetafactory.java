package dev.lukebemish.opensesame.runtime;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.InstructionAdapter;

import java.lang.invoke.*;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Supplier;

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
            if (getLookupProviderUnsafe().exception != null) {
                exception.addSuppressed(getLookupProviderUnsafe().exception);
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

    private static final LayeredServiceLoader<RuntimeRemapper> RUNTIME_REMAPPERS = LayeredServiceLoader.of(RuntimeRemapper.class);
    private static final Map<ClassLoaderKey, List<RuntimeRemapper>> REMAPPER_LOOKUP = new HashMap<>();
    private static final ReferenceQueue<ClassLoader> REMAPPER_LOOKUP_QUEUE = new ReferenceQueue<>();
    private static final LookupProvider LOOKUP_PROVIDER_SAFE = new LookupProviderFallback();
    
    private record LookupProviderResults(LookupProvider provider, Exception exception) {}
    private static LookupProviderResults LOOKUP_PROVIDER_UNSAFE_RESULTS;
    private static final Object LOOKUP_PROVIDER_UNSAFE_LOCK = new Object();

    private static final List<Supplier<LookupProvider>> IMPL_LOOKUP_PROVIDER_LIST = List.of(
            LookupProviderFFI::new,
            LookupProviderNative::new,
            LookupProviderUnsafe::new
    );

    private static LookupProviderResults getLookupProviderUnsafe() {
        if (LOOKUP_PROVIDER_UNSAFE_RESULTS != null) {
            return LOOKUP_PROVIDER_UNSAFE_RESULTS;
        }
        synchronized (LOOKUP_PROVIDER_UNSAFE_LOCK) {
            if (LOOKUP_PROVIDER_UNSAFE_RESULTS != null) {
                return LOOKUP_PROVIDER_UNSAFE_RESULTS;
            }
            Exception LOOKUP_PROVIDER_EXCEPTION1 = null;
            LookupProvider LOOKUP_PROVIDER1 = null;
            for (var supplier : IMPL_LOOKUP_PROVIDER_LIST) {
                try {
                    LOOKUP_PROVIDER1 = supplier.get();
                    break;
                } catch (Exception e) {
                    if (LOOKUP_PROVIDER_EXCEPTION1 != null) {
                        e.addSuppressed(LOOKUP_PROVIDER_EXCEPTION1);
                    }
                    LOOKUP_PROVIDER_EXCEPTION1 = e;
                }
            }
            var results = new LookupProviderResults(
                    LOOKUP_PROVIDER1 == null ? new LookupProviderFallback() : LOOKUP_PROVIDER1,
                    LOOKUP_PROVIDER_EXCEPTION1
            );
            LOOKUP_PROVIDER_UNSAFE_RESULTS = results;
            return results;
        }
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

    private synchronized static List<RuntimeRemapper> getRemapper(Class<?> clazz) {
        return LayeredServiceLoader.unique(RUNTIME_REMAPPERS.at(clazz));
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
        try {
            var fromModule = caller.lookupClass().getModule();
            var toModule = holdingClass.getModule();
            if (fromModule != toModule && !fromModule.canRead(toModule)) {
                var addReads = caller.findVirtual(Module.class, "addReads", MethodType.methodType(Module.class, Module.class));
                addReads.invoke(fromModule, toModule);
            }
        } catch (Throwable t) {
            throw new OpeningException("Issue ensuring module visibility: ", t);
        }

        MethodHandles.Lookup lookup;
        try {
            if (unsafe) {
                lookup = getLookupProviderUnsafe().provider.openingLookup(caller, holdingClass);
            } else {
                lookup = LOOKUP_PROVIDER_SAFE.openingLookup(caller, holdingClass);
            }
        } catch (IllegalAccessException e) {
            throw new OpeningException("Issue creating lookup", e);
        }
        if (type < STATIC_GET_TYPE) {
            name = remapMethod(name, accessType.descriptorString(), holdingClass.getName(), caller.lookupClass());
        } else if (type < CONSTRUCT_TYPE) {
            Class<?> fieldType;
            if (type == STATIC_GET_TYPE || type == INSTANCE_GET_TYPE) {
                fieldType = accessType.returnType();
            } else if (type == STATIC_SET_TYPE) {
                fieldType = accessType.parameterType(0);
            } else {
                fieldType = accessType.parameterType(1);
            }
            name = remapField(name, fieldType.descriptorString(), holdingClass.getName(), caller.lookupClass());
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
     * @deprecated
     * @see #remapMethod(String, String, String, Class) 
     */
    @Deprecated(forRemoval = true)
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
     * @deprecated
     * @see #remapField(String, String, String, Class) 
     */
    @Deprecated(forRemoval = true)
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
     * @deprecated
     * @see #remapClass(String, Class) 
     */
    @Deprecated(forRemoval = true)
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

    /**
     * Remap a class name.
     * @param targetMethodName the field name to remap
     * @param methodDescriptor the descriptor of the field to remap
     * @param holdingClassName the class name of the class that holds the field
     * @param contextClass the caller of {@link OpeningMetafactory}
     * @return the remapped class name, or the original if no remapping was found
     */
    public static String remapMethod(String targetMethodName, String methodDescriptor, String holdingClassName, Class<?> contextClass) {
        List<RuntimeRemapper> remappers = getRemapper(contextClass);
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
     * @param contextClass the caller of {@link OpeningMetafactory}
     * @return the remapped class name, or the original if no remapping was found
     */
    public static String remapField(String targetFieldName, String fieldDescriptor, String holdingClassName, Class<?> contextClass) {
        List<RuntimeRemapper> remappers = getRemapper(contextClass);
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
     * @param contextClass the caller of {@link OpeningMetafactory}
     * @return the remapped class name, or the original if no remapping was found
     */
    @SuppressWarnings("unused")
    public static String remapClass(String className, Class<?> contextClass) {
        List<RuntimeRemapper> remappers = getRemapper(contextClass);
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
                lookup = getLookupProviderUnsafe().provider.openingLookup(caller, targetClass);
            } else {
                lookup = LOOKUP_PROVIDER_SAFE.openingLookup(caller, targetClass);
                if (targetClass.getModule() != holdingClass.getModule() && (lookup.lookupModes() & MethodHandles.Lookup.ORIGINAL) == 0) {
                    throw new OpeningException("Holding interface and class to extend must be in the same module, or otherwise have ORIGINAL lookup access, if `unsafe` is false");
                }
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
            MethodHandle ctor = findCtorOrAllocator(factoryType, lookup, generatedClass);
            return new ConstantCallSite(ctor);
        } catch (Throwable e) {
            throw new OpeningException("Could not get existing generated subclass", e);
        }
    }

    private static MethodHandle findCtorOrAllocator(MethodType factoryType, MethodHandles.Lookup lookup, Class<?> generatedClass) throws Throwable {
        var constructor = generatedClass.getConstructor(factoryType.parameterArray());
        if (constructor.isAnnotationPresent(ManualAllocation.class)) {
            var annotationValue = constructor.getAnnotation(ManualAllocation.class);
            var superClass = annotationValue.superClass();
            var superClassCtor = lookup.findConstructor(superClass, MethodType.methodType(void.class, annotationValue.superConstructor()));
            return ManualAllocationUtil.constructionHandle(generatedClass, annotationValue.superClass(), superClassCtor, lookup, annotationValue.fields()).asType(factoryType);
        }
        return lookup.findConstructor(generatedClass, factoryType.changeReturnType(void.class)).asType(factoryType);
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
            overrideName = remapMethod(overrideName, overrideType.descriptorString(), targetClass.getName(), holdingClass);
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
        var allCtorsVisible = true;
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
                        allCtorsVisible = false;
                    }
                } catch (NoSuchMethodException e) {
                    allVisible = false;
                    allCtorsVisible = false;
                }
            }
        }

        ProxyUtil.ProxyData proxyData = new ProxyUtil.ProxyData(allVisible ? holdingClass : targetClass, false);

        Set<Class<?>> superRequirements = new HashSet<>();
        if (!allVisible) {
            Set<Class<?>> requirements = new HashSet<>();
            for (var field : fields) {
                var fieldType = (Class<?>) field.get(1);
                requirements.add(fieldType);
            }
            for (var override : overrides) {
                try {
                    var interfaceType = (MethodType) ((MethodHandle) override.get(1)).invoke(holdingClass.getClassLoader());
                    var superType = (MethodType) ((MethodHandle) override.get(3)).invoke(holdingClass.getClassLoader());

                    requirements.add(interfaceType.returnType());
                    requirements.addAll(interfaceType.parameterList());
                    
                    superRequirements.add(superType.returnType());
                    superRequirements.addAll(superType.parameterList());
                } catch (Throwable t) {
                    throw new OpeningException(t);
                }
            }
            for (var ctor : ctors) {
                try {
                    var ctorType = (MethodType) ((MethodHandle) ctor.get(0)).invoke(holdingClass.getClassLoader());
                    var superType = (MethodType) ((MethodHandle) ctor.get(1)).invoke(holdingClass.getClassLoader());

                    requirements.addAll(ctorType.parameterList());
                    superRequirements.addAll(superType.parameterList());
                } catch (Throwable t) {
                    throw new OpeningException(t);
                }
            }

            try {
                proxyData = ProxyUtil.makeProxyModule(
                        targetClass,
                        holdingClass,
                        requirements,
                        lookup
                );
            } catch (Throwable t) {
                throw new OpeningException(t);
            }
        }

        Map<String, Class<?>> fieldTypes = new HashMap<>();

        for (var field : fields) {
            String fieldName = (String) field.get(0);
            var fieldType = (Class<?>) field.get(1);
            fieldTypes.put(fieldName, fieldType);
        }

        boolean requiresManualAllocation = proxyData.isProxy() && !allCtorsVisible;
        boolean bouncesCtors = false;
        boolean boundeOverrides = false;
        try {
            if (proxyData.isProxy() && ((targetClass.getModifiers() & Modifier.PUBLIC) == 0 || !ProxyUtil.canAccess(proxyData.targetClass(), superRequirements, lookup))) {
                // Generate a new proxy interface and use that instead
                var ctorTypes = new ArrayList<MethodType>();
                var superCtorTypes = new ArrayList<MethodType>();
                var overridesTypes = new ArrayList<MethodType>();
                var overridesSuperTypes = new ArrayList<MethodType>();
                var overridesNames = new ArrayList<String>();
                var overridesSuperNames = new ArrayList<String>();
                for (var ctor : ctors) {
                    var ctorType = (MethodType) ((MethodHandle) ctor.get(0)).invoke(holdingClass.getClassLoader());
                    var superType = (MethodType) ((MethodHandle) ctor.get(1)).invoke(holdingClass.getClassLoader());
                    //noinspection unchecked
                    var fieldsToSet = (List<String>) ctor.get(2);

                    ctorTypes.add(MethodType.methodType(ctorType.returnType(), ctorType.parameterList().subList(fieldsToSet.size(), ctorType.parameterCount())));
                    superCtorTypes.add(superType);
                }
                for (var override : overrides) {
                    var interfaceType = (MethodType) ((MethodHandle) override.get(1)).invoke(holdingClass.getClassLoader());
                    var superType = (MethodType) ((MethodHandle) override.get(3)).invoke(holdingClass.getClassLoader());

                    var interfaceName = (String) override.get(0);
                    var superName = (String) override.get(2);
                    superName = remapMethod(superName, superType.descriptorString(), targetClass.getName(), holdingClass);

                    overridesTypes.add(interfaceType);
                    overridesSuperTypes.add(superType);

                    overridesNames.add(interfaceName);
                    overridesSuperNames.add(superName);
                }
                var bounceType = ProxyUtil.makeBounceType(targetClass, lookup, ctorTypes, superCtorTypes, overridesTypes, overridesSuperTypes, overridesNames, overridesSuperNames, (ctorTypeList, superTypeList, classVisitor) -> {
                    var methodVisitor = classVisitor.visitMethod(
                            Opcodes.ACC_PUBLIC,
                            "<init>",
                            MethodType.methodType(void.class, ctorTypeList.parameterArray()).descriptorString(),
                            null,
                            null
                    );
                    methodVisitor.visitCode();
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                    int j = 1;
                    for (int i = 0; i < superTypeList.parameterCount(); i++) {
                        var ctorArgClass = ctorTypeList.parameterType(i);
                        var t = Type.getType(ctorArgClass);
                        methodVisitor.visitVarInsn(t.getOpcode(Opcodes.ILOAD), j);
                        var superArgClass = superTypeList.parameterType(i);
                        convertToType(methodVisitor, ctorArgClass, superArgClass);
                        j += t.getSize();
                    }
                    methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(targetClass), "<init>", MethodType.methodType(void.class, superTypeList.parameterArray()).descriptorString(), false);
                    methodVisitor.visitInsn(Opcodes.RETURN);
                    methodVisitor.visitMaxs(0, 0);
                    methodVisitor.visitEnd();
                }, (implTypes, superTypes, implName, superName, classVisitor, bounceClassType, isDefault) -> {
                    var superMethodVisitor = classVisitor.visitMethod(
                            Opcodes.ACC_PUBLIC,
                            superName,
                            superTypes.descriptorString(),
                            null,
                            null
                    );
                    superMethodVisitor.visitCode();
                    superMethodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                    int j = 1;
                    for (int i = 0; i < superTypes.parameterCount(); i++) {
                        var superArgClass = superTypes.parameterType(i);
                        var t = Type.getType(superArgClass);
                        superMethodVisitor.visitVarInsn(t.getOpcode(Opcodes.ILOAD), j);
                        var implArgClass = implTypes.parameterType(i);
                        convertToType(superMethodVisitor, superArgClass, implArgClass);
                        j += t.getSize();
                    }
                    superMethodVisitor.visitMethodInsn(isDefault ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, bounceClassType, implName, implTypes.descriptorString(), isDefault);
                    var superReturnType = superTypes.returnType();
                    var returnT = Type.getType(superReturnType);
                    var implReturnType = implTypes.returnType();
                    convertToType(superMethodVisitor, implReturnType, superReturnType);
                    if (superReturnType.equals(void.class)) {
                        superMethodVisitor.visitInsn(Opcodes.RETURN);
                    } else {
                        superMethodVisitor.visitInsn(returnT.getOpcode(Opcodes.IRETURN));
                    }
                    superMethodVisitor.visitMaxs(0, 0);
                    superMethodVisitor.visitEnd();

                    var implMethodVisitor = classVisitor.visitMethod(
                            Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                            implName,
                            implTypes.descriptorString(),
                            null,
                            null
                    );
                    implMethodVisitor.visitEnd();
                });
                boundeOverrides = true;
                if (targetClass.isInterface()) {
                    interfaces[0] = Type.getInternalName(bounceType);
                } else {
                    bouncesCtors = true;
                    superClass = bounceType;
                }
            }
        } catch (Throwable t) {
            throw new OpeningException("Could not bounce interface " + targetClass.getName() + " to a proxy interface", t);
        }

        String generatedClassName = Type.getInternalName(proxyData.targetClass()) + "$" + Type.getInternalName(holdingClass).replace('/','$') + "$" + constructionMethodName;

        classWriter.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, generatedClassName, null, Type.getInternalName(superClass), interfaces);

        for (var field : fields) {
            String fieldName = (String) field.get(0);
            var fieldType = (Class<?>) field.get(1);
            boolean isFinal = (boolean) field.get(2);
            
            if (isFinal && requiresManualAllocation) {
                throw new OpeningException("Extension "+holdingClass+" of target "+targetClass+" requires use of a dynamic module and manual allocation to construct, and thus cannot have final fields, but is defined with final field '"+fieldName+"'");
            }

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
            overrideName = remapMethod(overrideName, overrideType.descriptorString(), targetClass.getName(), holdingClass);
            Arrays.stream(holdingClass.getDeclaredMethods())
                    .filter(m -> m.getName().equals(name) && m.getParameterCount() == interfaceType.parameterCount() && m.getReturnType().equals(interfaceType.returnType()) && Arrays.equals(m.getParameterTypes(), interfaceType.parameterArray()))
                    .filter(Method::isDefault)
                    .findFirst()
                    .orElseThrow(() -> new OpeningException("Could not find interface method to bounce override to with name "+name+", type "+interfaceType));
            var parameterTypes = Arrays.stream(overrideType.parameterArray()).map(Type::getType).toArray(Type[]::new);
            var overrideDesc = Type.getMethodDescriptor(Type.getType(overrideType.returnType()), parameterTypes);
            if (boundeOverrides) {
                var methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, name, overrideType.descriptorString(), null, null);
                methodVisitor.visitCode();
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                int j = 1;
                for (int i = 0; i < overrideType.parameterCount(); i++) {
                    var t = Type.getType(overrideType.parameterType(i));
                    methodVisitor.visitVarInsn(t.getOpcode(Opcodes.ILOAD), j);
                    j += t.getSize();
                }
                methodVisitor.visitMethodInsn(
                        isInterface ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL,
                        Type.getInternalName(superClass),
                        name,
                        overrideType.descriptorString(),
                        isInterface
                );
                if (overrideType.returnType().equals(void.class)) {
                    methodVisitor.visitInsn(Opcodes.RETURN);
                } else {
                    methodVisitor.visitInsn(Type.getType(overrideType.returnType()).getOpcode(Opcodes.IRETURN));
                }
                methodVisitor.visitMaxs(1, 1);
                methodVisitor.visitEnd();
            } else {
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
        }

        for (var ctor : ctors) {
            MethodType ctorType;
            MethodType superType;
            try {
                ctorType = (MethodType) ((MethodHandle) ctor.get(0)).invoke(holdingClass.getClassLoader());
                if (bouncesCtors) {
                    var originalSuperType = (MethodType) ((MethodHandle) ctor.get(1)).invoke(holdingClass.getClassLoader());
                    var fieldParams = ctorType.parameterCount() - originalSuperType.parameterCount();
                    var newParamTypes = ctorType.parameterList().subList(fieldParams, ctorType.parameterCount());
                    superType = MethodType.methodType(ctorType.returnType(), newParamTypes);
                } else {
                    superType = (MethodType) ((MethodHandle) ctor.get(1)).invoke(holdingClass.getClassLoader());
                }
            } catch (Throwable e) {
                throw new OpeningException(e);
            }
            //noinspection unchecked
            var fieldsToSet = (List<String>) ctor.get(2);

            var ctorDesc = ctorType.descriptorString();
            var methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", ctorDesc, null, null);
            
            if (proxyData.isProxy() && !isInterface) {
                try {
                    Constructor<?> originalCtor = targetClass.getDeclaredConstructor(superType.parameterArray());
                    if ((originalCtor.getModifiers() & Opcodes.ACC_PRIVATE) != 0) {
                        // This proxy constructor is not visible, and we are using a proxy-generated dynamic module, so
                        // construction must happen through manual allocation and superclass invocation.
                        var annotationVisitor = methodVisitor.visitAnnotation(
                                Type.getDescriptor(ManualAllocation.class),
                                true
                        );
                        var arrayVisitor = annotationVisitor.visitArray("superConstructor");
                        for (var type : superType.parameterArray()) {
                            arrayVisitor.visit("superConstructor", Type.getType(type));
                        }
                        arrayVisitor.visitEnd();
                        annotationVisitor.visit("superClass", Type.getType(superClass));
                        arrayVisitor = annotationVisitor.visitArray("fields");
                        for (var field : fieldsToSet) {
                            arrayVisitor.visit("fields", field);
                        }
                        arrayVisitor.visitEnd();
                        annotationVisitor.visitEnd();
                    }
                } catch (NoSuchMethodException e) {
                    throw new OpeningException(e);
                }
            }
            
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
            MethodHandles.Lookup lookupIn;
            if (allVisible) {
                lookupIn = originalLookup.in(holdingClass);
            } else {
                lookupIn = lookup.in(proxyData.targetClass());
            }
            if (!proxyData.isProxy()) {
                try {
                    ProxyUtil.expose(targetClass, holdingClass, lookup);
                    ProxyUtil.expose(targetClass, OpeningMetafactory.class, lookup);
                } catch (Throwable t) {
                    throw new OpeningException("While opening, issues occured setting up module links", t);
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
