package dev.lukebemish.opensesame.test.groovy.Open

import dev.lukebemish.opensesame.annotations.Open
import dev.lukebemish.opensesame.runtime.ClassProvider
import dev.lukebemish.opensesame.test.target.Public
import groovy.transform.CompileStatic
import groovyjarjarasm.asm.ClassWriter
import groovyjarjarasm.asm.Opcodes
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

@CompileStatic
class TestProvider {
    private static class SimpleProvider implements ClassProvider {
        @Override
        Class<?> provide(ClassLoader loader, String name) {
            try {
                return Class.forName(name, false, loader)
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e)
            }
        }
    }

    @Open(
            name = "privateStatic",
            targetName = "dev.lukebemish.opensesame.test.target.Public",
            targetProvider = SimpleProvider.class,
            type = Open.Type.STATIC
    )
    private static String simpleProviderTarget() {
        throw new RuntimeException()
    }

    @Test
    void testSimpleProvider() {
        assertEquals("privateStatic", simpleProviderTarget())
    }

    private static class InaccessibleProvider implements ClassProvider {
        private static final ClassLoader SINGLE_CLASS_LOADER

        static {
            var writer = new ClassWriter(
                    ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS
            )
            writer.visit(
                    Opcodes.V17,
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                    "dev/lukebemish/opensesame/test/generated/Generated",
                    null,
                    "java/lang/Object",
                    new String[0]
            )

            var methodWriter = writer.visitMethod(
                    Opcodes.ACC_PRIVATE,
                    "<init>",
                    "()V",
                    null,
                    null
            )
            methodWriter.visitCode()
            methodWriter.visitVarInsn(Opcodes.ALOAD, 0)
            methodWriter.visitMethodInsn(
                    Opcodes.INVOKESPECIAL,
                    "java/lang/Object",
                    "<init>",
                    "()V",
                    false
            )
            methodWriter.visitInsn(Opcodes.RETURN)
            methodWriter.visitMaxs(1, 1)
            methodWriter.visitEnd()

            writer.visitEnd()

            SINGLE_CLASS_LOADER = new ClassLoader(InaccessibleProvider.class.getClassLoader()) {
                @Override
                protected Class<?> findClass(String name) throws ClassNotFoundException {
                    if (name == 'dev.lukebemish.opensesame.test.generated.Generated') {
                        return defineClass(
                                name,
                                writer.toByteArray(),
                                0,
                                writer.toByteArray().length
                        )
                    }
                    throw new ClassNotFoundException(name)
                }
            }
        }

        @Override
        Class<?> provide(ClassLoader loader, String name) {
            try {
                return Class.forName(name, false, SINGLE_CLASS_LOADER)
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e)
            }
        }
    }

    @Open(
            targetName = "dev.lukebemish.opensesame.test.generated.Generated",
            targetProvider = InaccessibleProvider,
            type = Open.Type.CONSTRUCT
    )
    private static Object inaccessibleProviderTarget() {
        throw new RuntimeException()
    }

    @Test
    void testInaccessibleProvider() {
        assertEquals("Generated", inaccessibleProviderTarget().getClass().getSimpleName())
    }

    @Open(
            name = "privateStatic",
            targetProvider = { -> Public },
            type = Open.Type.STATIC
    )
    private static String closure0ProviderTarget() {
        throw new RuntimeException()
    }

    @Test
    void testClosure0Provider() {
        assertEquals("privateStatic", closure0ProviderTarget())
    }

    @Open(
            name = "privateStatic",
            targetProvider = { Class.forName('dev.lukebemish.opensesame.test.target.Public', false, it as ClassLoader) },
            type = Open.Type.STATIC
    )
    private static String closureImpliedProviderTarget() {
        throw new RuntimeException()
    }

    @Test
    void testClosureImpliedProvider() {
        assertEquals("privateStatic", closureImpliedProviderTarget())
    }

    @Open(
            name = "privateStatic",
            targetProvider = { ClassLoader cl -> Class.forName('dev.lukebemish.opensesame.test.target.Public', false, cl) },
            type = Open.Type.STATIC
    )
    private static String closure1ProviderTarget() {
        throw new RuntimeException()
    }

    @Test
    void testClosure1Provider() {
        assertEquals("privateStatic", closure1ProviderTarget())
    }

    @Open(
            name = "privateStatic",
            targetName = 'dev.lukebemish.opensesame.test.target.Public',
            targetProvider = { ClassLoader cl, String s -> Class.forName(s, false, cl) },
            type = Open.Type.STATIC
    )
    private static String closure2ProviderTarget() {
        throw new RuntimeException()
    }

    @Test
    void testClosure2Provider() {
        assertEquals("privateStatic", closure2ProviderTarget())
    }

    @Open(
            name = "privateStatic",
            targetName = 'dev.lukebemish.opensesame.test.target.Public',
            targetProvider = { Object cl, Object s -> Class.forName(s as String, false, cl as ClassLoader) },
            type = Open.Type.STATIC
    )
    private static String closureNonspecificProviderTarget() {
        throw new RuntimeException()
    }

    @Test
    void testClosureNonspecificProvider() {
        assertEquals("privateStatic", closure2ProviderTarget())
    }
}
