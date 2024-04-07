package dev.lukebemish.opensesame.compile.groovy

import com.google.auto.service.AutoService
import dev.lukebemish.opensesame.annotations.Open
import dev.lukebemish.opensesame.annotations.extend.Extend
import dev.lukebemish.opensesame.compile.OpenSesameGenerated
import groovy.transform.CompileStatic
import groovyjarjarasm.asm.ClassWriter
import groovyjarjarasm.asm.Opcodes
import groovyjarjarasm.asm.Type
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.classgen.asm.BytecodeHelper
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
@AutoService(ASTTransformation)
class Discoverer implements ASTTransformation {
    public static final String MIXIN_LINES_META = 'dev.lukebemish.opensesame:mixinLines'

    private static final ClassNode OPEN = ClassHelper.makeWithoutCaching(Open)
    private static final ClassNode EXTEND = ClassHelper.makeWithoutCaching(Extend)

    private final OpenTransformation openTransformation = new OpenTransformation()
    private final ExtendTransformation extendTransformation = new ExtendTransformation()

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        List<ClassNode> classes = new ArrayList<>(source.AST.classes)
        for (def classNode : classes) {
            boolean ran = false
            if (!classNode.getAnnotations(EXTEND).empty) {
                for (def annotation : classNode.getAnnotations(EXTEND)) {
                    ran = true
                    extendTransformation.visit(new ASTNode[] {annotation, classNode}, source)
                }
            }
            List<MethodNode> methods = new ArrayList<>(classNode.methods)
            for (def method : methods) {
                if (!method.getAnnotations(OPEN).empty) {
                    for (def annotation : method.getAnnotations(OPEN)) {
                        ran = true
                        openTransformation.visit(new ASTNode[] {annotation, method}, source)
                    }
                }
            }
            if (ran) {
                // post-process
                Map<MixinProviderType, List<String>> lines = classNode.getNodeMetaData(MIXIN_LINES_META)
                if (lines != null && !lines.empty) {
                    Type selfType = Type.getType(BytecodeHelper.getTypeDescription(classNode))
                    Path rootPath = source.configuration.targetDirectory.toPath()
                    writeMixinProviderLines(lines, selfType, rootPath)
                }
            }
        }
    }

    enum MixinProviderType {
        UNFINAL("unFinal"),
        EXPOSE_TO_OVERRIDE("exposeToOverride");

        private final String methodName

        MixinProviderType(String methodName) {
            this.methodName = methodName
        }
    }

    private static final String MIXIN_PROVIDER_SERVICE = '$$dev$lukebemish$opensesame$$MixinActionProvider'
    private static final String MIXIN_PACKAGE = 'dev/lukebemish/opensesame/mixin/targets'
    private static final Type MIXIN = Type.getObjectType('org/spongepowered/asm/mixin/Mixin')
    private static final Type UNFINAL = Type.getObjectType('dev/lukebemish/opensesame/mixin/annotations/UnFinal')
    private static final Type MIXIN_PROVIDER = Type.getObjectType('dev/lukebemish/opensesame/mixin/plugin/OpenSesameMixinProvider')

    protected static void writeMixinProviderLines(Map<MixinProviderType, List<String>> lines, Type selfType, Path rootPath) throws IOException {
        if (rootPath != null) {
            Set<Type> targets = new HashSet<>()
            lines.forEach((k, v) -> {
                for (String line : v) {
                    String type = line.split("\\.")[0]
                    targets.add(Type.getObjectType(type))
                }
            })
            List<Type> orderedTargets = new ArrayList<>(targets)
            Map<Type, Integer> targetIndexes = new HashMap<>()
            for (int i = 0; i < orderedTargets.size(); i++) {
                targetIndexes.put(orderedTargets.get(i), i)
            }
            String generatedClassName = selfType.getInternalName() + MIXIN_PROVIDER_SERVICE
            Path generatedClassPath = rootPath.resolve(generatedClassName + ".class")
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS)
            writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, generatedClassName, null, "java/lang/Object", new String[]{MIXIN_PROVIDER.getInternalName()})
            var generated = writer.visitAnnotation(OpenSesameGenerated.class.descriptorString(), false)
            generated.visit("value", UNFINAL)
            generated.visitEnd()
            var initWriter = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null)
            initWriter.visitCode()
            initWriter.visitVarInsn(Opcodes.ALOAD, 0)
            initWriter.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
            initWriter.visitInsn(Opcodes.RETURN)
            initWriter.visitMaxs(1, 1)
            initWriter.visitEnd()
            for (MixinProviderType type : MixinProviderType.values()) {
                List<String> specificLines = lines.getOrDefault(type, List.of())
                if (!lines.isEmpty()) {
                    generateImpl(type.methodName, selfType, writer, specificLines, targetIndexes)
                }
            }
            Files.write(generatedClassPath, writer.toByteArray())
            var serviceFile = rootPath.resolve("META-INF/services/" + MIXIN_PROVIDER.getInternalName().replace('/', '.'))
            if (!Files.exists(serviceFile)) {
                Files.createDirectories(serviceFile.getParent())
                Files.createFile(serviceFile)
            }
            Files.write(serviceFile, List.of(generatedClassName.replace('/','.')), StandardOpenOption.APPEND)

            for (Type target : targets) {
                int index = targetIndexes.get(target)
                makeMixins(true, true, selfType, target, index, rootPath)
                makeMixins(true, false, selfType, target, index, rootPath)
                makeMixins(false, true, selfType, target, index, rootPath)
                makeMixins(false, false, selfType, target, index, rootPath)
            }
        }
    }
    
    private static void generateImpl(String implMethodName, Type selfType, ClassWriter writer, List<String> typeLines, Map<Type, Integer> targetIndexes) {
        var implWriter = writer.visitMethod(Opcodes.ACC_PUBLIC, implMethodName, "()[Ljava/lang/String;", null, null)
        implWriter.visitCode()
        implWriter.visitLdcInsn(typeLines.size())
        implWriter.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String")
        for (int i = 0; i < typeLines.size(); i++) {
            implWriter.visitInsn(Opcodes.DUP)
            implWriter.visitLdcInsn(i)
            var lineEnd = typeLines.get(i)
            var targetClass = Type.getObjectType(lineEnd.split("\\.")[0])
            var mixinPackageFull = selfType.getInternalName() + '$' + targetIndexes.get(targetClass)
            implWriter.visitLdcInsn(mixinPackageFull +"."+lineEnd)
            implWriter.visitInsn(Opcodes.AASTORE)
        }
        implWriter.visitInsn(Opcodes.ARETURN)
        implWriter.visitMaxs(3, 1)
        implWriter.visitEnd()
        writer.visitEnd()
    }

    private static void makeMixins(boolean forPublic, boolean forClass, Type holderType, Type targetType, int index, Path rootPath) throws IOException {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS)
        String originalName = holderType.getInternalName()
        String mixinName = MIXIN_PACKAGE + "/" + originalName + '$' + index + "/" + (forPublic ? "public" : "private") + (forClass ? "class" : "interface")
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | (forClass ? 0 : (Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE)), mixinName, null, "java/lang/Object", new String[0])
        var mixin = writer.visitAnnotation(MIXIN.getDescriptor(), false)
        if (forPublic) {
            var v = mixin.visitArray("value")
            v.visit(null, targetType)
            v.visitEnd()
        } else {
            var v = mixin.visitArray("targets")
            v.visit(null, targetType.getInternalName().replace('/', '.'))
            v.visitEnd()
        }
        mixin.visitEnd()
        var generated = writer.visitAnnotation(OpenSesameGenerated.class.descriptorString(), false)
        generated.visit("value", UNFINAL)
        generated.visitEnd()
        if (forClass) {
            var initWriter = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null)
            initWriter.visitCode()
            initWriter.visitVarInsn(Opcodes.ALOAD, 0)
            initWriter.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
            initWriter.visitInsn(Opcodes.RETURN)
            initWriter.visitMaxs(1, 1)
            initWriter.visitEnd()
        }
        writer.visitEnd()
        var mixinPath = rootPath.resolve(mixinName + ".class")
        Files.createDirectories(mixinPath.getParent())
        Files.write(mixinPath, writer.toByteArray())
    }
}
