package dev.lukebemish.opensesame.test.java;

import dev.lukebemish.opensesame.annotations.Open;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import java.lang.invoke.MethodHandles;

@DisabledIf("wrongJavaVersion")
public class TestLookupProviderFFI {
    @Open(
            name = "openingLookup",
            targetName = "dev.lukebemish.opensesame.runtime.LookupProviderFFI",
            type = Open.Type.VIRTUAL,
            unsafe = true
    )
    private static MethodHandles.Lookup invokeOpeningLookup(Object provider, MethodHandles.Lookup original, Class<?> target) {
        throw new AssertionError("Method not transformed");
    }

    @Open(
            name = "<init>",
            targetName = "dev.lukebemish.opensesame.runtime.LookupProviderFFI",
            type = Open.Type.CONSTRUCT,
            unsafe = true
    )
    private static Object createLookupProvider() {
        throw new AssertionError("Method not transformed");
    }
    
    @Test
    void lookupHasOriginalAccess() throws Throwable {
        var provider = createLookupProvider();
        var implLookup = invokeOpeningLookup(provider, MethodHandles.lookup(), TestLookupProviderFFI.class);
        
        // Test if it is IMPL_LOOKUP, by using it to grab IMPL_LOOKUP
        Assertions.assertNotEquals(0, implLookup.lookupModes() & MethodHandles.Lookup.ORIGINAL);
        var getter = implLookup.findStaticGetter(MethodHandles.Lookup.class, "IMPL_LOOKUP", MethodHandles.Lookup.class);
        getter.invoke();
    }

    private static boolean wrongJavaVersion() {
        return Runtime.version().feature() < 22;
    }
}
