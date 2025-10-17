package dev.lukebemish.opensesame.runtime;

import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;

final class ClassCreatorUtils {
    private static final Path DUMP_DIRECTORY;
    
    static {
        var dumpDirectory = System.getProperty("dev.lukebemish.opensesame.class-dump-directory");
        if (dumpDirectory != null) {
            DUMP_DIRECTORY = Path.of(dumpDirectory);
        } else {
            DUMP_DIRECTORY = null;
        }
    }
    
    static MethodHandles.Lookup defineHiddenClass(MethodHandles.Lookup lookup, byte[] bytes, boolean initialize, MethodHandles.Lookup.ClassOption... options) throws IllegalAccessException {
        var defined = lookup.defineHiddenClass(bytes, initialize, options);
        dumpBytes(bytes, defined.lookupClass());
        return defined;
    }
    
    static MethodHandles.Lookup defineHiddenClassWithClassData(MethodHandles.Lookup lookup, byte[] bytes, Object data, boolean initialize, MethodHandles.Lookup.ClassOption... options) throws IllegalAccessException {
        var defined = lookup.defineHiddenClassWithClassData(bytes, data, initialize, options);
        dumpBytes(bytes, defined.lookupClass());
        return defined;
    }
    
    static Class<?> defineClass(MethodHandles.Lookup lookup, byte[] bytes) throws IllegalAccessException {
        var definedClass = lookup.defineClass(bytes);
        dumpBytes(bytes, definedClass);
        return definedClass;
    }
    
    private static void dumpBytes(byte[] bytes, Class<?> definedClass) {
        if (DUMP_DIRECTORY != null) {
            try {
                var path = DUMP_DIRECTORY.resolve(definedClass.getName().replace('.', '/') + ".class");
                Files.createDirectories(path.getParent());
                Files.write(path, bytes);
            } catch (Exception e) {
                throw new RuntimeException("Failed to dump generated class bytes for " + definedClass.getName(), e);
            }
        }
    }
}
