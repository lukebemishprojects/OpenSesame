package dev.lukebemish.opensesame.test.natives;

import dev.lukebemish.opensesame.natives.NativeImplementations;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

class TestNatives {
    @Test
    void testNativeImplLookup() throws Throwable {
        Class<?> clazz = NativeImplementations.class;
        var lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
        MethodHandle setup = lookup.findStatic(clazz, "setup", MethodType.methodType(void.class));
        MethodHandle nativeImplLookup = lookup.findStatic(clazz, "nativeImplLookup", MethodType.methodType(MethodHandles.Lookup.class));
        setup.invoke();
        MethodHandles.Lookup implLookup = (MethodHandles.Lookup) nativeImplLookup.invoke();

        // Test if it is IMPL_LOOKUP, by using it to grab IMPL_LOOKUP
        Assertions.assertNotEquals(0, implLookup.lookupModes() & MethodHandles.Lookup.ORIGINAL);
        var getter = implLookup.findStaticGetter(MethodHandles.Lookup.class, "IMPL_LOOKUP", MethodHandles.Lookup.class);
        getter.invoke();
    }
}
