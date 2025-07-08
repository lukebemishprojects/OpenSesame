package dev.lukebemish.opensesame.test.natives;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@DisabledIf("wrongJavaVersion")
public class TestFFI {
    @Test
    void testFFILookup() throws Throwable {
        Class<?> clazz = Class.forName("dev.lukebemish.opensesame.runtime.LookupProviderFFI");
        var lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
        var ctor = lookup.findConstructor(clazz, MethodType.methodType(void.class));
        var provider = ctor.invoke();
        var getLookup = lookup.findVirtual(clazz, "openingLookup", MethodType.methodType(MethodHandles.Lookup.class, MethodHandles.Lookup.class, Class.class));
        var implLookup = (MethodHandles.Lookup) getLookup.invoke(provider, MethodHandles.lookup(), TestFFI.class);

        // Test if it is IMPL_LOOKUP, by using it to grab IMPL_LOOKUP
        Assertions.assertNotEquals(0, implLookup.lookupModes() & MethodHandles.Lookup.ORIGINAL);
        var getter = implLookup.findStaticGetter(MethodHandles.Lookup.class, "IMPL_LOOKUP", MethodHandles.Lookup.class);
        getter.invoke();
    }

    private static boolean wrongJavaVersion() {
        return Runtime.version().feature() < 22;
    }
}
