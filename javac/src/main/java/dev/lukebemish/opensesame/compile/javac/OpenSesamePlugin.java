package dev.lukebemish.opensesame.compile.javac;

import com.google.auto.service.AutoService;
import com.sun.source.tree.*;
import com.sun.source.util.*;
import dev.lukebemish.opensesame.annotations.Open;
import dev.lukebemish.opensesame.compile.Processor;
import dev.lukebemish.opensesame.compile.OpenSesameGenerated;
import dev.lukebemish.opensesame.runtime.OpeningMetafactory;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

@AutoService(Plugin.class)
public class OpenSesamePlugin implements Plugin {
    public static final String PLUGIN_NAME = "OpenSesame";
    private static final String GENERATED_SUFFIX = "$$dev$lukebemish$opensesame$bridge$Open";

    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    @Override
    public void init(JavacTask task, String... args) {
        task.addTaskListener(new TaskListener() {
            @Override
            public void started(TaskEvent e) {
                if (e.getKind() != TaskEvent.Kind.GENERATE) {
                    return;
                }

                Elements elements = task.getElements();
                Types types = task.getTypes();
                ProcessingEnvironment processingEnv;
                Object symtab;
                Object treemaker;
                Object names;
                try {
                    processingEnv = (ProcessingEnvironment) Utils.PROCESSING_ENV_FROM_TASK.invoke(task);
                    symtab = Utils.SYMTAB_FROM_TASK.invoke(task);
                    treemaker = Utils.TREEMAKER_FROM_TASK.invoke(task);
                    names = Utils.NAMES_FROM_TASK.invoke(task);
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }

                var element = e.getTypeElement();
                if (element == null) {
                    return;
                }
                String targetBinName = elements.getBinaryName(element).toString().replace('.', '/');
                e.getCompilationUnit().accept(new TreeScanner<Void, Context>() {

                    @Override
                    public Void visitClass(ClassTree node, Context unused) {
                        Context inner = new Context();
                        inner.enclosingClass = node;
                        inner.processor = new JavacProcessor(node, elements);
                        String binName = inner.processor.declaringClassType.getInternalName();
                        if (!binName.equals(targetBinName)) {
                            return super.visitClass(node, new Context());
                        }
                        var out = super.visitClass(node, inner);
                        if (inner.writer != null) {
                            inner.writer.visitEnd();
                            try {
                                var file = processingEnv.getFiler().createClassFile(
                                        inner.processor.declaringClassType.getClassName() + GENERATED_SUFFIX
                                );
                                try (var os = file.openOutputStream()) {
                                    os.write(inner.writer.toByteArray());
                                }
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                            inner.writer = null;
                        }
                        inner.processor = null;
                        inner.enclosingClass = null;
                        return out;
                    }

                    void fillMethod(MethodTree method, Context inner) {
                        Processor.Opening<Type> opening = inner.processor.opening(method);

                        var name = method.getName().toString() +
                                opening.factoryType().getDescriptor().replace('/','$').replace(";","$$").replace("[","$_Array$").replace("(", "$_Args$").replace(")", "$$");

                        setupClassWriter(inner);
                        var methodWriter = inner.writer.visitMethod(
                                Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                                name,
                                MethodType.methodType(
                                        CallSite.class,
                                        MethodHandles.Lookup.class,
                                        String.class,
                                        MethodType.class
                                ).descriptorString(),
                                null,
                                null
                        );

                        var annotationVisitor = methodWriter.visitAnnotation(OpenSesameGenerated.class.descriptorString(), false);
                        annotationVisitor.visit("value", Type.getType(Open.class));
                        annotationVisitor.visitEnd();

                        methodWriter.visitCode();

                        methodWriter.visitVarInsn(Opcodes.ALOAD, 0);
                        methodWriter.visitLdcInsn(opening.name());
                        methodWriter.visitLdcInsn(opening.factoryType());

                        methodWriter.visitLdcInsn(opening.targetProvider());
                        methodWriter.visitLdcInsn(opening.methodTypeProvider());
                        methodWriter.visitIntInsn(Opcodes.BIPUSH, opening.type().ordinal());

                        methodWriter.visitMethodInsn(
                                Opcodes.INVOKESTATIC,
                                Type.getInternalName(OpeningMetafactory.class),
                                opening.unsafe() ? "invokeUnsafe" : "invoke",
                                MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, MethodHandle.class, MethodHandle.class, int.class).descriptorString(),
                                false
                        );

                        methodWriter.visitInsn(Opcodes.ARETURN);
                        methodWriter.visitMaxs(0, 0);
                        methodWriter.visitEnd();

                        fillBody(
                                method,
                                name,
                                inner.processor.declaringClassType.getClassName() + GENERATED_SUFFIX,
                                inner.enclosingClass,
                                elements,
                                types,
                                treemaker,
                                symtab,
                                names
                        );
                    }

                    void setupClassWriter(Context inner) {
                        if (inner.writer == null) {
                            inner.writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                            inner.writer.visit(
                                    Opcodes.V17,
                                    Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC,
                                    inner.processor.declaringClassType.getInternalName()+GENERATED_SUFFIX,
                                    null,
                                    Type.getInternalName(Object.class),
                                    new String[0]
                            );

                            var annotationVisitor = inner.writer.visitAnnotation(OpenSesameGenerated.class.descriptorString(), false);
                            annotationVisitor.visit("value", Type.getType(Open.class));
                            annotationVisitor.visitEnd();

                            var init = inner.writer.visitMethod(
                                    Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC,
                                    "<init>",
                                    "()V",
                                    null,
                                    null
                            );

                            init.visitCode();
                            init.visitVarInsn(Opcodes.ALOAD, 0);
                            init.visitMethodInsn(
                                    Opcodes.INVOKESPECIAL,
                                    Type.getInternalName(Object.class),
                                    "<init>",
                                    "()V",
                                    false
                            );
                            init.visitInsn(Opcodes.RETURN);
                            init.visitMaxs(0, 0);
                            init.visitEnd();
                        }
                    }

                    @Override
                    public Void visitMethod(MethodTree node, Context inner) {
                        if (inner.processor == null) {
                            return super.visitMethod(node, inner);
                        }
                        boolean[] hasOpen = new boolean[1];
                        node.getModifiers().getAnnotations().forEach(a -> {
                            try {
                                AnnotationMirror annotation = (AnnotationMirror) Utils.JC_ANNOTATION_GET_ATTRIBUTE.invoke(a);
                                var binaryName = elements.getBinaryName((TypeElement) annotation.getAnnotationType().asElement());
                                if (binaryName.contentEquals(Open.class.getName())) {
                                    hasOpen[0] = true;
                                }
                            } catch (Throwable ex) {
                                throw new RuntimeException(ex);
                            }
                        });
                        if (hasOpen[0]) {
                            fillMethod(node, inner);
                        }
                        return super.visitMethod(node, inner);
                    }
                }, new Context());
            }
        });
    }

