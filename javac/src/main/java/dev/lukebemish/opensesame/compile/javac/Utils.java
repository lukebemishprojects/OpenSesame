package dev.lukebemish.opensesame.compile.javac;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.util.JavacTask;
import dev.lukebemish.opensesame.annotations.Open;
import dev.lukebemish.opensesame.runtime.OpeningMetafactory;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

class Utils {
    public static final MethodHandle JC_ANNOTATION_GET_ATTRIBUTE;
    public static final MethodHandle JC_VARIABLE_GET_SYMBOL;
    public static final MethodHandle JC_CLASS_GET_SYMBOL;
    public static final MethodHandle PROCESSING_ENV_FROM_TASK;
    public static final MethodHandle SYMTAB_FROM_TASK;
    public static final MethodHandle TREEMAKER_FROM_TASK;
    public static final MethodHandle NAMES_FROM_TASK;

    public static final MethodHandle TM_APPLY;
    public static final MethodHandle TM_IDENT;
    public static final MethodHandle TM_IDENT_VAR;
    public static final MethodHandle TM_SELECT;
    public static final MethodHandle TM_EXEC;
    public static final MethodHandle DYN_METHOD_SYM;
    public static final MethodHandle METHOD_SYM;
    public static final MethodHandle AS_HANDLE;
    public static final MethodHandle FROM_STRING_NAMES;
    public static final MethodHandle CLASS_SYMBOL;
    public static final MethodHandle METHOD_TYPE;
    public static final MethodHandle LIST_FROM;
    public static final MethodHandle LIST_NIL;
    public static final MethodHandle SYMTAB_NO_SYMBOL;
    public static final MethodHandle LOADABLE_CONSTANT_ARRAY;
    public static final MethodHandle QUAL_SET_SYM;
    public static final MethodHandle QUAL_SET_TYPE;
    public static final MethodHandle MTIN_SET_TYPE;
    public static final MethodHandle JC_BLOCK_SET_STATS;
    public static final MethodHandle EXPR_SET_TYPE;
    public static final MethodHandle SYMBOL_AS_TYPE;
    public static final MethodHandle JC_IDENT_SET_SYM;
    public static final MethodHandle TM_RETURN;

    static {
        try {
            JC_ANNOTATION_GET_ATTRIBUTE = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "attribute",
                    MethodType.methodType(
                            Class.forName("com.sun.tools.javac.code.Attribute$Compound"),
                            Class.forName("com.sun.tools.javac.tree.JCTree$JCAnnotation")
                    ),
                    Class.forName("com.sun.tools.javac.tree.JCTree$JCAnnotation"),
                    Open.Type.GET_INSTANCE.ordinal()
            ).getTarget().asType(MethodType.methodType(Object.class, AnnotationTree.class));

            JC_VARIABLE_GET_SYMBOL = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "sym",
                    MethodType.methodType(
                            Class.forName("com.sun.tools.javac.code.Symbol"),
                            Class.forName("com.sun.tools.javac.tree.JCTree$JCIdent")
                    ),
                    Class.forName("com.sun.tools.javac.tree.JCTree$JCIdent"),
                    Open.Type.GET_INSTANCE.ordinal()
            ).getTarget().asType(MethodType.methodType(Object.class, IdentifierTree.class));

