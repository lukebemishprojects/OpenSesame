package dev.lukebemish.opensesame.compile.asm;

import dev.lukebemish.opensesame.annotations.Coerce;
import dev.lukebemish.opensesame.annotations.Open;
import dev.lukebemish.opensesame.annotations.extend.Constructor;
import dev.lukebemish.opensesame.annotations.extend.Extend;
import dev.lukebemish.opensesame.annotations.extend.Field;
import dev.lukebemish.opensesame.annotations.extend.Overrides;
import dev.lukebemish.opensesame.compile.ConDynUtils;
import dev.lukebemish.opensesame.compile.OpenSesameGenerated;
import dev.lukebemish.opensesame.compile.Processor;
import dev.lukebemish.opensesame.compile.TypeProvider;
import dev.lukebemish.opensesame.runtime.Extension;
import dev.lukebemish.opensesame.runtime.OpeningMetafactory;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public class VisitingProcessor extends ClassVisitor implements Processor<Type, VisitingProcessor.Annotation, VisitingProcessor.Method> {
    private static final Type UNFINAL = Type.getObjectType("dev/lukebemish/opensesame/mixin/annotations/UnFinal");
    private static final Type MIXIN = Type.getObjectType("org/spongepowered/asm/mixin/Mixin");
    private static final Type UNFINAL_LINE_PROVIDER = Type.getObjectType("dev/lukebemish/opensesame/mixin/plugin/UnFinalLineProvider");

    public static final Set<Type> ANNOTATIONS = Set.of(
            Type.getType(Open.class),
            Type.getType(Coerce.class),
            Type.getType(Extend.class),
            Type.getType(Overrides.class),
            Type.getType(Field.class),
            Type.getType(Field.Final.class),
            Type.getType(Constructor.class),
            Type.getType(OpenSesameGenerated.class),
            UNFINAL,
            MIXIN
    );
    private static final String CTOR_DUMMY = "$$dev$lukebemish$opensesame$$new";
    private static final String UNFINAL_SERVICE = "$$dev$lukebemish$opensesame$$UnFinalService";
    private static final String MIXIN_PACKAGE = "dev/lukebemish/opensesame/mixin/targets";

    private final Set<String> annotationDescriptors;
    private Type type;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java dev.lukebemish.opensesame.compile.asm.VisitingProcessor <input> <output> ...");
            System.exit(1);
        }
        var input = Path.of(args[0]);
        var output = Path.of(args[1]);
        try {
            process(input, output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void process(Path input, Path output) throws IOException {
        if (Files.isRegularFile(input)) {
            processFile(input, output, null);
        } else {
            try (var paths = Files.walk(input)) {
                paths.filter(Files::isRegularFile).forEach(file -> {
                    try {
                        var relative = input.relativize(file);
                        var out = output.resolve(relative);
                        Files.createDirectories(out.getParent());
                        processFile(file, out, output);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    private static void makeMixins(boolean forPublic, boolean forClass, Type holderType, Type targetType, int index, Path rootPath) throws IOException {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        String originalName = holderType.getInternalName();
        String mixinName = MIXIN_PACKAGE + "/" + originalName + "$" + index + "/" + (forPublic ? "public" : "private") + (forClass ? "class" : "interface");
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | (forClass ? 0 : (Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE)), mixinName, null, "java/lang/Object", new String[0]);
        var mixin = writer.visitAnnotation(MIXIN.getDescriptor(), false);
        if (forPublic) {
            var v = mixin.visitArray("value");
            v.visit(null, targetType);
            v.visitEnd();
        } else {
            var v = mixin.visitArray("targets");
            v.visit(null, targetType.getInternalName().replace('/', '.'));
            v.visitEnd();
        }
        mixin.visitEnd();
        var generated = writer.visitAnnotation(OpenSesameGenerated.class.descriptorString(), false);
        generated.visit("value", UNFINAL);
        generated.visitEnd();
        if (forClass) {
            var initWriter = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            initWriter.visitCode();
            initWriter.visitVarInsn(Opcodes.ALOAD, 0);
            initWriter.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            initWriter.visitInsn(Opcodes.RETURN);
            initWriter.visitMaxs(1, 1);
            initWriter.visitEnd();
        }
        writer.visitEnd();
        var mixinPath = rootPath.resolve(mixinName + ".class");
        Files.createDirectories(mixinPath.getParent());
        Files.write(mixinPath, writer.toByteArray());
    }

    private static void processFile(Path file, Path out, @Nullable Path rootPath) throws IOException {
        if (!file.getFileName().toString().endsWith(".class")) {
            Files.copy(file, out);
            return;
        }
        try (var inputStream = Files.newInputStream(file)) {
            ClassReader reader = new ClassReader(inputStream);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            try {
                reader.accept(new VisitingProcessor(writer, VisitingProcessor.ANNOTATIONS) {
                    @Override
                    protected void writeUnFinalLines(List<String> lines, Type selfType) throws IOException {
                        if (rootPath != null) {
                            Set<Type> targets = new HashSet<>();
                            for (String line : lines) {
                                String type = line.split(" ")[0];
                                targets.add(Type.getObjectType(type.replace('.', '/')));
                            }
                            List<Type> orderedTargets = new ArrayList<>(targets);
                            Map<Type, Integer> targetIndexes = new HashMap<>();
                            for (int i = 0; i < orderedTargets.size(); i++) {
                                targetIndexes.put(orderedTargets.get(i), i);
                            }
                            String generatedClassName = selfType.getInternalName() + UNFINAL_SERVICE;
                            Path generatedClassPath = rootPath.resolve(generatedClassName + ".class");
                            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                            writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, generatedClassName, null, "java/lang/Object", new String[]{UNFINAL_LINE_PROVIDER.getInternalName()});
                            var generated = writer.visitAnnotation(OpenSesameGenerated.class.descriptorString(), false);
                            generated.visit("value", UNFINAL);
                            generated.visitEnd();
                            var initWriter = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
                            initWriter.visitCode();
                            initWriter.visitVarInsn(Opcodes.ALOAD, 0);
                            initWriter.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
                            initWriter.visitInsn(Opcodes.RETURN);
                            initWriter.visitMaxs(1, 1);
                            initWriter.visitEnd();
                            var implWriter = writer.visitMethod(Opcodes.ACC_PUBLIC, "lines", "()[Ljava/lang/String;", null, null);
                            implWriter.visitCode();
                            implWriter.visitLdcInsn(lines.size());
                            implWriter.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");
                            for (int i = 0; i < lines.size(); i++) {
                                implWriter.visitInsn(Opcodes.DUP);
                                implWriter.visitLdcInsn(i);
                                var lineEnd = lines.get(i);
                                var targetClass = Type.getObjectType(lineEnd.split(" ")[0].replace('.', '/'));
                                var mixinPackageFull = selfType.getInternalName() + "$" + targetIndexes.get(targetClass);
                                implWriter.visitLdcInsn(mixinPackageFull.replace('/','.') +" "+lineEnd);
                                implWriter.visitInsn(Opcodes.AASTORE);
                            }
                            implWriter.visitInsn(Opcodes.ARETURN);
                            implWriter.visitMaxs(3, 1);
                            implWriter.visitEnd();
                            writer.visitEnd();
                            Files.write(generatedClassPath, writer.toByteArray());
                            var serviceFile = rootPath.resolve("META-INF/services/" + UNFINAL_LINE_PROVIDER.getInternalName().replace('/', '.'));
                            if (!Files.exists(serviceFile)) {
                                Files.createDirectories(serviceFile.getParent());
                                Files.createFile(serviceFile);
                            }
                            Files.write(serviceFile, List.of(generatedClassName.replace('/','.')), StandardOpenOption.APPEND);

                            for (Type target : targets) {
                                int index = targetIndexes.get(target);
                                makeMixins(true, true, selfType, target, index, rootPath);
                                makeMixins(true, false, selfType, target, index, rootPath);
                                makeMixins(false, true, selfType, target, index, rootPath);
                                makeMixins(false, false, selfType, target, index, rootPath);
                            }
                        }
                        super.writeUnFinalLines(lines, selfType);
                    }
                }, 0);
            } catch (RuntimeException e) {
                throw new IOException("Error processing class " + file, e);
            }
            Files.write(out, writer.toByteArray());
        }
    }

    List<String> unFinalLines = new ArrayList<>();
    boolean unFinalShouldRemapConstants = false;

    boolean isExtension = false;
    private boolean isInterface = false;
    ConDynUtils.TypedDynamic<?, Type> extendTargetClassHandle = null;
    Map<String, Annotation> annotations = new HashMap<>();
    Map<String, ExtendFieldInfo<Type>> fields = new HashMap<>();
    List<ExtendOverrideInfo<Type>> overrides = new ArrayList<>();
    List<ExtendCtorInfo> ctors = new ArrayList<>();
    boolean hasClassInit = false;
    boolean unsafeExtension = false;
    final AnnotationDependentVisitor delegate;

    public VisitingProcessor(ClassVisitor delegate, Set<Type> annotations) {
        super(Opcodes.ASM9, new AnnotationDependentVisitor(Opcodes.ASM9, delegate));
        this.delegate = (AnnotationDependentVisitor) this.getDelegate();
        this.annotationDescriptors = annotations.stream().map(Type::getDescriptor).collect(Collectors.toSet());
    }

    protected void writeUnFinalLines(List<String> lines, Type selfType) throws IOException {

    }

    final List<Runnable> extendCallbacks = new ArrayList<>();

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (annotationDescriptors.contains(descriptor)) {
            var type = Type.getType(descriptor);
            var annotation = new Annotation(super.visitAnnotation(descriptor, visible), descriptor);
            if (descriptor.equals(Extend.class.descriptorString())) {
                if (!isInterface) {
                    throw new RuntimeException("@Extend annotation must be only used on an interface");
                }
                isExtension = true;
                annotation.onEnd(() -> {
                    extendTargetClassHandle = typeProviderFromAnnotation(annotation, null, Extend.class);
                    unsafeExtension = unsafe(annotation);
                    var interfaces = delegate.classInfo.interfaces;
                    if (Arrays.stream(interfaces).noneMatch(s -> s.equals(Type.getInternalName(Extension.class)))) {
                        interfaces = Arrays.copyOf(interfaces, interfaces.length + 1);
                        interfaces[interfaces.length - 1] = Type.getInternalName(Extension.class);
                        delegate.classInfo.interfaces = interfaces;
                    }
                    extendCallbacks.forEach(Runnable::run);
                });
            } else if (descriptor.equals(UNFINAL.getDescriptor())) {
                Runnable callback = () -> {
                    var extendType = VisitingProcessor.this.extendTargetClassHandle.type();
                    if (extendType == null) {
                        throw new RuntimeException("Could not determine target class for @UnFinal");
                    }
                    String name = remapClassName(extendType.getInternalName().replace('/', '.'));
                    this.unFinalLines.add(name);
                };
                annotation.onEnd(() -> {
                    if (!isExtension) {
                        extendCallbacks.add(callback);
                    } else {
                        callback.run();
                    }
                });
            } else if (descriptor.equals(OpenSesameGenerated.class.descriptorString())) {
                annotation.onEnd(() -> {
                    if (UNFINAL.equals(annotation.literals.get("value"))) {
                        unFinalShouldRemapConstants = true;
                    }
                    delegate.annotationsAndAttributes.forEach(annotationOrAttribute -> {
                        if (annotationOrAttribute instanceof AnnotationDependentVisitor.AnnotationInfo annotationInfo) {
                            if (annotationInfo.node.desc.equals(MIXIN.getDescriptor())) {
                                var classes = annotations.get(MIXIN.getDescriptor()).arrays.get("value");
                                var targets = annotations.get(MIXIN.getDescriptor()).arrays.get("targets");
                                var remappedClasses = new ArrayList<Type>();
                                var remappedTargets = new ArrayList<String>();
                                if (classes != null) {
                                    for (var classType : classes) {
                                        var oldType = (Type) classType;
                                        var newType = remapType(oldType);
                                        remappedClasses.add(newType);
                                    }
                                }
                                if (targets != null) {
                                    for (var target : targets) {
                                        var oldName = (String) target;
                                        var newName = remapClassName(oldName.replace('.', '/')).replace('/', '.');
                                        remappedTargets.add(newName);
                                    }
                                }
                                AnnotationNode node = new AnnotationNode(MIXIN.getDescriptor());
                                var valueVisitor = node.visitArray("value");
                                for (var remappedClass : remappedClasses) {
                                    valueVisitor.visit(null, remappedClass);
                                }
                                var targetsVisitor = node.visitArray("targets");
                                for (var remappedTarget : remappedTargets) {
                                    targetsVisitor.visit(null, remappedTarget);
                                }
                                node.visitEnd();
                                annotationInfo.node = node;
                            }
                        }
                    });
                });
            }
            if (annotations.put(descriptor, annotation) != null) {
                throw new RuntimeException("Type may have at most one annotation of type " + type.getClassName() + ", but had more than one");
            }
            return annotation;
        }
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        type = Type.getObjectType(name);
        if ((access & Opcodes.ACC_INTERFACE) != 0) {
            isInterface = true;
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    private boolean skip(String name) {
        return name.equals(Processor.EXTEND_INFO_GENERATED) || name.equals(Processor.EXTEND_GENERATED_CLASS);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        if (!ending && isExtension && skip(name)) {
            return new FieldNode(access, name, descriptor, signature, value);
        }

        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (!ending && isExtension && skip(name)) {
            return new MethodNode(access, name, descriptor, signature, exceptions);
        }

        Type returnType = Type.getReturnType(descriptor);
        List<Type> parameterTypes = Arrays.stream(Type.getArgumentTypes(descriptor)).toList();

        return new Method(super.visitMethod(access, name, descriptor, signature, exceptions), parameterTypes, returnType, type, name, access);
    }

    @Override
    public TypeProvider<Type, ?, ?> types() {
        return ASMTypeProvider.INSTANCE;
    }

    @Override
    public ConDynUtils<Type, ?, ?> conDynUtils() {
        return ASMTypeProvider.CON_DYN_UTILS;
    }

    protected String remapClassName(String name) {
        return name;
    }

    protected String remapMethodName(Type className, String methodName, Type returnType, List<Type> parameters) {
        return methodName;
    }

    protected String remapFieldName(Type className, String fieldName, Type fieldType) {
        return fieldName;
    }

    @Override
    public ConDynUtils.TypedDynamic<?, Type> typeProviderFromAnnotation(Annotation annotation, Object context, Class<?> annotationType) {
        ConDynUtils.TypedDynamic<?, Type> targetClassHandle = null;

        String targetName = (String) annotation.literals.get("targetName");
        Type targetClass = (Type) annotation.literals.get("targetClass");
        Type targetProvider = (Type) annotation.literals.get("targetProvider");

        if (targetName == null && targetClass == null && targetProvider == null) {
            throw new RuntimeException(annotationType.getSimpleName()+" annotation must have exactly one of targetName, targetClass, or targetProvider");
        } else if (targetName != null && targetProvider == null) {
            targetClassHandle = conDynUtils().conDynFromName(targetName, this::remapClassName);
        }
        if (targetClass != null) {
            if (targetClassHandle != null) {
                throw new RuntimeException(annotationType.getSimpleName()+" annotation must have exactly one of targetName, targetClass, or targetProvider");
            }

            targetClassHandle = conDynUtils().conDynFromClass(targetClass);
        }
        if (targetProvider != null) {
            if (targetClassHandle != null) {
                throw new RuntimeException(annotationType.getSimpleName()+" annotation must have exactly one of targetName, targetClass, or targetProvider");
            }

            targetClassHandle = conDynUtils().conDynFromFunction(targetProvider, targetName);
        }

        return targetClassHandle;
    }

    @Override
    public @Nullable VisitingProcessor.Annotation annotation(Method method, Class<?> type) {
        return method.annotations.get(type.descriptorString());
    }

    @Override
    public List<MethodParameter<Type, Annotation>> parameters(Method method, @Nullable Class<?> type) {
        List<MethodParameter<Type, Annotation>> parameters = new ArrayList<>(method.parameterTypes.size());
        for (int i = 0; i < method.parameterTypes.size(); i++) {
            var parameterType = method.parameterTypes.get(i);
            Annotation[] parameterAnnotations = null;
            if (type != null) {
                parameterAnnotations = method.parameterAnnotations.get(type.descriptorString());
            }
            parameters.add(new MethodParameter<>(parameterType, parameterAnnotations == null ? null : parameterAnnotations[i]));
        }
        return parameters;
    }

    @Override
    public Open.Type type(Annotation annotation) {
        if (annotation.enums.containsKey("type")) {
            return Open.Type.valueOf(annotation.enums.get("type").value());
        }
        throw new RuntimeException(Open.class.getSimpleName()+" annotation must have a type");
    }

    @Override
    public @Nullable String name(Annotation annotation) {
        var name = (String) annotation.literals.get("name");
        if (name == null) {
            return (String) annotation.literals.get("value");
        }
        return name;
    }

    @Override
    public boolean unsafe(Annotation annotation) {
        if (annotation.literals.containsKey("unsafe")) {
            return (boolean) annotation.literals.get("unsafe");
        }
        return false;
    }

    @Override
    public Type returnType(Method method) {
        return method.returnType;
    }

    @Override
    public boolean isStatic(Method method) {
        return method.isStatic;
    }

    @Override
    public String methodName(Method method) {
        return method.name;
    }

    @Override
    public Type declaringClass(Method method) {
        return method.declaringClass;
    }

    public static final class Annotation extends AnnotationVisitor {
        Map<String, Object> literals = new HashMap<>();
        Map<String, EnumConstant> enums = new HashMap<>();
        Map<String, Annotation> annotations = new HashMap<>();
        Map<String, List<Object>> arrays = new HashMap<>();

        private Runnable onEnd = () -> {};

        final String descriptor;

        public Annotation(AnnotationVisitor delegate, String descriptor) {
            super(Opcodes.ASM9, delegate);
            this.descriptor = descriptor;
        }

        void onEnd(Runnable onEndNew) {
            var old = this.onEnd;
            this.onEnd = () -> {
                old.run();
                onEndNew.run();
            };
        }

        @Override
        public void visit(String name, Object value) {
            literals.put(name, value);
            super.visit(name, value);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            var annotation = new Annotation(super.visitAnnotation(name, descriptor), descriptor);
            annotations.put(name, annotation);
            return annotation;
        }

        @Override
        public void visitEnum(String name, String descriptor, String value) {
            var type = Type.getType(descriptor);
            var enumConstant = new EnumConstant(type, value);
            enums.put(name, enumConstant);
            super.visitEnum(name, descriptor, value);
        }

        private static class ArrayAnnotationVisitor extends AnnotationVisitor {
            final List<Object> addTo;
            List<Object> arrayList;
            protected ArrayAnnotationVisitor(List<Object> addTo, AnnotationVisitor annotationVisitor) {
                super(Opcodes.ASM9, annotationVisitor);
                this.addTo = addTo;
            }

            @Override
            public void visit(String name, Object value) {
                addTo.add(value);
                super.visit(name, value);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                var annotation = new Annotation(super.visitAnnotation(name, descriptor), descriptor);
                addTo.add(annotation);
                return annotation;
            }

            @Override
            public void visitEnum(String name, String descriptor, String value) {
                var type = Type.getType(descriptor);
                var enumConstant = new EnumConstant(type, value);
                addTo.add(enumConstant);
                super.visitEnum(name, descriptor, value);
            }

            @Override
            public AnnotationVisitor visitArray(String name) {
                if (arrayList == null) {
                    arrayList = new ArrayList<>();
                    addTo.add(arrayList);
                }
                return new ArrayAnnotationVisitor(arrayList, super.visitArray(name));
            }
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            return new ArrayAnnotationVisitor(arrays.computeIfAbsent(name, k -> new ArrayList<>()), super.visitArray(name));
        }

        @Override
        public void visitEnd() {
            onEnd.run();
            super.visitEnd();
        }
    }

    public record EnumConstant(Type type, String value) {}

    boolean ending = false;

    @Override
    public void visitEnd() {
        if (isExtension) {
            ending = true;
            this.extensionBytecode(
                    new ASMClassAccumulator(this),
                    ctors,
                    extendTargetClassHandle,
                    fields,
                    !hasClassInit,
                    overrides,
                    type,
                    this::remapMethodName
            );
        }
        if (!unFinalLines.isEmpty()) {
            try {
                writeUnFinalLines(unFinalLines, type);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        super.visitEnd();
    }

    public final class Method extends MethodVisitor {
        private final List<Type> parameterTypes;
        private final Type returnType;
        private final Map<String, Annotation[]> parameterAnnotations = new HashMap<>();
        private final Map<String, Annotation> annotations = new HashMap<>();
        private final Type declaringClass;
        private final String name;
        private final boolean isStatic;
        private final boolean isAbstract;

        public Method(MethodVisitor delegate, List<Type> parameterTypes, Type returnType, Type declaringClass, String name, int access) {
            super(Opcodes.ASM9, delegate);
            this.parameterTypes = parameterTypes;
            this.returnType = returnType;
            this.declaringClass = declaringClass;
            this.name = name;
            this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
            this.isAbstract = (access & Opcodes.ACC_ABSTRACT) != 0;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (annotationDescriptors.contains(descriptor)) {
                var type = Type.getType(descriptor);
                var annotation = new Annotation(super.visitAnnotation(descriptor, visible), descriptor);
                if (annotations.put(descriptor, annotation) != null) {
                    throw new RuntimeException("Method may have at most one annotation of type " + type.getClassName() + ", but had more than one");
                }
                return annotation;
            }
            return super.visitAnnotation(descriptor, visible);
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
            if (annotationDescriptors.contains(descriptor)) {
                var type = Type.getType(descriptor);
                var annotation = new Annotation(super.visitParameterAnnotation(parameter, descriptor, visible), descriptor);
                var annotations = parameterAnnotations.computeIfAbsent(descriptor, k -> new Annotation[parameterTypes.size()]);
                if (annotations[parameter] != null) {
                    throw new RuntimeException("Method parameter " + parameter + " may have at most one annotation of type " + type.getClassName() + ", but had more than one");
                }
                annotations[parameter] = annotation;
                return annotation;
            }
            return super.visitParameterAnnotation(parameter, descriptor, visible);
        }

        @Override
        public void visitEnd() {
            if (this.annotations.containsKey(Open.class.descriptorString())) {
                if (this.isAbstract) {
                    throw new RuntimeException("Method "+this.name+" is abstract, but "+Open.class.getSimpleName()+" expects a concrete method");
                }
            } else if (this.annotations.containsKey(Constructor.class.descriptorString())) {
                if (this.isAbstract) {
                    throw new RuntimeException("Method "+this.name+" is abstract, but "+Constructor.class.getSimpleName()+" expects a concrete method");
                }
            } else if (this.annotations.containsKey(Overrides.class.descriptorString())) {
                if (this.isAbstract) {
                    throw new RuntimeException("Method "+this.name+" is abstract, but "+Overrides.class.getSimpleName()+" expects a concrete method");
                }
            } else if (this.annotations.containsKey(Field.class.descriptorString())) {
                handleField();
            }
            super.visitEnd();
        }

        @Override
        public void visitLdcInsn(Object value) {
            if (unFinalShouldRemapConstants && value instanceof String line) {
                var parts = line.split(" ");
                var packageName = parts[0];
                if (parts.length == 2) {
                    var className = parts[1];
                    line = packageName + " " + remapClassName(className.replace('.', '/')).replace('/', '.');
                } else if (parts.length == 4) {
                    var className = parts[1];
                    var name = parts[2];
                    var desc = parts[3];
                    var type = Type.getType(desc);
                    var remapClassName = remapClassName(className.replace('.', '/')).replace('/', '.');
                    if (type.getSort() == Type.METHOD) {
                        line = packageName + " " + remapClassName + " " + remapMethodName(
                                Type.getObjectType(className.replace('.', '/')),
                                name,
                                type.getReturnType(),
                                Arrays.stream(type.getArgumentTypes()).toList()
                        ) + " " + remapType(type).getDescriptor();
                    } else if (type.getSort() == Type.OBJECT) {
                        line = packageName + " " + remapClassName + " " + remapFieldName(
                                Type.getObjectType(className.replace('.', '/')),
                                name,
                                type
                        ) + " " + remapType(type).getDescriptor();
                    }
                }
                super.visitLdcInsn(line);
                return;
            }
            super.visitLdcInsn(value);
        }

        @Override
        public void visitCode() {
            super.visitCode();

            if (isExtension && "<clinit>".equals(this.name)) {
                hasClassInit = true;
                extensionClassInitSetup(new ASMClassAccumulator.ASMMethodMaker(this), type);
            }

            if (this.annotations.containsKey(Open.class.descriptorString())) {
                handleOpen();
            } else if (this.annotations.containsKey(Constructor.class.descriptorString())) {
                handleConstructor();
            } else if (this.annotations.containsKey(Overrides.class.descriptorString())) {
                handleOverrides();
            }
        }

        private void handleField() {
            if (isExtension) {
                if (this.parameterTypes.size() > 1 || this.isStatic) {
                    throw new RuntimeException("@Field getter/setter must have at most one parameter and must not be static");
                }
                var setter = this.parameterTypes.size() == 1;
                var fieldName = name(this.annotations.get(Field.class.descriptorString()));
                var setAsFinal = this.annotations.containsKey(Field.Final.class.descriptorString());
                if (setter) {
                    if (this.returnType.getSort() != Type.VOID) {
                        throw new RuntimeException("@Field setter must have void return type");
                    }
                    Type fieldType = this.parameterTypes.get(0);
                    ExtendFieldInfo<Type> fieldInfo = fields.computeIfAbsent(fieldName, k -> new ExtendFieldInfo<>(fieldName, fieldType, setAsFinal));
                    if (!fieldInfo.isFinal() && setAsFinal) {
                        fieldInfo = new ExtendFieldInfo<>(fieldName, fieldType, true, fieldInfo.getters(), fieldInfo.setters());
                        fields.put(fieldName, fieldInfo);
                    }
                    fieldInfo.setters().add(this.name);
                } else {
                    if (this.returnType.getSort() == Type.VOID) {
                        throw new RuntimeException("@Field getter must not have void return type");
                    }
                    Type fieldType = this.returnType;
                    ExtendFieldInfo<Type> fieldInfo = fields.computeIfAbsent(fieldName, k -> new ExtendFieldInfo<>(fieldName, fieldType, setAsFinal));
                    if (!fieldInfo.isFinal() && setAsFinal) {
                        fieldInfo = new ExtendFieldInfo<>(fieldName, fieldType, true, fieldInfo.getters(), fieldInfo.setters());
                        fields.put(fieldName, fieldInfo);
                    }
                    fieldInfo.getters().add(this.name);
                }
            } else {
                throw new RuntimeException("@Field annotation must be used on an interface marked with Extend");
            }
        }

        private void handleOverrides() {
            if (isExtension) {
                if (this.isStatic) {
                    throw new RuntimeException("@Overrides must not be static");
                }
                CoercedDescriptor<Type> descriptor = coercedDescriptor(this);
                String originalName = name(this.annotations.get(Overrides.class.descriptorString()));
                if (this.name.equals(originalName)) {
                    throw new RuntimeException("@Overrides must not have the same name as the original method");
                }
                overrides.add(new ExtendOverrideInfo<>(
                        name,
                        conDynUtils().conDynFromClass(returnType),
                        parameterTypes.stream().<ConDynUtils.TypedDynamic<?, Type>>map(conDynUtils()::conDynFromClass).toList(),
                        originalName,
                        descriptor.returnType(),
                        descriptor.parameterTypes()
                ));
                if (this.annotations.containsKey(UNFINAL.getDescriptor())) {
                    if (!(extendTargetClassHandle.type() == null || descriptor.returnType().type() == null || descriptor.parameterTypes().stream().map(ConDynUtils.TypedDynamic::type).anyMatch(Objects::isNull))) {
                        String line = remapClassName(extendTargetClassHandle.type().getInternalName()).replace('/', '.') + " " +
                                remapMethodName(
                                        extendTargetClassHandle.type(),
                                        originalName,
                                        descriptor.returnType().type(),
                                        descriptor.parameterTypes().stream().map(ConDynUtils.TypedDynamic::type).toList()
                                ) + " " +
                                remapType(Type.getMethodType(descriptor.returnType().type(), descriptor.parameterTypes().stream().map(ConDynUtils.TypedDynamic::type).toArray(Type[]::new))).getDescriptor();
                        unFinalLines.add(line);
                    }
                }
            } else {
                throw new RuntimeException("@Overrides annotation must be used on an interface marked with Extend");
            }
        }

        private void handleConstructor() {
            if (isExtension) {
                if (!this.isStatic) {
                    throw new RuntimeException("@Constructor must be static");
                }
                CoercedDescriptor<Type> descriptor = coercedDescriptor(this);
                if (!type.equals(descriptor.returnType().type())) {
                    throw new RuntimeException("@Constructor must have return type of " + type.getClassName());
                }
                var fields = parameterAnnotations.get(Field.class.descriptorString());
                var fieldsFinal = parameterAnnotations.get(Field.Final.class.descriptorString());
                int drop = 0;
                List<String> fieldNames = new ArrayList<>();
                if (fields != null) {
                    boolean finished = false;
                    for (int i = 0; i < fields.length; i++) {
                        var field = fields[i];
                        if (field == null) {
                            finished = true;
                            continue;
                        }
                        if (finished) {
                            throw new RuntimeException("@Constructor must have all field parameters before non-field parameters");
                        }
                        if (fieldNames.contains(name(field))) {
                            throw new RuntimeException("@Constructor must not have duplicate field parameters");
                        }
                        drop++;
                        Type fieldType = this.parameterTypes.get(i);
                        var name = name(field);
                        var setAsFinal = fieldsFinal != null && fieldsFinal[i] != null;
                        ExtendFieldInfo<Type> fieldInfo = VisitingProcessor.this.fields.computeIfAbsent(name, k -> new ExtendFieldInfo<>(name, fieldType, setAsFinal));
                        if (!fieldInfo.isFinal() && setAsFinal) {
                            fieldInfo = new ExtendFieldInfo<>(name, fieldType, true, fieldInfo.getters(), fieldInfo.setters());
                            VisitingProcessor.this.fields.put(name, fieldInfo);
                        }
                        if (!fieldInfo.type().equals(fieldType)) {
                            throw new RuntimeException("@Constructor field parameter type must match field type");
                        }
                        fieldNames.add(name);
                    }
                }
                List<ConDynUtils.TypedDynamic<?, Type>> superCtorTypes = new ArrayList<>(descriptor.parameterTypes());
                if (drop > 0) {
                    superCtorTypes.subList(0, drop).clear();
                }
                var voidType = conDynUtils().conDynFromClass(Type.getType(void.class));

                Object superCtorType = conDynUtils().conDynMethodType(voidType.constantDynamic(), superCtorTypes.stream().<Object>map(ConDynUtils.TypedDynamic::constantDynamic).toList());
                Object ctorType = conDynUtils().conDynMethodType(voidType.constantDynamic(), this.parameterTypes.stream().<Object>map(t -> conDynUtils().conDynFromClass(t).constantDynamic()).toList());

                ctors.add(new ExtendCtorInfo(ctorType, superCtorType, fieldNames));

                int j = 0;
                for (Type parameterType : this.parameterTypes) {
                    super.visitVarInsn(parameterType.getOpcode(Opcodes.ILOAD), j);
                    j += parameterType.getSize();
                }

                super.visitInvokeDynamicInsn(
                        name,
                        Type.getMethodDescriptor(returnType, this.parameterTypes.toArray(Type[]::new)),
                        new Handle(
                                Opcodes.H_INVOKESTATIC,
                                Type.getInternalName(OpeningMetafactory.class),
                                unsafeExtension ? "makeOpenClassUnsafe" : "makeOpenClass",
                                MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, MethodHandle.class, MethodHandle.class, MethodHandle.class, MethodHandle.class).toMethodDescriptorString(),
                                false
                        ),
                        extendTargetClassHandle.constantDynamic(),
                        new Handle(
                                Opcodes.H_INVOKESTATIC,
                                type.getInternalName(),
                                Processor.EXTEND_GENERATED_CLASS,
                                MethodType.methodType(void.class, Class.class).toMethodDescriptorString(),
                                true
                        ),
                        new Handle(
                                Opcodes.H_INVOKESTATIC,
                                type.getInternalName(),
                                Processor.EXTEND_GENERATED_CLASS,
                                MethodType.methodType(Class.class).toMethodDescriptorString(),
                                true
                        ),
                        new Handle(
                                Opcodes.H_INVOKESTATIC,
                                type.getInternalName(),
                                Processor.EXTEND_INFO_GENERATED,
                                MethodType.methodType(List.class, ClassLoader.class).toMethodDescriptorString(),
                                true
                        )
                );

                super.visitInsn(Opcodes.ARETURN);

                int parametersSize = 0;
                for (Type parameterType : this.parameterTypes) {
                    parametersSize += parameterType.getSize();
                }
                super.visitMaxs(Math.max(2, parametersSize), parametersSize);
                super.visitEnd();

                this.mv = null;
            } else {
                throw new RuntimeException("@Constructor annotation must be used on an interface marked with Extend");
            }
        }

        private void handleOpen() {
            Opening<Type> opening = opening(this);

            if (!isStatic) {
                super.visitVarInsn(Opcodes.ALOAD, 0);
            }

            int j = 0;
            for (Type parameterType : this.parameterTypes) {
                super.visitVarInsn(parameterType.getOpcode(Opcodes.ILOAD), isStatic ? j : j + 1);
                j += parameterType.getSize();
            }

            var methodType = opening.type().ordinal();

            String remappedName;
            if (opening.targetType() == null || opening.returnType() == null || opening.parameterTypes().stream().anyMatch(Objects::isNull)) {
                remappedName = opening.name();
            } else {
                remappedName = switch (opening.type()) {
                    case STATIC -> remapMethodName(
                            opening.targetType(),
                            opening.name(),
                            opening.returnType(),
                            opening.parameterTypes()
                    );
                    case VIRTUAL, SPECIAL -> {
                        List<Type> parameterTypes = new ArrayList<>(opening.parameterTypes());
                        parameterTypes.remove(0);
                        yield remapMethodName(
                                opening.targetType(),
                                opening.name(),
                                opening.returnType(),
                                parameterTypes
                        );
                    }
                    case GET_STATIC, GET_INSTANCE -> remapFieldName(
                            opening.targetType(),
                            opening.name(),
                            opening.returnType()
                    );
                    case SET_STATIC, SET_INSTANCE -> remapFieldName(
                            opening.targetType(),
                            opening.name(),
                            opening.parameterTypes().get(opening.parameterTypes().size()-1)
                    );
                    case CONSTRUCT -> CTOR_DUMMY;
                    case ARRAY -> opening.name();
                };
            }

            super.visitInvokeDynamicInsn(
                    remappedName,
                    opening.factoryType().getDescriptor(),
                    new Handle(
                            Opcodes.H_INVOKESTATIC,
                            Type.getInternalName(OpeningMetafactory.class),
                            opening.unsafe() ? "invokeUnsafe" : "invoke",
                            MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, MethodHandle.class, MethodHandle.class, int.class).toMethodDescriptorString(),
                            false
                    ),
                    opening.targetProvider(),
                    opening.methodTypeProvider(),
                    methodType
            );

            if (opening.factoryType().getReturnType().getSort() != Type.VOID) {
                super.visitInsn(opening.factoryType().getReturnType().getOpcode(Opcodes.IRETURN));
            } else {
                super.visitInsn(Opcodes.RETURN);
            }

            int parametersSize = 0;
            for (Type parameterType : this.parameterTypes) {
                parametersSize += parameterType.getSize();
            }
            super.visitMaxs(Math.max(2, parametersSize + (isStatic ? 0 : 1)), parametersSize + (isStatic ? 0 : 1));
            super.visitEnd();

            if (annotations.get(UNFINAL.getDescriptor()) != null) {
                String line;
                if (opening.targetType() == null || opening.returnType() == null || opening.parameterTypes().stream().anyMatch(Objects::isNull)) {
                    line = null;
                } else {
                    line = switch (opening.type()) {
                        case STATIC -> remappedName + " " + remapType(Type.getMethodType(opening.returnType(), opening.parameterTypes().toArray(Type[]::new))).getDescriptor();
                        case VIRTUAL, SPECIAL -> {
                            List<Type> parameterTypes = new ArrayList<>(opening.parameterTypes());
                            parameterTypes.remove(0);
                            yield remappedName + " " + remapType(Type.getMethodType(opening.returnType(), parameterTypes.toArray(Type[]::new))).getDescriptor();
                        }
                        case GET_STATIC, GET_INSTANCE -> remappedName + " " + remapType(opening.returnType()).getDescriptor();
                        case SET_STATIC, SET_INSTANCE -> remappedName + " " + remapType(opening.parameterTypes().get(opening.parameterTypes().size()-1)).getDescriptor();
                        default -> null;
                    };
                }
                if (line != null) {
                    unFinalLines.add(remapClassName(opening.targetType().getInternalName()).replace('/','.') + " " + line);
                }
            }

            this.mv = null;
        }
    }

    private Type remapType(Type type) {
        if (type.getSort() == Type.METHOD) {
            return Type.getMethodType(
                    remapType(type.getReturnType()),
                    Arrays.stream(type.getArgumentTypes()).map(this::remapType).toArray(Type[]::new)
            );
        } else if (type.getSort() == Type.ARRAY) {
            return Type.getType('['+remapType(type.getElementType()).getDescriptor());
        } else if (type.getSort() == Type.OBJECT) {
            return Type.getObjectType(remapClassName(type.getInternalName()));
        } else {
            return type;
        }
    }
}
