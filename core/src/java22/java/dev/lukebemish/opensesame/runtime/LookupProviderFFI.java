package dev.lukebemish.opensesame.runtime;

import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

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

@ApiStatus.Internal
@SuppressWarnings("unused")
class LookupProviderFFI implements LookupProvider {
    private final MethodHandles.Lookup lookup;

    @SuppressWarnings("unused")
    LookupProviderFFI() {
        this.lookup = retrieveLookup();
    }   
    
    // Hopefully, a unique ID... and yes, making a dummy class is slightly cursed
    private static final String GENERATED_CLASS_NAME = "java/lang/"+LookupProviderFFI.class.getName().replace('.', '$')+"$IMPL_LOOKUP$"+new Object().hashCode();
    private static final String TEMP_FIELD_NAME = "IMPL_LOOKUP_TEMP";

    private static byte[] generateClassBytes() {
        var writer = new ClassWriter(0);
        writer.visit(Opcodes.V22, Opcodes.ACC_PUBLIC, GENERATED_CLASS_NAME, null, Type.getInternalName(Object.class), new String[] {});
        writer.visitField(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, TEMP_FIELD_NAME, Type.getDescriptor(MethodHandles.Lookup.class), null, null)
                .visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }
    
    private synchronized static MethodHandles.Lookup retrieveLookup() {
        try {
            return findValueFromGenerated();
        } catch (ClassNotFoundException ignored) {} catch (Throwable e) {
            throw new RuntimeException("Failed to retrieve IMPL_LOOKUP from generated class", e);
        }
        try (Arena arena = Arena.ofConfined()) {
            var env = new Env(arena);
            
            var lookupClass = env.newGlobalRef(env.getJniClass(MethodHandles.Lookup.class));
            
            var implLookupFieldId = env.getStaticFieldId(lookupClass, "IMPL_LOOKUP", MethodHandles.Lookup.class);
            var implLookupValue = env.newGlobalRef(env.getStaticObjectField(lookupClass, implLookupFieldId));
            
            var objectClass = env.newGlobalRef(env.getJniClass(Object.class));
            var inMethodId = env.getMethodId(lookupClass, "in", MethodType.methodType(MethodHandles.Lookup.class, Class.class));
            var implLookupInObjectValue = env.newGlobalRef(env.callObjectMethod(implLookupValue, inMethodId, objectClass));
            
            var defineClassMethodId = env.getMethodId(lookupClass, "defineClass", MethodType.methodType(Class.class, byte[].class));
            var bytes = env.makeByteArray(generateClassBytes());
            var definedClass = env.newGlobalRef(env.callObjectMethod(implLookupInObjectValue, defineClassMethodId, bytes));
            
            var tempFieldId = env.getStaticFieldId(definedClass, TEMP_FIELD_NAME, MethodHandles.Lookup.class);
            env.putStaticObjectField(definedClass, tempFieldId, implLookupValue);
            
            env.deleteGlobalRef(lookupClass);
            env.deleteGlobalRef(objectClass);
            env.deleteGlobalRef(definedClass);
            env.deleteGlobalRef(bytes);
            env.deleteGlobalRef(implLookupValue);
            env.deleteGlobalRef(implLookupInObjectValue);
            
            return findValueFromGenerated();
        } catch (Throwable t) {
            if (t instanceof RuntimeException runtimeException) {
                throw runtimeException;
            } else {
                throw new RuntimeException(t);
            }
        }
    }

    private static MethodHandles.Lookup findValueFromGenerated() throws Throwable {
        var clazz = Class.forName(GENERATED_CLASS_NAME.replace('/', '.'));
        var getter = MethodHandles.lookup().findStaticGetter(clazz, TEMP_FIELD_NAME, MethodHandles.Lookup.class);
        var setter = MethodHandles.lookup().findStaticSetter(clazz, TEMP_FIELD_NAME, MethodHandles.Lookup.class);
        var value = (MethodHandles.Lookup) getter.invokeExact();
        setter.invokeExact((MethodHandles.Lookup) null);
        return value;
    }

