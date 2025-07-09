package dev.lukebemish.opensesame.runtime;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

final class ProxyUtil {
    private ProxyUtil() {}
    
    record ProxyData(Class<?> targetClass, boolean isProxy) {}
    
    public static void expose(Class<?> viewer, Class<?> target, MethodHandles.Lookup unsafeLookup) throws Throwable {
        var viewerModule = viewer.getModule();
        var targetModule = target.getModule();
        
        if (viewerModule != targetModule && !viewerModule.canRead(targetModule)) {
            MethodHandle addReadsHandle;
            if (viewer.getName().startsWith("java.lang.invoke.")) {
                addReadsHandle = unsafeLookup.in(Annotation.class).findVirtual(Module.class, "addReads", MethodType.methodType(Module.class, Module.class));
            } else {
                addReadsHandle = unsafeLookup.in(viewer).findVirtual(Module.class, "addReads", MethodType.methodType(Module.class, Module.class));
            }
            addReadsHandle.invoke(viewer.getModule(), target.getModule());
        }

        if (viewerModule != targetModule && !targetModule.isExported(target.getPackageName(), viewerModule)) {
            MethodHandle addExportsHandle;
            if (target.getName().startsWith("java.lang.invoke.")) {
                addExportsHandle = unsafeLookup.in(Annotation.class).findVirtual(Module.class, "addExports", MethodType.methodType(Module.class, String.class, Module.class));
            } else {
                addExportsHandle = unsafeLookup.in(target).findVirtual(Module.class, "addExports", MethodType.methodType(Module.class, String.class, Module.class));
            }
            addExportsHandle.invoke(target.getModule(), target.getPackageName(), viewer.getModule());
        }
    }
    