    private static final class Context {
        JavacProcessor processor = null;
        ClassWriter writer = null;
        ClassTree enclosingClass = null;
    }

    private static void fillBody(MethodTree method, String methodName, String outClass, ClassTree enclosingClass, Elements elements, Types types, Object tm, Object symtab, Object names) {
        var lookupType = elements.getTypeElement(MethodHandles.Lookup.class.getCanonicalName()).asType();
        var stringType = elements.getTypeElement(String.class.getCanonicalName()).asType();
        var methodTypeType = elements.getTypeElement(MethodType.class.getCanonicalName()).asType();
        var callSiteType = elements.getTypeElement(CallSite.class.getCanonicalName()).asType();
        List<TypeMirror> bsmStaticArgsTypes = List.of(
                lookupType,
                stringType,
                methodTypeType
        );

        try {
            TypeElement enclosingClassTypeSymbol = (TypeElement) Utils.JC_CLASS_GET_SYMBOL.invoke(enclosingClass);

            var returnType = typeFromTree(method.getReturnType(), types);
            var parameterTypes = new ArrayList<TypeMirror>();
            var parameters = new ArrayList<>();
            if (!method.getModifiers().getFlags().contains(Modifier.STATIC)) {
                parameterTypes.add(typeFromTree(enclosingClass, types));
                parameters.add(Utils.TM_IDENT_VAR.invoke(tm, method.getReceiverParameter()));
            }
            for (var param : method.getParameters()) {
                parameterTypes.add(typeFromTree(param.getType(), types));
                parameters.add(Utils.TM_IDENT_VAR.invoke(tm, param));
            }
            var methodType = Utils.METHOD_TYPE.invoke(
                    Utils.LIST_FROM.invoke(parameterTypes),
                    returnType,
                    Utils.LIST_NIL.invoke(),
                    enclosingClassTypeSymbol
            );
            Element enclosing = enclosingClassTypeSymbol;
            while (!(enclosing instanceof PackageElement)) {
                enclosing = enclosing.getEnclosingElement();
            }
            var outClassName = Utils.FROM_STRING_NAMES.invoke(names, outClass);
            var outClassNameOnlyLast = Utils.FROM_STRING_NAMES.invoke(names, outClass.substring(outClass.lastIndexOf('.')+ 1));
            var outClassTypeSymbol = Utils.CLASS_SYMBOL.invoke(
                    (long) Opcodes.ACC_STATIC,
                    outClassNameOnlyLast,
                    enclosing
            );
            var targetType = Utils.METHOD_TYPE.invoke(
                    Utils.LIST_FROM.invoke(bsmStaticArgsTypes),
                    callSiteType,
                    Utils.LIST_NIL.invoke(),
                    outClassTypeSymbol
            );
            var targetBsmName = Utils.FROM_STRING_NAMES.invoke(names, methodName);
            var methodSymbol = Utils.METHOD_SYM.invoke(
                    (long) Opcodes.ACC_STATIC,
                    targetBsmName,
                    targetType,
                    outClassTypeSymbol
            );
            var dynSym = Utils.DYN_METHOD_SYM.invoke(
                    Utils.FROM_STRING_NAMES.invoke(names, "bridge"),
                    Utils.SYMTAB_NO_SYMBOL.invoke(symtab),
                    Utils.AS_HANDLE.invoke(methodSymbol),
                    methodType,
                    Utils.LOADABLE_CONSTANT_ARRAY.invoke(0)
            );

            var outClassExpression = Utils.TM_IDENT.invoke(tm, outClassName);
            Utils.EXPR_SET_TYPE.invoke(outClassExpression, Utils.SYMBOL_AS_TYPE.invoke(outClassTypeSymbol));
            Utils.JC_IDENT_SET_SYM.invoke(outClassExpression, outClassTypeSymbol);

            var qualifier = Utils.TM_SELECT.invoke(
                    tm,
                    outClassExpression,
                    targetBsmName
            );
            Utils.QUAL_SET_SYM.invoke(qualifier, dynSym);
            Utils.QUAL_SET_TYPE.invoke(qualifier, methodType);

            var proxyCall = Utils.TM_APPLY.invoke(
                    tm,
                    Utils.LIST_NIL.invoke(),
                    qualifier,
                    Utils.LIST_FROM.invoke(parameters)
            );
            Utils.MTIN_SET_TYPE.invoke(proxyCall, returnType);

            Object execCall;
            if (returnType.getKind() != TypeKind.VOID) {
                execCall = Utils.TM_RETURN.invoke(tm, proxyCall);
            } else {
                execCall = Utils.TM_EXEC.invoke(tm, proxyCall);
            }

            Utils.JC_BLOCK_SET_STATS.invoke(method.getBody(), Utils.LIST_FROM.invoke(List.of(
                    execCall
            )));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static TypeMirror typeFromTree(Tree outType, Types types) {
        try {
            if (outType instanceof IdentifierTree identifierTree) {
                TypeElement paramType = (TypeElement) Utils.JC_VARIABLE_GET_SYMBOL.invoke(identifierTree);
                return paramType.asType();
            } else if (outType instanceof ArrayTypeTree arrayTypeTree) {
                try {
                    return types.getArrayType(typeFromTree(arrayTypeTree.getType(), types));
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            } else {
                PrimitiveTypeTree primitiveTypeTree = (PrimitiveTypeTree) outType;
                if (primitiveTypeTree.getPrimitiveTypeKind().isPrimitive()) {
                    return types.getPrimitiveType(primitiveTypeTree.getPrimitiveTypeKind());
                } else {
                    return types.getNoType(primitiveTypeTree.getPrimitiveTypeKind());
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
