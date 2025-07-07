package dev.lukebemish.opensesame.runtime;

import org.jetbrains.annotations.ApiStatus;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@ApiStatus.Internal
class LookupProviderNative implements LookupProvider {
    private final MethodHandles.Lookup lookup;

    LookupProviderNative() {
        try {
            Class<?> clazz = Class.forName("dev.lukebemish.opensesame.natives.NativeImplementations");
            var nativesLookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
            MethodHandle setup = nativesLookup.findStatic(clazz, "setup", MethodType.methodType(void.class));
            MethodHandle nativeImplLookup = nativesLookup.findStatic(clazz, "nativeImplLookup", MethodType.methodType(MethodHandles.Lookup.class));
            setup.invoke();
            this.lookup = (MethodHandles.Lookup) nativeImplLookup.invoke();
        } catch (Throwable t) {
            throw new RuntimeException("Error calling native library", t);
        }
    }

    @Override
    public MethodHandles.Lookup openingLookup(MethodHandles.Lookup original, Class<?> target) {
        return this.lookup;
    }
}
