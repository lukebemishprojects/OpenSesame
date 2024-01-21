package dev.lukebemish.opensesame.compile.javac;

import com.sun.source.tree.*;
import com.sun.source.util.JavacTask;
import dev.lukebemish.opensesame.annotations.Coerce;
import dev.lukebemish.opensesame.annotations.Open;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

final class Utils {
    private static final String SYMBOL = "com.sun.tools.javac.code.Symbol";
    private static final String TM = "com.sun.tools.javac.tree.TreeMaker";
    private static final String BASIC_JAVA_TASK = "com.sun.tools.javac.api.BasicJavacTask";
    private static final String CONTEXT = "com.sun.tools.javac.util.Context";
    private static final String PROCESSING_ENV = "com.sun.tools.javac.processing.JavacProcessingEnvironment";
    private static final String NAME = "com.sun.tools.javac.util.Name";
    private static final String JCIDENT = "com.sun.tools.javac.tree.JCTree$JCIdent";
    private static final String TYPE = "com.sun.tools.javac.code.Type";
    private static final String JCEXPRESSION = "com.sun.tools.javac.tree.JCTree$JCExpression";
    private static final String LIST = "com.sun.tools.javac.util.List";

    private Utils() {}

    @Open(
            name = "attribute",
            targetName = "com.sun.tools.javac.tree.JCTree$JCAnnotation",
            type = Open.Type.GET_INSTANCE,
            unsafe = true
    )
    static @Coerce(targetName = "com.sun.tools.javac.code.Attribute$Compound") AnnotationMirror jcAnnotationGetAttribute(@Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCAnnotation") AnnotationTree ignoredTree) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "sym",
            targetName = JCIDENT,
            type = Open.Type.GET_INSTANCE,
            unsafe = true
    )
    static @Coerce(targetName = SYMBOL) TypeElement jcIdentGetSymbol(@Coerce(targetName = JCIDENT) IdentifierTree ignoredTree) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "sym",
            targetName = "com.sun.tools.javac.tree.JCTree$JCClassDecl",
            type = Open.Type.GET_INSTANCE,
            unsafe = true
    )
    static @Coerce(targetName = "com.sun.tools.javac.code.Symbol$ClassSymbol") TypeElement jcClassGetSymbol(@Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCClassDecl") ClassTree ignoredTree) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "getContext",
            targetName = BASIC_JAVA_TASK,
            type = Open.Type.VIRTUAL,
            unsafe = true
    )
    private static @Coerce(targetName = CONTEXT) Object getContext(@Coerce(targetName = BASIC_JAVA_TASK) JavacTask ignoredTask) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "instance",
            targetName = PROCESSING_ENV,
            type = Open.Type.STATIC,
            unsafe = true
    )
    private static @Coerce(targetName = PROCESSING_ENV) ProcessingEnvironment getProcessingEnv(@Coerce(targetName = CONTEXT) Object ignoredContext) {
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
    private static @Coerce(targetName = "com.sun.tools.javac.code.Symtab") Object getSymTab(@Coerce(targetName = CONTEXT) Object ignoredContext) {
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
    private static @Coerce(targetName = TM) Object getTreeMaker(@Coerce(targetName = CONTEXT) Object ignoredContext) {
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
    static @Coerce(targetName = "com.sun.tools.javac.util.Names") Object getNames(@Coerce(targetName = CONTEXT) Object ignoredContext) {
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
    static @Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCMethodInvocation") Object tmApply(@Coerce(targetName = TM) Object ignoredTreeMaker, @Coerce(targetName = LIST) List<? extends ExpressionTree> ignoredTypeArgs, @Coerce(targetName = JCEXPRESSION) Object ignoredMeth, @Coerce(targetName = LIST) List<? extends ExpressionTree> ignoredArgs) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "Ident",
            targetName = TM,
            type = Open.Type.VIRTUAL,
            unsafe = true
    )
    static @Coerce(targetName = JCIDENT) IdentifierTree tmIdent(@Coerce(targetName = TM) Object ignoredTreeMaker, @Coerce(targetName = NAME) Name ignoredName) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "Ident",
            targetName = TM,
            type = Open.Type.VIRTUAL,
            unsafe = true
    )
    static @Coerce(targetName = JCEXPRESSION) ExpressionTree tmIdentVar(@Coerce(targetName = TM) Object ignoredTreeMaker, @Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCVariableDecl") Object ignoredVar) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "Select",
            targetName = TM,
            type = Open.Type.VIRTUAL,
            unsafe = true
    )
    static @Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCFieldAccess") MemberSelectTree tmSelect(@Coerce(targetName = TM) Object ignoredTreeMaker, @Coerce(targetName = JCEXPRESSION) Object ignoredSelected, @Coerce(targetName = NAME) Name ignoredName) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "Exec",
            targetName = TM,
            type = Open.Type.VIRTUAL,
            unsafe = true
    )
    static @Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCExpressionStatement") ExpressionStatementTree tmExec(@Coerce(targetName = TM) Object ignoredTreeMaker, @Coerce(targetName = JCEXPRESSION) Object ignoredExpr) {
        throw new UnsupportedOperationException();
    }

    @Open(
            targetName = "com.sun.tools.javac.code.Symbol$DynamicMethodSymbol",
            type = Open.Type.CONSTRUCT,
            unsafe = true
    )
    static ExecutableElement dynMethodSymbol(@Coerce(targetName = NAME) Name ignoredName, @Coerce(targetName = SYMBOL) Object ignoredOwner, @Coerce(targetName = "com.sun.tools.javac.code.Symbol$MethodHandleSymbol") Object ignoredMethodHandle, @Coerce(targetName = TYPE) Object ignoredType, @Coerce(targetName = "[Lcom/sun/tools/javac/jvm/PoolConstant$LoadableConstant;") Object ignoredLoadableConstants) {
        throw new UnsupportedOperationException();
    }

    @Open(
            targetName = "com.sun.tools.javac.code.Symbol$MethodSymbol",
            type = Open.Type.CONSTRUCT,
            unsafe = true
    )
    static ExecutableElement methodSymbol(@SuppressWarnings("SameParameterValue") long ignoredFlags, @Coerce(targetName = NAME) Name ignoredName, @Coerce(targetName = TYPE) Object ignoredType, @Coerce(targetName = SYMBOL) Object ignoredOwner) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "asHandle",
            targetName = "com.sun.tools.javac.code.Symbol$MethodSymbol",
            type = Open.Type.VIRTUAL,
            unsafe = true
    )
    static @Coerce(targetName = "com.sun.tools.javac.code.Symbol$MethodHandleSymbol") ExecutableElement asHandle(@Coerce(targetName = "com.sun.tools.javac.code.Symbol$MethodSymbol") Object ignoredMethodSymbol) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "fromString",
            targetName = "com.sun.tools.javac.util.Names",
            type = Open.Type.VIRTUAL,
            unsafe = true
    )
    static @Coerce(targetName = NAME) Name fromStringNames(@Coerce(targetName = "com.sun.tools.javac.util.Names") Object ignoredNames, String ignoredName) {
        throw new UnsupportedOperationException();
    }

    @Open(
            targetName = "com.sun.tools.javac.code.Symbol$ClassSymbol",
            type = Open.Type.CONSTRUCT,
            unsafe = true
    )
    static Object classSymbol(@SuppressWarnings("SameParameterValue") long ignoredFlags, @Coerce(targetName = NAME) Name ignoredName, @Coerce(targetName = SYMBOL) Object ignoredOwner) {
        throw new UnsupportedOperationException();
    }

    @Open(
            targetName = "com.sun.tools.javac.code.Type$MethodType",
            type = Open.Type.CONSTRUCT,
            unsafe = true
    )
    static Object methodType(@Coerce(targetName = LIST) List<? extends TypeMirror> ignoredArgtypes, @Coerce(targetName = TYPE) Object ignoredRestype, @Coerce(targetName = LIST) List<? extends TypeMirror> ignoredThrown, @Coerce(targetName = "com.sun.tools.javac.code.Symbol$TypeSymbol") Object ignoredTsym) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "from",
            targetName = LIST,
            type = Open.Type.STATIC,
            unsafe = true
    )
    static <T> @Coerce(targetName = LIST) List<T> listFrom(Iterable<T> ignoredIterable) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "nil",
            targetName = LIST,
            type = Open.Type.STATIC,
            unsafe = true
    )
    static <T> @Coerce(targetName = LIST) List<T> listNil() {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "noSymbol",
            targetName = "com.sun.tools.javac.code.Symtab",
            type = Open.Type.GET_INSTANCE,
            unsafe = true
    )
    static @Coerce(targetName = "com.sun.tools.javac.code.Symbol$TypeSymbol") Object symTabNoSymbol(@Coerce(targetName = "com.sun.tools.javac.code.Symtab") Object ignoredSymtab) {
        throw new UnsupportedOperationException();
    }

    @Open(
            targetName = "com.sun.tools.javac.jvm.PoolConstant$LoadableConstant",
            type = Open.Type.ARRAY,
            unsafe = true
    )
    static Object[] loadableConstantArray(@SuppressWarnings("SameParameterValue") int ignoredSize) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "sym",
            targetName = "com.sun.tools.javac.tree.JCTree$JCFieldAccess",
            type = Open.Type.SET_INSTANCE,
            unsafe = true
    )
    static void qualSetSym(@Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCFieldAccess") Object ignoredFieldAccess, @Coerce(targetName = SYMBOL) Object ignoredSym) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "type",
            targetName = "com.sun.tools.javac.tree.JCTree$JCFieldAccess",
            type = Open.Type.SET_INSTANCE,
            unsafe = true
    )
    static void qualSetType(@Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCFieldAccess") Object ignoredFieldAccess, @Coerce(targetName = TYPE) Object ignoredType) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "type",
            targetName = "com.sun.tools.javac.tree.JCTree$JCMethodInvocation",
            type = Open.Type.SET_INSTANCE,
            unsafe = true
    )
    static void mtinSetType(@Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCMethodInvocation") Object ignoredMethodInvocation, @Coerce(targetName = TYPE) Object ignoredType) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "stats",
            targetName = "com.sun.tools.javac.tree.JCTree$JCBlock",
            type = Open.Type.SET_INSTANCE,
            unsafe = true
    )
    static void jcBlockSetStats(@Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCBlock") Object ignoredBlock, @Coerce(targetName = LIST) List<? extends StatementTree> ignoredStats) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "setType",
            targetName = JCEXPRESSION,
            type = Open.Type.VIRTUAL,
            unsafe = true
    )
    static @Coerce(targetName = JCEXPRESSION) void exprSetType(@Coerce(targetName = JCEXPRESSION) Object ignoredExpr, @Coerce(targetName = TYPE) Object ignoredType) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "asType",
            targetName = SYMBOL,
            type = Open.Type.VIRTUAL,
            unsafe = true
    )
    static @Coerce(targetName = TYPE) Object symbolAsType(@Coerce(targetName = SYMBOL) Object ignoredSym) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "sym",
            targetName = JCIDENT,
            type = Open.Type.SET_INSTANCE,
            unsafe = true
    )
    static void jcIdentSetSym(@Coerce(targetName = JCIDENT) Object ignoredIdent, @Coerce(targetName = SYMBOL) Object ignoredSym) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "Return",
            targetName = TM,
            type = Open.Type.VIRTUAL,
            unsafe = true
    )
    static @Coerce(targetName = "com.sun.tools.javac.tree.JCTree$JCReturn") ReturnTree tmReturn(@Coerce(targetName = TM) Object ignoredTreeMaker, @Coerce(targetName = JCEXPRESSION) Object ignoredExpr) {
        throw new UnsupportedOperationException();
    }
}
