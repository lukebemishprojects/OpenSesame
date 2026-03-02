package dev.lukebemish.opensesame.runtime;

import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Tracks lookups of various permissions from a given source lookup to a target.
 */
class LookupState {
    private static final String EXPOSE_LOOKUP_FIELD = "$$dev$lukebemish$opensesame$$LOOKUP";
    
    private final boolean allowsUnsafe;
    private final Class<?> targetClass;
    private final MethodHandles.Lookup sourceLookup;
    private MethodHandles.@Nullable Lookup targetLookup;
    private MethodHandles.@Nullable Lookup targetOriginalLookup;
    private MethodHandles.@Nullable Lookup unsafeLookup;

    LookupState(boolean allowsUnsafe, Class<?> targetClass, MethodHandles.Lookup sourceLookup) {
        this.allowsUnsafe = allowsUnsafe;
        this.targetClass = targetClass;
        this.sourceLookup = sourceLookup;
        addModuleReads();
    }
    
    MethodHandles.Lookup sourceLookup() {
        return sourceLookup;
    }
    
    synchronized MethodHandles.Lookup targetLookup() {
        if (targetLookup != null) {
            return targetLookup;
        }
        try {
            targetLookup = MethodHandles.privateLookupIn(targetClass, sourceLookup);
        } catch (IllegalAccessException e) {
            targetLookup = targetOriginalLookup(List.of(e));
        }
        return targetLookup;
    }

    MethodHandles.Lookup targetOriginalLookup() {
        return targetOriginalLookup(List.of());
    }
    
    MethodHandles.Lookup unsafeLookup() {
        return unsafeLookup(List.of());
    }

    private synchronized MethodHandles.Lookup targetOriginalLookup(List<Throwable> suppressed) {
        if (targetOriginalLookup != null) {
            return targetOriginalLookup;
        }
        try {
            var targetPublicLookup = sourceLookup.in(targetClass);
            var getter = targetPublicLookup.findStaticGetter(targetClass, EXPOSE_LOOKUP_FIELD, MethodHandles.Lookup.class);
            targetOriginalLookup = (MethodHandles.Lookup) getter.invoke();
        } catch (Throwable e) {
            var allExceptions = new ArrayList<>(suppressed);
            allExceptions.add(e);
            targetOriginalLookup = unsafeLookup(allExceptions);
        }
        return targetOriginalLookup;
    }

    private record LookupProviderResults(@Nullable LookupProvider provider, Exception exception) {}
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
                    LOOKUP_PROVIDER1,
                    LOOKUP_PROVIDER1 == null ? LOOKUP_PROVIDER_EXCEPTION1 : null
            );
            LOOKUP_PROVIDER_UNSAFE_RESULTS = results;
            return results;
        }
    }
    
    private synchronized MethodHandles.Lookup unsafeLookup(List<Throwable> suppressed) {
        if (!allowsUnsafe) {
            var exception = new OpeningException("Unsafe lookup not allowed for this target, but could not obtain a safe lookup for the requested operation");
            for (var t : suppressed) {
                exception.addSuppressed(t);
            }
            throw exception;
        }
        if (unsafeLookup != null) {
            return unsafeLookup;
        }
        var results = getLookupProviderUnsafe();
        if (results.provider() != null) {
            unsafeLookup = results.provider().unsafeLookup();
        } else {
            var exception = results.exception();
            for (var t : suppressed) {
                exception.addSuppressed(t);
            }
            throw new OpeningException("Unable to obtain unsafe lookup via any provider", exception);
        }
        return unsafeLookup;
    }

    private void addModuleReads() {
        var sourceModule = sourceLookup.lookupClass().getModule();
        var targetModule = targetClass.getModule();
        try {
            var addReads = sourceLookup.findVirtual(Module.class, "addReads", MethodType.methodType(Module.class, Module.class));
            addReads.invoke(sourceModule, targetModule);
        } catch (Throwable e) {
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new OpeningException(e);
        }
    }
}
