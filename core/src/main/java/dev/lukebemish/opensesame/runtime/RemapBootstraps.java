package dev.lukebemish.opensesame.runtime;

import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class RemapBootstraps {
    private static final String REMAP_REFERENCE_SUFFIX = "$$dev$lukebemish$opensesame$$REMAP_REFERENCE";
    
    public static String remapTag(MethodHandles.Lookup lookup, String tag) {
        // - grab the target class bytecode
        // - parse (classfile or ASM, depending), grab the method with name tag
        // - find the single method/field instruction in it, grab the method/field name
        // - in the case of ambiguity, look for the last such method/insn
        var resourceName = "/" + lookup.lookupClass().getName().replace('.', '/') + REMAP_REFERENCE_SUFFIX + ".class";
        try {
            var getResourceAsStream = lookup.findVirtual(Class.class, "getResourceAsStream", MethodType.methodType(InputStream.class, String.class));
            try (var stream = (InputStream) getResourceAsStream.invoke(lookup.lookupClass(), resourceName)) {
                if (stream == null) {
                    throw new OpeningException("Remap reference resource not found for "+lookup.lookupClass().getName());
                }
                return RemapReferenceParser.findTag(stream, tag);
            } catch (Throwable e) {
                throw new OpeningException("Issue loading remap reference resource for "+lookup.lookupClass().getName(), e);
            }
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new OpeningException(e);
        }
    }
}