            JC_CLASS_GET_SYMBOL = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "sym",
                    MethodType.methodType(
                            Class.forName("com.sun.tools.javac.code.Symbol$ClassSymbol"),
                            Class.forName("com.sun.tools.javac.tree.JCTree$JCClassDecl")
                    ),
                    Class.forName("com.sun.tools.javac.tree.JCTree$JCClassDecl"),
                    Open.Type.GET_INSTANCE.ordinal()
            ).getTarget().asType(MethodType.methodType(TypeElement.class, ClassTree.class));

            var getContext = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "getContext",
                    MethodType.methodType(
                            Class.forName("com.sun.tools.javac.util.Context"),
                            Class.forName("com.sun.tools.javac.api.BasicJavacTask")
                    ),
                    Class.forName("com.sun.tools.javac.api.BasicJavacTask"),
                    Open.Type.VIRTUAL.ordinal()
            ).getTarget();

            var getProcessingEnv = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "instance",
                    MethodType.methodType(
                            Class.forName("com.sun.tools.javac.processing.JavacProcessingEnvironment"),
                            Class.forName("com.sun.tools.javac.util.Context")
                    ),
                    Class.forName("com.sun.tools.javac.processing.JavacProcessingEnvironment"),
                    Open.Type.STATIC.ordinal()
            ).getTarget();

            var getSymTab = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "instance",
                    MethodType.methodType(
                            Class.forName("com.sun.tools.javac.code.Symtab"),
                            Class.forName("com.sun.tools.javac.util.Context")
                    ),
                    Class.forName("com.sun.tools.javac.code.Symtab"),
                    Open.Type.STATIC.ordinal()
            ).getTarget();

            var treeMakerClass = Class.forName("com.sun.tools.javac.tree.TreeMaker");

            var getTreeMaker = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "instance",
                    MethodType.methodType(
                            treeMakerClass,
                            Class.forName("com.sun.tools.javac.util.Context")
                    ),
                    treeMakerClass,
                    Open.Type.STATIC.ordinal()
            ).getTarget();

            var getNames = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "instance",
                    MethodType.methodType(
                            Class.forName("com.sun.tools.javac.util.Names"),
                            Class.forName("com.sun.tools.javac.util.Context")
                    ),
                    Class.forName("com.sun.tools.javac.util.Names"),
                    Open.Type.STATIC.ordinal()
            ).getTarget();

            PROCESSING_ENV_FROM_TASK = MethodHandles.filterReturnValue(
                    getContext,
                    getProcessingEnv
            ).asType(MethodType.methodType(ProcessingEnvironment.class, JavacTask.class));
            SYMTAB_FROM_TASK = MethodHandles.filterReturnValue(
                    getContext,
                    getSymTab
            );
            TREEMAKER_FROM_TASK = MethodHandles.filterReturnValue(
                    getContext,
                    getTreeMaker
            );
            NAMES_FROM_TASK = MethodHandles.filterReturnValue(
                    getContext,
                    getNames
            );

            var listClass = Class.forName("com.sun.tools.javac.util.List");
            var exprClass = Class.forName("com.sun.tools.javac.tree.JCTree$JCExpression");

            TM_APPLY = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "Apply",
                    MethodType.methodType(
                            Class.forName("com.sun.tools.javac.tree.JCTree$JCMethodInvocation"),
                            treeMakerClass,
                            listClass,
                            exprClass,
                            listClass
                    ),
                    treeMakerClass,
                    Open.Type.VIRTUAL.ordinal()
            ).getTarget();

            TM_EXEC = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "Exec",
                    MethodType.methodType(
                            Class.forName("com.sun.tools.javac.tree.JCTree$JCExpressionStatement"),
                            treeMakerClass,
                            exprClass
                    ),
                    treeMakerClass,
                    Open.Type.VIRTUAL.ordinal()
            ).getTarget();

            var nameClass = Class.forName("com.sun.tools.javac.util.Name");

            TM_IDENT = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "Ident",
                    MethodType.methodType(
                            Class.forName("com.sun.tools.javac.tree.JCTree$JCIdent"),
                            treeMakerClass,
                            nameClass
                    ),
                    treeMakerClass,
                    Open.Type.VIRTUAL.ordinal()
            ).getTarget();

            TM_IDENT_VAR = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "Ident",
                    MethodType.methodType(
                            Class.forName("com.sun.tools.javac.tree.JCTree$JCExpression"),
                            treeMakerClass,
                            Class.forName("com.sun.tools.javac.tree.JCTree$JCVariableDecl")
                    ),
                    treeMakerClass,
                    Open.Type.VIRTUAL.ordinal()
            ).getTarget();

            TM_SELECT = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "Select",
                    MethodType.methodType(
                            Class.forName("com.sun.tools.javac.tree.JCTree$JCFieldAccess"),
                            treeMakerClass,
                            exprClass,
                            nameClass
                    ),
                    treeMakerClass,
                    Open.Type.VIRTUAL.ordinal()
            ).getTarget();

            var returnClass = Class.forName("com.sun.tools.javac.tree.JCTree$JCReturn");

            TM_RETURN = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "Return",
                    MethodType.methodType(
                            returnClass,
                            treeMakerClass,
                            exprClass
                    ),
                    treeMakerClass,
                    Open.Type.VIRTUAL.ordinal()
            ).getTarget();

            var symClass = Class.forName("com.sun.tools.javac.code.Symbol");
            var methodSymClass = Class.forName("com.sun.tools.javac.code.Symbol$MethodSymbol");
            var dynMethodSymClass = Class.forName("com.sun.tools.javac.code.Symbol$DynamicMethodSymbol");
            var classSymClass = Class.forName("com.sun.tools.javac.code.Symbol$ClassSymbol");
            var methodHandleSymClass = Class.forName("com.sun.tools.javac.code.Symbol$MethodHandleSymbol");
            var typeSymbolClass = Class.forName("com.sun.tools.javac.code.Symbol$TypeSymbol");
            var typeClass = Class.forName("com.sun.tools.javac.code.Type");
            var methodTypeClass = Class.forName("com.sun.tools.javac.code.Type$MethodType");
            var loadableConstantClass = Class.forName("com.sun.tools.javac.jvm.PoolConstant$LoadableConstant");

            DYN_METHOD_SYM = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "<init>",
                    MethodType.methodType(
                           dynMethodSymClass,
                            nameClass,
                            symClass,
                            methodHandleSymClass,
                            typeClass,
                            loadableConstantClass.arrayType()
                    ),
                    dynMethodSymClass,
                    Open.Type.CONSTRUCT.ordinal()
            ).getTarget();

            METHOD_SYM = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "<init>",
                    MethodType.methodType(
                            methodSymClass,
                            long.class,
                            nameClass,
                            typeClass,
                            symClass
                    ),
                    methodSymClass,
                    Open.Type.CONSTRUCT.ordinal()
            ).getTarget();

            AS_HANDLE = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "asHandle",
                    MethodType.methodType(
                            methodHandleSymClass,
                            methodSymClass
                    ),
                    methodSymClass,
                    Open.Type.VIRTUAL.ordinal()
            ).getTarget();

            FROM_STRING_NAMES = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "fromString",
                    MethodType.methodType(
                            nameClass,
                            Class.forName("com.sun.tools.javac.util.Names"),
                            Class.forName("java.lang.String")
                    ),
                    Class.forName("com.sun.tools.javac.util.Names"),
                    Open.Type.VIRTUAL.ordinal()
            ).getTarget();

            CLASS_SYMBOL = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "ClassSymbol",
                    MethodType.methodType(
                            classSymClass,
                            long.class,
                            nameClass,
                            symClass
                    ),
                    classSymClass,
                    Open.Type.CONSTRUCT.ordinal()
            ).getTarget();

            METHOD_TYPE = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "MethodType",
                    MethodType.methodType(
                            methodTypeClass,
                            listClass,
                            typeClass,
                            listClass,
                            typeSymbolClass
                    ),
                    methodTypeClass,
                    Open.Type.CONSTRUCT.ordinal()
            ).getTarget();

            LIST_FROM = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "from",
                    MethodType.methodType(
                            listClass,
                            Iterable.class
                    ),
                    listClass,
                    Open.Type.STATIC.ordinal()
            ).getTarget();

            LIST_NIL = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "nil",
                    MethodType.methodType(
                            listClass
                    ),
                    listClass,
                    Open.Type.STATIC.ordinal()
            ).getTarget();

            SYMTAB_NO_SYMBOL = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "noSymbol",
                    MethodType.methodType(
                            typeSymbolClass,
                            Class.forName("com.sun.tools.javac.code.Symtab")
                    ),
                    Class.forName("com.sun.tools.javac.code.Symtab"),
                    Open.Type.GET_INSTANCE.ordinal()
            ).getTarget();

            LOADABLE_CONSTANT_ARRAY = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "loadableConstantArray",
                    MethodType.methodType(
                            loadableConstantClass.arrayType(),
                            int.class
                    ),
                    loadableConstantClass,
                    Open.Type.ARRAY.ordinal()
            ).getTarget();

            var fieldAccessClass = Class.forName("com.sun.tools.javac.tree.JCTree$JCFieldAccess");

            QUAL_SET_SYM = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "sym",
                    MethodType.methodType(
                            void.class,
                            fieldAccessClass,
                            symClass
                    ),
                    fieldAccessClass,
                    Open.Type.SET_INSTANCE.ordinal()
            ).getTarget();

            QUAL_SET_TYPE = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "type",
                    MethodType.methodType(
                            void.class,
                            fieldAccessClass,
                            typeClass
                    ),
                    fieldAccessClass,
                    Open.Type.SET_INSTANCE.ordinal()
            ).getTarget();

            var methodInvocationClass = Class.forName("com.sun.tools.javac.tree.JCTree$JCMethodInvocation");

            MTIN_SET_TYPE = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "type",
                    MethodType.methodType(
                            void.class,
                            methodInvocationClass,
                            typeClass
                    ),
                    methodInvocationClass,
                    Open.Type.SET_INSTANCE.ordinal()
            ).getTarget();

            var blockClass = Class.forName("com.sun.tools.javac.tree.JCTree$JCBlock");

            JC_BLOCK_SET_STATS = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "stats",
                    MethodType.methodType(
                            void.class,
                            blockClass,
                            listClass
                    ),
                    blockClass,
                    Open.Type.SET_INSTANCE.ordinal()
            ).getTarget();

            EXPR_SET_TYPE = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "setType",
                    MethodType.methodType(
                            exprClass,
                            exprClass,
                            typeClass
                    ),
                    exprClass,
                    Open.Type.VIRTUAL.ordinal()
            ).getTarget();

            SYMBOL_AS_TYPE = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "asType",
                    MethodType.methodType(
                            typeClass,
                            symClass
                    ),
                    symClass,
                    Open.Type.VIRTUAL.ordinal()
            ).getTarget();

            var identClass = Class.forName("com.sun.tools.javac.tree.JCTree$JCIdent");

            JC_IDENT_SET_SYM = OpeningMetafactory.invokeKnownUnsafe(
                    MethodHandles.lookup(),
                    "sym",
                    MethodType.methodType(
                            void.class,
                            identClass,
                            symClass
                    ),
                    identClass,
                    Open.Type.SET_INSTANCE.ordinal()
            ).getTarget();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
