package dev.lukebemish.opensesame.runtime;

import org.jetbrains.annotations.ApiStatus;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

@ApiStatus.Internal
@SuppressWarnings("unused")
class LookupProviderFFI implements LookupProvider {
    private final MethodHandles.Lookup lookup;
    
    @SuppressWarnings("unused")
    private static MethodHandles.Lookup LOOKUP;

    @SuppressWarnings("unused")
    LookupProviderFFI() {
        this.lookup = retrieveLookup();
    }

    private synchronized static MethodHandles.Lookup retrieveLookup() {
        var contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(LookupProviderFFI.class.getClassLoader());
            try (Arena arena = Arena.ofConfined()) {
                try (var env = new Env(arena)) {
                    var lookupClass = env.getJniClass(MethodHandles.Lookup.class);

                    var implLookupFieldId = env.getStaticFieldId(lookupClass, "IMPL_LOOKUP", MethodHandles.Lookup.class);
                    var implLookupValue = env.getStaticObjectField(lookupClass, implLookupFieldId);
                    
                    var threadClass = env.getJniClass(Thread.class);
                    var currentThreadMethodId = env.getStaticMethodId(threadClass, "currentThread", MethodType.methodType(Thread.class));
                    var currentThreadValue = env.callStaticMethod(threadClass, currentThreadMethodId);
                    var getContextClassLoaderMethodId = env.getMethodId(threadClass, "getContextClassLoader", MethodType.methodType(ClassLoader.class));
                    var contextClassLoaderValue = env.callObjectMethod(currentThreadValue, getContextClassLoaderMethodId);
                    
                    var classClass = env.getJniClass(Class.class);
                    
                    var forNameMethodId = env.getStaticMethodId(classClass, "forName", MethodType.methodType(Class.class, String.class, boolean.class, ClassLoader.class));
                    
                    var selfClassName = env.newString(LookupProviderFFI.class.getName());
                    var selfClass = env.callObjectMethod(
                            classClass,
                            forNameMethodId,
                            selfClassName,
                            arena.allocateFrom(ValueLayout.JAVA_BYTE, (byte) 1),
                            contextClassLoaderValue
                    );
                    
                    var lookupFieldId = env.getStaticFieldId(selfClass, "LOOKUP", MethodHandles.Lookup.class);
                    env.putStaticObjectField(selfClass, lookupFieldId, implLookupValue);

                    var lookup = LOOKUP;
                    if (lookup == null) {
                        throw new RuntimeException("Failed to capture IMPL_LOOKUP; FFI code successfully invoked, but callback field is empty");
                    }
                    return lookup;
                }
            } catch (Throwable t) {
                if (t instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                } else {
                    throw new RuntimeException(t);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    private static class Env implements AutoCloseable {
        private final Arena arena;
        private final MemorySegment env;
        private final List<MemorySegment> globalRefs = new ArrayList<>();

        private Env(Arena arena) throws Throwable {
            this.arena = arena;
            var symbolLookup = SymbolLookup.libraryLookup(System.mapLibraryName("jvm"), arena);

            this.JNI_GetCreatedJavaVMs = Linker.nativeLinker().downcallHandle(
                    symbolLookup.find("JNI_GetCreatedJavaVMs").orElseThrow(), FunctionDescriptor.of(
                            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS
                    )
            );
            var javaVm = getJavaVm();
            this.GetEnv = Linker.nativeLinker().downcallHandle(
                    getFunction(javaVm, 6), FunctionDescriptor.of(
                            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT
                    )
            );

            this.env = getJniEnv(javaVm);
            
            this.FindClass = Linker.nativeLinker().downcallHandle(
                    getFunction(env, 6), FunctionDescriptor.of(
                            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS
                    )
            );
            this.GetStaticFieldID = Linker.nativeLinker().downcallHandle(
                    getFunction(env, 144), FunctionDescriptor.of(
                            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS
                    )
            );
            this.GetStaticObjectField = Linker.nativeLinker().downcallHandle(
                    getFunction(env, 145), FunctionDescriptor.of(
                            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS
                    )
            );
            this.SetStaticObjectField = Linker.nativeLinker().downcallHandle(
                    getFunction(env, 154), FunctionDescriptor.ofVoid(
                            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS
                    )
            );
            this.GetMethodID = Linker.nativeLinker().downcallHandle(
                    getFunction(env, 33), FunctionDescriptor.of(
                            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS
                    )
            );
            this.GetStaticMethodID = Linker.nativeLinker().downcallHandle(
                    getFunction(env, 113), FunctionDescriptor.of(
                            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS
                    )
            );
            this.NewGlobalRef = Linker.nativeLinker().downcallHandle(
                    getFunction(env, 21), FunctionDescriptor.of(
                            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS
                    )
            );
            this.DeleteGlobalRef = Linker.nativeLinker().downcallHandle(
                    getFunction(env, 22), FunctionDescriptor.ofVoid(
                            ValueLayout.ADDRESS, ValueLayout.ADDRESS
                    )
            );
            this.NewString = Linker.nativeLinker().downcallHandle(
                    getFunction(env, 163), FunctionDescriptor.of(
                            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT
                    )
            );
        }
        
        private final MethodHandle JNI_GetCreatedJavaVMs; // JavaVM **vmBuf, jsize bufLen, jsize *nVMs -> jint
        private final MethodHandle GetEnv; // JavaVM *vm, void **env, jint version -> jint
        private final MethodHandle FindClass; // JNIEnv *env, const char *name -> jclass
        private final MethodHandle GetStaticFieldID; // JNIEnv *env, jclass clazz, const char *name, const char *sig -> jfieldID
        private final MethodHandle GetStaticObjectField; // JNIEnv *env, jclass clazz, jfieldID fieldID -> jobject
        private final MethodHandle SetStaticObjectField; // JNIEnv *env, jclass clazz, jfieldID fieldID, jobject value -> void
        private final MethodHandle GetMethodID; // JNIEnv *env, jclass clazz, const char *name, const char *sig -> jmethodID
        private final MethodHandle GetStaticMethodID; // JNIEnv *env, jclass clazz, const char *name, const char *sig -> jmethodID
        private final MethodHandle NewGlobalRef; // JNIEnv *env, jobject obj -> jobject
        private final MethodHandle DeleteGlobalRef; // JNIEnv *env, jobject obj -> void
        private final MethodHandle NewString; // JNIEnv *env, const jchar *unicodeChars, jsize len -> jstring

        private static final long PTR_SIZE = ValueLayout.ADDRESS.byteSize();
        private static final int JNI_VERSION_21 = 0x00150000;

        private MemorySegment getJavaVm() throws Throwable {
            final MemorySegment vmCount = arena.allocate(ValueLayout.JAVA_INT);
            final MemorySegment vmRef = arena.allocate(ValueLayout.ADDRESS);
            checkError((int) JNI_GetCreatedJavaVMs.invokeExact(vmRef, 1, vmCount));
            if (vmCount.get(ValueLayout.JAVA_INT, 0) < 1) {
                throw new IllegalStateException("No JavaVM available");
            }
            return vmRef.get(ValueLayout.ADDRESS, 0);
        }

        private MemorySegment getJniEnv(MemorySegment vm) throws Throwable {
            final MemorySegment envRef = arena.allocate(ValueLayout.ADDRESS);
            checkError((int) GetEnv.invokeExact(vm, envRef, JNI_VERSION_21));
            return envRef.get(ValueLayout.ADDRESS, 0);
        }

        private MemorySegment getJniClass(Class<?> clazz) throws Throwable {
            return (MemorySegment) withGlobalRef(FindClass).invoke(env, arena.allocateFrom(clazz.getName().replace('.', '/')));
        }
        
        private MemorySegment getStaticFieldId(MemorySegment clazz, String name, @SuppressWarnings("SameParameterValue") Class<?> type) throws Throwable {
            return (MemorySegment) GetStaticFieldID.invoke(env, clazz, arena.allocateFrom(name), arena.allocateFrom(type.descriptorString()));
        }
        
        private MemorySegment getStaticObjectField(MemorySegment clazz, MemorySegment fieldId) throws Throwable {
            return (MemorySegment) withGlobalRef(GetStaticObjectField).invoke(env, clazz, fieldId);
        }
        
        private MethodHandle withGlobalRef(MethodHandle handle) {
            return MethodHandles.filterReturnValue(handle, NewGlobalRef.bindTo(env));
        }

        private MemorySegment newString(String str) throws Throwable {
            MemorySegment chars = arena.allocateFrom(ValueLayout.JAVA_CHAR, str.toCharArray());
            return (MemorySegment) withGlobalRef(NewString).invoke(env, chars, str.length());
        }
        
        private void deleteGlobalRef(MemorySegment obj) throws Throwable {
            DeleteGlobalRef.invoke(env, obj);
        }

        private void putStaticObjectField(MemorySegment clazz, MemorySegment fieldId, MemorySegment value) throws Throwable {
            SetStaticObjectField.invoke(env, clazz, fieldId, value);
        }

        private MemorySegment getMethodId(MemorySegment clazz, @SuppressWarnings("SameParameterValue") String name, MethodType type) throws Throwable {
            return (MemorySegment) GetMethodID.invoke(env, clazz, arena.allocateFrom(name), arena.allocateFrom(type.descriptorString()));
        }

        private MemorySegment getStaticMethodId(MemorySegment clazz, String name, MethodType type) throws Throwable {
            return (MemorySegment) GetStaticMethodID.invoke(env, clazz, arena.allocateFrom(name), arena.allocateFrom(type.descriptorString()));
        }
        
        private MemorySegment callObjectMethod(MemorySegment obj, MemorySegment methodId, MemorySegment... args) throws Throwable {
            // JNIEnv *env, jobject obj, jmethodID methodID, ... -> jobject
            var argsLayout = new ArrayList<MemoryLayout>();
            for (int i = 0; i < args.length + 3; i++) {
                argsLayout.add(ValueLayout.ADDRESS);
            }
            var CallObjectMethod = Linker.nativeLinker().downcallHandle(
                    getFunction(env, 34), FunctionDescriptor.of(
                            ValueLayout.ADDRESS, argsLayout.toArray(MemoryLayout[]::new)
                    )
            ).asSpreader(MemorySegment[].class, args.length);
            return (MemorySegment) withGlobalRef(CallObjectMethod).invoke(env, obj, methodId, args);
        }

        private MemorySegment callStaticMethod(MemorySegment clazz, MemorySegment methodId, MemorySegment... args) throws Throwable {
            // JNIEnv *env, jclass clazz, jmethodID methodID, ... -> jobject
            var argsLayout = new ArrayList<MemoryLayout>();
            for (int i = 0; i < args.length + 3; i++) {
                argsLayout.add(ValueLayout.ADDRESS);
            }
            var CallObjectMethod = Linker.nativeLinker().downcallHandle(
                    getFunction(env, 114), FunctionDescriptor.of(
                            ValueLayout.ADDRESS, argsLayout.toArray(MemoryLayout[]::new)
                    )
            ).asSpreader(MemorySegment[].class, args.length);
            return (MemorySegment) withGlobalRef(CallObjectMethod).invoke(env, clazz, methodId, args);
        }

        private static MemorySegment getFunction(MemorySegment obj, int function) {
            return obj.reinterpret(PTR_SIZE)
                    .get(ValueLayout.ADDRESS, 0)
                    .reinterpret((function + PTR_SIZE) * PTR_SIZE)
                    .getAtIndex(ValueLayout.ADDRESS, function);
        }

        private static void checkError(int err) {
            if (err != 0) {
                throw new RuntimeException("Error with JNI: "+err);
            }
        }

        @Override
        public void close() {
            try {
                for (MemorySegment ref : globalRefs) {
                    deleteGlobalRef(ref);
                }
                globalRefs.clear();
            } catch (Throwable t) {
                throwUnchecked(t);
            }
        }

        @SuppressWarnings("unchecked")
        private static <T, X extends Throwable> void throwUnchecked(T throwable) throws X {
            throw (X) throwable;
        }
    }
    
    @Override
    public MethodHandles.Lookup openingLookup(MethodHandles.Lookup original, Class<?> target) {
        return this.lookup;
    }
}
