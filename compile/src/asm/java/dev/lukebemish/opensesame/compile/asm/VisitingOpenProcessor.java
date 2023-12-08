package dev.lukebemish.opensesame.compile.asm;

import dev.lukebemish.opensesame.annotations.Coerce;
import dev.lukebemish.opensesame.annotations.Open;
import dev.lukebemish.opensesame.compile.ConDynUtils;
import dev.lukebemish.opensesame.compile.OpenProcessor;
import dev.lukebemish.opensesame.compile.TypeProvider;
import dev.lukebemish.opensesame.runtime.OpeningMetafactory;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.io.IOException;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class VisitingOpenProcessor extends ClassVisitor implements OpenProcessor<Type, VisitingOpenProcessor.Annotation, VisitingOpenProcessor.Method> {
    public static final Set<Type> ANNOTATIONS = Set.of(Type.getType(Open.class), Type.getType(Coerce.class));
    private static final String CTOR_DUMMY = "$$dev$lukebemish$opensesame$$new";

    private final Set<String> annotationDescriptors;
    private Type type;

    public static void main(String[] args) {
        if ((~args.length & 1) != 1) {
            System.err.println("Usage: java dev.lukebemish.opensesame.compile.asm.Processor <input> <output> <input> <output> ...");
            System.exit(1);
        }
        for (int i = 0; i < args.length; i += 2) {
            var input = Path.of(args[0]);
            var output = Path.of(args[1]);
            try {
                process(input, output);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void process(Path input, Path output) throws IOException {
        if (Files.isRegularFile(input)) {
            processFile(input, output);
        } else {
            try (var paths = Files.walk(input)) {
                paths.filter(Files::isRegularFile).forEach(file -> {
                    try {
                        var relative = input.relativize(file);
                        var out = output.resolve(relative);
                        Files.createDirectories(out.getParent());
                        processFile(file, out);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    private static void processFile(Path file, Path out) throws IOException {
        if (!file.getFileName().toString().endsWith(".class")) {
            Files.copy(file, out);
            return;
        }
        try (var inputStream = Files.newInputStream(file)) {
            ClassReader reader = new ClassReader(inputStream);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            reader.accept(new VisitingOpenProcessor(writer, VisitingOpenProcessor.ANNOTATIONS), 0);
            Files.write(out, writer.toByteArray());
        }
    }

    public VisitingOpenProcessor(ClassVisitor delegate, Set<Type> annotations) {
        super(Opcodes.ASM9, delegate);
        this.annotationDescriptors = annotations.stream().map(Type::getDescriptor).collect(Collectors.toSet());
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        type = Type.getObjectType(name);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        Type returnType = Type.getReturnType(descriptor);
        List<Type> parameterTypes = Arrays.stream(Type.getArgumentTypes(descriptor)).toList();

        boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
        Method method = new Method(super.visitMethod(access, name, descriptor, signature, exceptions), parameterTypes, returnType, type, name, isStatic);

        return new MethodVisitor(Opcodes.ASM9, method) {
            boolean inCode;

            @Override
            public MethodVisitor getDelegate() {
                if (inCode) return null;
                return super.getDelegate();
            }

            @Override
            public void visitCode() {
                if (!method.annotations.containsKey(Open.class.descriptorString())) {
                    super.visitCode();
                    return;
                }

                inCode = true;

                super.visitCode();

                Opening<Type> opening = opening(method);

                if (!isStatic) {
                    super.visitVarInsn(Opcodes.ALOAD, 0);
                }

                for (int i = 0; i < method.parameterTypes.size(); i++) {
                    Type parameterType = method.parameterTypes.get(i);
                    super.visitVarInsn(parameterType.getOpcode(Opcodes.ILOAD), isStatic ? i : i + 1);
                }

                var methodType = opening.type().ordinal();

                super.visitInvokeDynamicInsn(
                        opening.type() == Open.Type.CONSTRUCT ? CTOR_DUMMY : opening.name(),
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

                super.visitMaxs(method.parameterTypes.size() + (isStatic ? 0 : 1), method.parameterTypes.size() + (isStatic ? 0 : 1));
                super.visitEnd();
            }
        };
    }

    @Override
    public TypeProvider<Type, ?, ?> types() {
        return ASMTypeProvider.INSTANCE;
    }

    @Override
    public ConDynUtils<Type, ?, ?> conDynUtils() {
        return ASMTypeProvider.CON_DYN_UTILS;
    }

    @Override
    public Object typeProviderFromAnnotation(Annotation annotation, Method method, Class<?> annotationType) {
        Object targetClassHandle = null;

        String targetName = (String) annotation.literals.get("targetName");
        Type targetClass = (Type) annotation.literals.get("targetClass");
        Type targetFunction = (Type) annotation.literals.get("targetProvider");

        if (targetName == null && targetClass == null && targetFunction == null) {
            throw new RuntimeException(annotationType.getSimpleName()+" annotation must have exactly one of targetName, targetClass, or targetProvider");
        } else if (targetName != null && targetFunction == null) {
            targetClassHandle = conDynUtils().conDynFromName(targetName);
        }
        if (targetClass != null) {
            if (targetClassHandle != null) {
                throw new RuntimeException(annotationType.getSimpleName()+" annotation must have exactly one of targetName, targetClass, or targetProvider");
            }

            targetClassHandle = conDynUtils().conDynFromClass(targetClass);
        }
        if (targetFunction != null) {
            if (targetClassHandle != null) {
                throw new RuntimeException(annotationType.getSimpleName()+" annotation must have exactly one of targetName, targetClass, or targetProvider");
            }

            targetClassHandle = conDynUtils().conDynFromFunction(targetFunction, targetName);
        }

        return targetClassHandle;
    }

    @Override
    public @Nullable VisitingOpenProcessor.Annotation annotation(Method method, Class<?> type) {
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
        return (String) annotation.literals.get("name");
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

        public Annotation(AnnotationVisitor delegate) {
            super(Opcodes.ASM9, delegate);
        }

        @Override
        public void visit(String name, Object value) {
            literals.put(name, value);
            super.visit(name, value);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            var annotation = new Annotation(super.visitAnnotation(name, descriptor));
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

        @Override
        public AnnotationVisitor visitArray(String name) {
            // currently we ignore arrays as they're not used in any of our stuff - we'll change it if we have to
            return super.visitArray(name);
        }
    }

    public record EnumConstant(Type type, String value) {}

    public final class Method extends MethodVisitor {
        private final List<Type> parameterTypes;
        private final Type returnType;
        private final Map<String, Annotation[]> parameterAnnotations = new HashMap<>();
        private final Map<String, Annotation> annotations = new HashMap<>();
        private final Type declaringClass;
        private final String name;
        private final boolean isStatic;

        public Method(MethodVisitor delegate, List<Type> parameterTypes, Type returnType, Type declaringClass, String name, boolean isStatic) {
            super(Opcodes.ASM9, delegate);
            this.parameterTypes = parameterTypes;
            this.returnType = returnType;
            this.declaringClass = declaringClass;
            this.name = name;
            this.isStatic = isStatic;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (annotationDescriptors.contains(descriptor)) {
                var type = Type.getType(descriptor);
                var annotation = new Annotation(super.visitAnnotation(descriptor, visible));
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
                var annotation = new Annotation(super.visitParameterAnnotation(parameter, descriptor, visible));
                var annotations = parameterAnnotations.computeIfAbsent(descriptor, k -> new Annotation[parameterTypes.size()]);
                if (annotations[parameter] != null) {
                    throw new RuntimeException("Method parameter " + parameter + " may have at most one annotation of type " + type.getClassName() + ", but had more than one");
                }
                annotations[parameter] = annotation;
                return annotation;
            }
            return super.visitParameterAnnotation(parameter, descriptor, visible);
        }
    }
}
