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
                    processingEnv = Utils.processingEnvFromTask(task);
                    symtab = Utils.symTabFromTask(task);
                    treemaker = Utils.treeMakerFromTask(task);
                    names = Utils.namesFromTask(task);
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
                            AnnotationMirror annotation = Utils.jcAnnotationGetAttribute(a);
                            var binaryName = elements.getBinaryName((TypeElement) annotation.getAnnotationType().asElement());
                            if (binaryName.contentEquals(Open.class.getName())) {
                                hasOpen[0] = true;
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

        TypeElement enclosingClassTypeSymbol = Utils.jcClassGetSymbol(enclosingClass);

        var returnType = typeFromTree(method.getReturnType(), types);
        var parameterTypes = new ArrayList<TypeMirror>();
        List<ExpressionTree> parameters = new ArrayList<>();
        if (!method.getModifiers().getFlags().contains(Modifier.STATIC)) {
            parameterTypes.add(typeFromTree(enclosingClass, types));
            parameters.add(Utils.tmIdentVar(tm, method.getReceiverParameter()));
        }
        for (var param : method.getParameters()) {
            parameterTypes.add(typeFromTree(param.getType(), types));
            parameters.add(Utils.tmIdentVar(tm, param));
        }
        var methodType = Utils.methodType(
                Utils.listFrom(parameterTypes),
                returnType,
                Utils.listNil(),
                enclosingClassTypeSymbol
        );
        Element enclosing = enclosingClassTypeSymbol;
        while (!(enclosing instanceof PackageElement)) {
            enclosing = enclosing.getEnclosingElement();
        }
        var outClassName = Utils.fromStringNames(names, outClass);
        var outClassNameOnlyLast = Utils.fromStringNames(names, outClass.substring(outClass.lastIndexOf('.')+ 1));
        var outClassTypeSymbol = Utils.classSymbol(
                Opcodes.ACC_STATIC,
                outClassNameOnlyLast,
                enclosing
        );
        var targetType = Utils.methodType(
                Utils.listFrom(bsmStaticArgsTypes),
                callSiteType,
                Utils.listNil(),
                outClassTypeSymbol
        );
        var targetBsmName = Utils.fromStringNames(names, methodName);
        var methodSymbol = Utils.methodSymbol(
                Opcodes.ACC_STATIC,
                targetBsmName,
                targetType,
                outClassTypeSymbol
        );
        var dynSym = Utils.dynMethodSymbol(
                Utils.fromStringNames(names, "bridge"),
                Utils.symTabNoSymbol(symtab),
                Utils.asHandle(methodSymbol),
                methodType,
                Utils.loadableConstantArray(0)
        );

        var outClassExpression = Utils.tmIdent(tm, outClassName);
        Utils.exprSetType(outClassExpression, Utils.symbolAsType(outClassTypeSymbol));
        Utils.jcIdentSetSym(outClassExpression, outClassTypeSymbol);

        var qualifier = Utils.tmSelect(
                tm,
                outClassExpression,
                targetBsmName
        );
        Utils.qualSetSym(qualifier, dynSym);
        Utils.qualSetType(qualifier, methodType);

        var proxyCall = Utils.tmApply(
                tm,
                Utils.listNil(),
                qualifier,
                Utils.listFrom(parameters)
        );
        Utils.mtinSetType(proxyCall, returnType);

        StatementTree execCall;
        if (returnType.getKind() != TypeKind.VOID) {
            execCall = Utils.tmReturn(tm, proxyCall);
        } else {
            execCall = Utils.tmExec(tm, proxyCall);
        }

        Utils.jcBlockSetStats(method.getBody(), Utils.listFrom(List.of(
                execCall
        )));
    }

    private static TypeMirror typeFromTree(Tree outType, Types types) {
        if (outType instanceof IdentifierTree identifierTree) {
            TypeElement paramType = Utils.jcIdentGetSymbol(identifierTree);
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
    }
}