    private static boolean canSee(Class<?> viewer, Class<?> target, MethodHandles.Lookup unsafeLookup, boolean strict) throws Throwable {
        expose(viewer, target, unsafeLookup);
        
        var generatedName = Type.getInternalName(viewer)+"$CheckCanSee";
        
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        writer.visit(
                Opcodes.V17,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                generatedName,
                null,
                Type.getInternalName(Object.class),
                new String[]{}
        );
        var check = writer.visitMethod(
                Opcodes.ACC_STATIC,
                "check",
                MethodType.methodType(Class.class).descriptorString(),
                null,
                null
        );
        check.visitCode();
        if (strict) {
            check.visitLdcInsn(Type.getType(target));
        } else {
            check.visitLdcInsn(target.getName());
            check.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    Type.getInternalName(Class.class),
                    "forName",
                    MethodType.methodType(Class.class, String.class).descriptorString(),
                    false
            );
        }
        check.visitInsn(Opcodes.ARETURN);
        check.visitMaxs(1, 1);
        check.visitEnd();
        writer.visitEnd();
        try {
            var handle = unsafeLookup.in(viewer).defineHiddenClass(writer.toByteArray(), false);
            var getter = handle.findStatic(handle.lookupClass(), "check", MethodType.methodType(Class.class));
            var clazz = (Class<?>) getter.invokeExact();
            return clazz == target;
        } catch (ClassNotFoundException | NoClassDefFoundError | IllegalAccessException | IllegalAccessError ignored) {
            return false;
        }
    }
    
    private static class DoubleDelegatingClassLoader extends ClassLoader {
        private final List<ClassLoader> delegates;

        DoubleDelegatingClassLoader(ClassLoader parent, List<ClassLoader> delegates) {
            super(parent);
            this.delegates = delegates;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            try {
                return super.loadClass(name, resolve);
            } catch (ClassNotFoundException e) {
                for (var cl : delegates) {
                    try {
                        return cl.loadClass(name);
                    } catch (ClassNotFoundException ignored) {
                        // Ignore and continue to the next delegate
                    }
                }
                throw e;
            }
        }
    }

    static ProxyData makeProxyModule(Class<?> target, Class<?> extensionClass, Collection<Class<?>> requiredClasses, MethodHandles.Lookup unsafeLookup) throws Throwable {
        if (canSee(target, extensionClass, unsafeLookup, true)) {
            return new ProxyData(target, false);
        }
        Class<?> targetInterface;
        if (target.isInterface() && (target.getModifiers() & Modifier.PUBLIC) != 0) {
            targetInterface = target;
        } else {
            targetInterface = makeInterfaceInPackage(target, unsafeLookup);
        }
        var interfaces = new ArrayList<Class<?>>();
        interfaces.add(targetInterface);
        interfaces.add(extensionClass);
        var modules = new HashSet<Module>();
        modules.add(targetInterface.getModule());
        modules.add(extensionClass.getModule());
        var actuallyRequiredClasses = new HashSet<Class<?>>();
        for (var requiredClass : requiredClasses) {
            if (requiredClass.getModule() == null) {
                continue;
            }
            while (requiredClass.isArray()) {
                requiredClass = requiredClass.getComponentType();
            }
            if (requiredClass.isPrimitive()) {
                continue;
            }
            actuallyRequiredClasses.add(requiredClass);
            if (!modules.add(requiredClass.getModule())) {
                Class<?> newInterface;
                if (requiredClass.isInterface() && (requiredClass.getModifiers() & Modifier.PUBLIC) != 0) {
                    newInterface = requiredClass;
                } else {
                    newInterface = makeInterfaceInPackage(requiredClass, unsafeLookup);
                }
                interfaces.add(newInterface);
            }
        }
        var classLoaders = interfaces.stream()
                .map(Class::getClassLoader)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        var proxyInstance = Proxy.newProxyInstance(
                new DoubleDelegatingClassLoader(extensionClass.getClassLoader(), classLoaders),
                interfaces.toArray(Class[]::new),
                (p, m, a) -> {
                    throw new UnsupportedOperationException("This dummy proxy should never be invoked.");
                }
        );
        for (var requiredClass : actuallyRequiredClasses) {
            var inProxyLookup = unsafeLookup.in(proxyInstance.getClass());
            try {
                inProxyLookup.accessClass(requiredClass);
            } catch (IllegalAccessException | SecurityException e) {
                throw new IllegalArgumentException("Generated proxy for target class "+target.getName()+" cannot access required class "+requiredClass.getName()+", which is not public.", e);
            }
            if (!canSee(proxyInstance.getClass(), requiredClass, unsafeLookup, true)) {
                throw new IllegalArgumentException("Generated proxy for target class "+target.getName()+" cannot access required class "+requiredClass.getName());
            }
        }
        return new ProxyData(proxyInstance.getClass(), true);
    }

    private static Class<?> makeInterfaceInPackage(Class<?> target, MethodHandles.Lookup unsafeLookup) throws Throwable {
        if (target.isArray()) {
            target = target.componentType();
        }
        String generatedName;
        do {
            generatedName = Type.getInternalName(target)+"$ProxyInterface$"+new Object().hashCode();
        } while (exists(generatedName, target));
        var writer = new ClassWriter(0);
        writer.visit(
                Opcodes.V17,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT,
                generatedName,
                null,
                Type.getInternalName(Object.class),
                new String[]{}
        );
        writer.visitEnd();
        return unsafeLookup.in(target).defineClass(writer.toByteArray());
    }
    
    interface CtorWriter {
        void writeConstructor(
                List<Class<?>> ctorTypes,
                List<Class<?>> superTypes,
                ClassVisitor classVisitor
        );
    }

    static Class<?> makeBounceType(Class<?> targetClass, MethodHandles.Lookup unsafeLookup, List<List<Class<?>>> ctorTypes, List<List<Class<?>>> superTypes, CtorWriter ctorWriter) throws Throwable {
        String generatedName;
        do {
            generatedName = Type.getInternalName(targetClass) + "$OpenSesameBounceInterface$" + new Object().hashCode();
        } while (exists(generatedName, targetClass));
        
        var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        writer.visit(
                Opcodes.V17,
                Opcodes.ACC_PUBLIC | (targetClass.isInterface() ? Opcodes.ACC_INTERFACE : 0) | Opcodes.ACC_ABSTRACT,
                generatedName,
                null,
                targetClass.isInterface() ? Type.getInternalName(Object.class) : Type.getInternalName(targetClass),
                targetClass.isInterface() ? new String[]{Type.getInternalName(targetClass)} : new String[] {}
        );
        if (!targetClass.isInterface()) {
            for (int i = 0; i < ctorTypes.size(); i++) {
                var ctorTypeList = ctorTypes.get(i);
                var superTypeList = superTypes.get(i);
                ctorWriter.writeConstructor(
                        ctorTypeList,
                        superTypeList,
                        writer
                );
            }
        }
        writer.visitEnd();

        return unsafeLookup.in(targetClass).defineClass(writer.toByteArray());
    }

    private static boolean exists(String generatedName, Class<?> target) {
        try {
            Class.forName(generatedName, false, target.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
