package dev.lukebemish.opensesame.compile.javac;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.util.JavacTask;
import dev.lukebemish.opensesame.annotations.Coerce;
import dev.lukebemish.opensesame.annotations.Open;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;

final class Utils {
    private Utils() {}

    private static final String TM = "com.sun.tools.javac.tree.TreeMaker";

    @Open(
            name = "attribute",
            targetName = "com.sun.tools.javac.tree.JCTree$JCAnnotation",
            type = Open.Type.GET_INSTANCE,
            unsafe = true
    )
    static @Coerce(targetName = "com.sun.tools.javac.code.Attribute$Compound") AnnotationMirror jcAnnotationGetAttribute(@Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCAnnotation") AnnotationTree tree) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "sym",
            targetName = "com.sun.tools.javac.tree.JCTree$JCIdent",
            type = Open.Type.GET_INSTANCE,
            unsafe = true
    )
    static @Coerce(targetName = "com.sun.tools.javac.code.Symbol") TypeElement jcIdentGetSymbol(@Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCIdent") IdentifierTree tree) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "sym",
            targetName = "com.sun.tools.javac.tree.JCTree$JCClassDecl",
            type = Open.Type.GET_INSTANCE,
            unsafe = true
    )
    static @Coerce(targetName = "com.sun.tools.javac.code.Symbol$ClassSymbol") TypeElement jcClassGetSymbol(@Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCClassDecl") ClassTree tree) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "getContext",
            targetName = "com.sun.tools.javac.api.BasicJavacTask",
            type = Open.Type.VIRTUAL,
            unsafe = true
    )
    private static @Coerce(targetName = "com.sun.tools.javac.util.Context") Object getContext(@Coerce(targetName = "com.sun.tools.javac.api.BasicJavacTask") JavacTask task) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "instance",
            targetName = "com.sun.tools.javac.processing.JavacProcessingEnvironment",
            type = Open.Type.STATIC,
            unsafe = true
    )
    private static @Coerce(targetName = "com.sun.tools.javac.processing.JavacProcessingEnvironment") ProcessingEnvironment getProcessingEnv(@Coerce(targetName = "com.sun.tools.javac.util.Context") Object context) {
        throw new UnsupportedOperationException();
    }

    static ProcessingEnvironment processingEnvFromTask(JavacTask task) {
        return getProcessingEnv(getContext(task));
    }

    @Open(
            name = "instance",
            targetName = "com.sun.tools.javac.code.Symtab",
            type = Open.Type.STATIC,
            unsafe = true
    )
    private static @Coerce(targetName = "com.sun.tools.javac.code.Symtab") Object getSymTab(@Coerce(targetName = "com.sun.tools.javac.util.Context") Object context) {
        throw new UnsupportedOperationException();
    }

    static Object symTabFromTask(JavacTask task) {
        return getSymTab(getContext(task));
    }

    @Open(
            name = "instance",
            targetName = TM,
            type = Open.Type.STATIC,
            unsafe = true
    )
    private static @Coerce(targetName = TM) Object getTreeMaker(@Coerce(targetName = "com.sun.tools.javac.util.Context") Object context) {
        throw new UnsupportedOperationException();
    }

    static Object treeMakerFromTask(JavacTask task) {
        return getTreeMaker(getContext(task));
    }

    @Open(
            name = "instance",
            targetName = "com.sun.tools.javac.util.Names",
            type = Open.Type.STATIC,
            unsafe = true
    )
    static @Coerce(targetName = "com.sun.tools.javac.util.Names") Object getNames(@Coerce(targetName = "com.sun.tools.javac.util.Context") Object context) {
        throw new UnsupportedOperationException();
    }

    static Object namesFromTask(JavacTask task) {
        return getNames(getContext(task));
    }

    @Open(
            name = "Apply",
            targetName = TM,
            type = Open.Type.VIRTUAL,
            unsafe = true
    )
    static @Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCMethodInvocation") Object tmApply(@Coerce(targetName = TM) Object treeMaker, @Coerce(targetName = "com.sun.tools.javac.util.List") Object typeArgs, @Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCExpression") Object meth, @Coerce(targetName = "com.sun.tools.javac.util.List") Object args) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "Ident",
            targetName = TM,
            type = Open.Type.VIRTUAL,
            unsafe = true
    )
    static @Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCIdent") Object tmIdent(@Coerce(targetName = TM) Object treeMaker, @Coerce(targetName = "com.sun.tools.javac.util.Name") Object name) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "Ident",
            targetName = TM,
            type = Open.Type.VIRTUAL,
            unsafe = true
    )
    static @Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCExpression") Object tmIdentVar(@Coerce(targetName = TM) Object treeMaker, @Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCVariableDecl") Object var) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "Select",
            targetName = TM,
            type = Open.Type.VIRTUAL,
            unsafe = true
    )
    static @Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCFieldAccess") Object tmSelect(@Coerce(targetName = TM) Object treeMaker, @Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCExpression") Object selected, @Coerce(targetName = "com.sun.tools.javac.util.Name") Object name) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "Exec",
            targetName = TM,
            type = Open.Type.VIRTUAL,
            unsafe = true
    )
    static @Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCExpressionStatement") Object tmExec(@Coerce(targetName = TM) Object treeMaker, @Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCExpression") Object expr) {
        throw new UnsupportedOperationException();
    }

    @Open(
            targetName = "com.sun.tools.javac.code.Symbol$DynamicMethodSymbol",
            type = Open.Type.CONSTRUCT,
            unsafe = true
    )
    static Object dynMethodSymbol(@Coerce(targetName = "com.sun.tools.javac.util.Name") Object name, @Coerce(targetName = "com.sun.tools.javac.code.Symbol") Object owner, @Coerce(targetName = "com.sun.tools.javac.code.Symbol$MethodHandleSymbol") Object methodHandle, @Coerce(targetName = "com.sun.tools.javac.code.Type") Object type, @Coerce(targetName = "[Lcom/sun/tools/javac/jvm/PoolConstant$LoadableConstant;") Object loadableConstants) {
        throw new UnsupportedOperationException();
    }

    @Open(
            targetName = "com.sun.tools.javac.code.Symbol$MethodSymbol",
            type = Open.Type.CONSTRUCT,
            unsafe = true
    )
    static Object methodSymbol(long flags, @Coerce(targetName = "com.sun.tools.javac.util.Name") Object name, @Coerce(targetName = "com.sun.tools.javac.code.Type") Object type, @Coerce(targetName = "com.sun.tools.javac.code.Symbol") Object owner) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "asHandle",
            targetName = "com.sun.tools.javac.code.Symbol$MethodSymbol",
            type = Open.Type.VIRTUAL,
            unsafe = true
    )
    static @Coerce(targetName = "com.sun.tools.javac.code.Symbol$MethodHandleSymbol") Object asHandle(@Coerce(targetName = "com.sun.tools.javac.code.Symbol$MethodSymbol") Object methodSymbol) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "fromString",
            targetName = "com.sun.tools.javac.util.Names",
            type = Open.Type.VIRTUAL,
            unsafe = true
    )
    static @Coerce(targetName = "com.sun.tools.javac.util.Name") Object fromStringNames(@Coerce(targetName = "com.sun.tools.javac.util.Names") Object names, String name) {
        throw new UnsupportedOperationException();
    }

    @Open(
            targetName = "com.sun.tools.javac.code.Symbol$ClassSymbol",
            type = Open.Type.CONSTRUCT,
            unsafe = true
    )
    static Object classSymbol(long flags, @Coerce(targetName = "com.sun.tools.javac.util.Name") Object name, @Coerce(targetName = "com.sun.tools.javac.code.Symbol") Object owner) {
        throw new UnsupportedOperationException();
    }

    @Open(
            targetName = "com.sun.tools.javac.code.Type$MethodType",
            type = Open.Type.CONSTRUCT,
            unsafe = true
    )
    static Object methodType(@Coerce(targetName = "com.sun.tools.javac.util.List") Object argtypes, @Coerce(targetName = "com.sun.tools.javac.code.Type") Object restype, @Coerce(targetName = "com.sun.tools.javac.util.List") Object thrown, @Coerce(targetName = "com.sun.tools.javac.code.Symbol$TypeSymbol") Object tsym) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "from",
            targetName = "com.sun.tools.javac.util.List",
            type = Open.Type.STATIC,
            unsafe = true
    )
    static @Coerce(targetName = "com.sun.tools.javac.util.List") Object listFrom(@Coerce(targetName = "java.lang.Iterable") Object iterable) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "nil",
            targetName = "com.sun.tools.javac.util.List",
            type = Open.Type.STATIC,
            unsafe = true
    )
    static @Coerce(targetName = "com.sun.tools.javac.util.List") Object listNil() {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "noSymbol",
            targetName = "com.sun.tools.javac.code.Symtab",
            type = Open.Type.GET_INSTANCE,
            unsafe = true
    )
    static @Coerce(targetName = "com.sun.tools.javac.code.Symbol$TypeSymbol") Object symTabNoSymbol(@Coerce(targetName = "com.sun.tools.javac.code.Symtab") Object symtab) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "loadableConstantArray",
            targetName = "com.sun.tools.javac.jvm.PoolConstant$LoadableConstant",
            type = Open.Type.ARRAY,
            unsafe = true
    )
    static Object[] loadableConstantArray(int size) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "sym",
            targetName = "com.sun.tools.javac.tree.JCTree$JCFieldAccess",
            type = Open.Type.SET_INSTANCE,
            unsafe = true
    )
    static void qualSetSym(@Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCFieldAccess") Object fieldAccess, @Coerce(targetName = "com.sun.tools.javac.code.Symbol") Object sym) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "type",
            targetName = "com.sun.tools.javac.tree.JCTree$JCFieldAccess",
            type = Open.Type.SET_INSTANCE,
            unsafe = true
    )
    static void qualSetType(@Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCFieldAccess") Object fieldAccess, @Coerce(targetName = "com.sun.tools.javac.code.Type") Object type) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "type",
            targetName = "com.sun.tools.javac.tree.JCTree$JCMethodInvocation",
            type = Open.Type.SET_INSTANCE,
            unsafe = true
    )
    static void mtinSetType(@Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCMethodInvocation") Object methodInvocation, @Coerce(targetName = "com.sun.tools.javac.code.Type") Object type) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "stats",
            targetName = "com.sun.tools.javac.tree.JCTree$JCBlock",
            type = Open.Type.SET_INSTANCE,
            unsafe = true
    )
    static void jcBlockSetStats(@Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCBlock") Object block, @Coerce(targetName = "com.sun.tools.javac.util.List") Object stats) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "setType",
            targetName = "com.sun.tools.javac.tree.JCTree$JCExpression",
            type = Open.Type.VIRTUAL,
            unsafe = true
    )
    static @Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCExpression") Object exprSetType(@Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCExpression") Object expr, @Coerce(targetName = "com.sun.tools.javac.code.Type") Object type) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "asType",
            targetName = "com.sun.tools.javac.code.Symbol",
            type = Open.Type.VIRTUAL,
            unsafe = true
    )
    static @Coerce(targetName = "com.sun.tools.javac.code.Type") Object symbolAsType(@Coerce(targetName = "com.sun.tools.javac.code.Symbol") Object sym) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "sym",
            targetName = "com.sun.tools.javac.tree.JCTree$JCIdent",
            type = Open.Type.SET_INSTANCE,
            unsafe = true
    )
    static void jcIdentSetSym(@Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCIdent") Object ident, @Coerce(targetName = "com.sun.tools.javac.code.Symbol") Object sym) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "Return",
            targetName = TM,
            type = Open.Type.VIRTUAL,
            unsafe = true
    )
    static @Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCReturn") Object tmReturn(@Coerce(targetName = TM) Object treeMaker, @Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCExpression") Object expr) {
        throw new UnsupportedOperationException();
    }
}
