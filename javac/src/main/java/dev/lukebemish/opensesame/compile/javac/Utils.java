package dev.lukebemish.opensesame.compile.javac;

import com.sun.source.util.JavacTask;
import dev.lukebemish.opensesame.annotations.Coerce;
import dev.lukebemish.opensesame.annotations.Open;

final class Utils {
    private static final String BASIC_JAVA_TASK = "com.sun.tools.javac.api.BasicJavacTask";
    private static final String CONTEXT = "com.sun.tools.javac.util.Context";

    private Utils() {}

    @Open(
            name = "get",
            targetName = CONTEXT,
            type = Open.Type.VIRTUAL,
            unsafe = true
    )
    public static <T> T getFromContext(@Coerce(targetName = CONTEXT) Object ignoredContext, Class<T> ignoredKey) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "getContext",
            targetName = BASIC_JAVA_TASK,
            type = Open.Type.VIRTUAL,
            unsafe = true
    )
    static @Coerce(targetName = CONTEXT) Object getContext(@Coerce(targetName = BASIC_JAVA_TASK) JavacTask ignoredTask) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "instance",
            targetName = "com.sun.tools.javac.jvm.ClassWriter",
            type = Open.Type.STATIC,
            unsafe = true
    )
    static @Coerce(targetName = "com.sun.tools.javac.jvm.ClassWriter") Object getClassWriter(@Coerce(targetName = CONTEXT) Object ignoredContext) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "multiModuleMode",
            targetName = "com.sun.tools.javac.jvm.ClassWriter",
            type = Open.Type.GET_INSTANCE,
            unsafe = true
    )
    static boolean isMultiModuleMode(Object ignoredClassWriter) {
        throw new UnsupportedOperationException();
    }
}