    private static class Env {
        private final Arena arena;
        private final MemorySegment env;

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
            this.NewByteArray = Linker.nativeLinker().downcallHandle(
                    getFunction(env, 176), FunctionDescriptor.of(
                            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT
                    )
            );
            this.ReleaseByteArrayElements = Linker.nativeLinker().downcallHandle(
                    getFunction(env, 192), FunctionDescriptor.ofVoid(
                            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT
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
        }
        
        private final MethodHandle JNI_GetCreatedJavaVMs; // JavaVM **vmBuf, jsize bufLen, jsize *nVMs -> jint
        private final MethodHandle GetEnv; // JavaVM *vm, void **env, jint version -> jint
        private final MethodHandle FindClass; // JNIEnv *env, const char *name -> jclass
        private final MethodHandle GetStaticFieldID; // JNIEnv *env, jclass clazz, const char *name, const char *sig -> jfieldID
        private final MethodHandle GetStaticObjectField; // JNIEnv *env, jclass clazz, jfieldID fieldID -> jobject
        private final MethodHandle SetStaticObjectField; // JNIEnv *env, jclass clazz, jfieldID fieldID, jobject value -> void
        private final MethodHandle GetMethodID; // JNIEnv *env, jclass clazz, const char *name, const char *sig -> jmethodID
        private final MethodHandle NewByteArray; // JNIEnv *env, jsize length -> jbyteArray
        private final MethodHandle ReleaseByteArrayElements; // JNIEnv *env, jbyteArray array, jbyte *elems, jint mode -> void
        private final MethodHandle NewGlobalRef; // JNIEnv *env, jobject obj -> jobject
        private final MethodHandle DeleteGlobalRef; // JNIEnv *env, jobject obj -> void
        
        private static final int JNI_COMMIT = 1;
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
            return (MemorySegment) FindClass.invoke(env, arena.allocateFrom(clazz.getName().replace('.', '/')));
        }
        
        private MemorySegment getStaticFieldId(MemorySegment clazz, String name, @SuppressWarnings("SameParameterValue") Class<?> type) throws Throwable {
            return (MemorySegment) GetStaticFieldID.invoke(env, clazz, arena.allocateFrom(name), arena.allocateFrom(type.descriptorString()));
        }
        
        private MemorySegment getStaticObjectField(MemorySegment clazz, MemorySegment fieldId) throws Throwable {
            return (MemorySegment) GetStaticObjectField.invoke(env, clazz, fieldId);
        }
        
        private MemorySegment newGlobalRef(MemorySegment obj) throws Throwable {
            return (MemorySegment) NewGlobalRef.invoke(env, obj);
        }
        
        private void deleteGlobalRef(MemorySegment obj) throws Throwable {
            DeleteGlobalRef.invoke(env, obj);
        }
        
        private MemorySegment makeByteArray(byte[] bytes) throws Throwable {
            var layout = MemoryLayout.sequenceLayout(bytes.length, ValueLayout.JAVA_BYTE);
            // JNIEnv *env, jbyteArray array, jboolean *isCopy -> jbyte *
            var GetByteArrayElements = Linker.nativeLinker().downcallHandle(
                    getFunction(env, 184), FunctionDescriptor.of(
                            ValueLayout.ADDRESS.withTargetLayout(layout), ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS
                    )
            );
            
            MemorySegment from = MemorySegment.ofArray(bytes);
            MemorySegment array = newGlobalRef((MemorySegment) NewByteArray.invoke(env, bytes.length));
            MemorySegment elements = (MemorySegment) GetByteArrayElements.invoke(env, array, MemorySegment.NULL);
            
            elements.copyFrom(from);
            ReleaseByteArrayElements.invoke(env, array, elements, JNI_COMMIT);
            
            return array;
        }
        
        private void putStaticObjectField(MemorySegment clazz, MemorySegment fieldId, MemorySegment value) throws Throwable {
            SetStaticObjectField.invoke(env, clazz, fieldId, value);
        }
        
        private MemorySegment getMethodId(MemorySegment clazz, @SuppressWarnings("SameParameterValue") String name, MethodType type) throws Throwable {
            return (MemorySegment) GetMethodID.invoke(env, clazz, arena.allocateFrom(name), arena.allocateFrom(type.descriptorString()));
        }
        
        @SuppressWarnings("UnusedReturnValue")
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
            return (MemorySegment) CallObjectMethod.invoke(env, obj, methodId, args);
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
    }
    
    @Override
    public MethodHandles.Lookup openingLookup(MethodHandles.Lookup original, Class<?> target) {
        return this.lookup;
    }
}
