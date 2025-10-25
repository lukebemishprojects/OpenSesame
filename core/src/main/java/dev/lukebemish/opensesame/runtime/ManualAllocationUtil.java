package dev.lukebemish.opensesame.runtime;

import org.jetbrains.annotations.ApiStatus;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

@ApiStatus.Internal
final class ManualAllocationUtil {
    private ManualAllocationUtil() {}
    
    static MethodHandle constructionHandle(Class<?> subClass, Class<?> targetClass, MethodHandle superClassCtor, MethodHandles.Lookup unsafeLookup, String[] fields) throws Throwable {
        var constructorClass = Class.forName("java.lang.invoke.DirectMethodHandle$Constructor");
        var initMethodField = constructorClass.getDeclaredField("initMethod");
        var memberName = unsafeLookup.unreflectGetter(initMethodField).invoke(superClassCtor);

        //noinspection Java9ReflectionClassVisibility
        var memberNameClass = Class.forName("java.lang.invoke.MemberName");
        var flagsField = memberNameClass.getDeclaredField("flags");
        var flags = (int) unsafeLookup.unreflectGetter(flagsField).invoke(memberName);
        flags &= ~0x00020000; // remove "is constructor"
        flags |= 0x00010000; // add "is (non-constructor) method"
        unsafeLookup.unreflectSetter(flagsField).invoke(memberName, flags);

        var getDirectMethodMethodHandle = unsafeLookup.findVirtual(
                MethodHandles.Lookup.class,
                "getDirectMethod",
                MethodType.methodType(
                        MethodHandle.class,
                        byte.class,
                        Class.class,
                        memberNameClass,
                        MethodHandles.Lookup.class
                )
        );

        var constructionHandle = (MethodHandle) getDirectMethodMethodHandle.invoke(unsafeLookup, (byte) 5, superClassCtor.type().returnType(), memberName, unsafeLookup);
        constructionHandle = constructionHandle.asFixedArity();
        
        var unsafeClass = Class.forName("sun.misc.Unsafe", true, ManualAllocationUtil.class.getClassLoader());
        Object theUnsafe = UnsafeProvision.theUnsafe();
        var allocateInstanceHandle = unsafeLookup.findVirtual(
                unsafeClass,
                "allocateInstance",
                MethodType.methodType(Object.class, Class.class)
        ).bindTo(theUnsafe).bindTo(subClass);
        
        // (args...) -> subClass
        var identify = MethodHandles.identity(targetClass);
        var constructAndReturn = MethodHandles.foldArguments(
                MethodHandles.dropArguments(identify, 1, Arrays.copyOfRange(constructionHandle.type().parameterArray(), 1, constructionHandle.type().parameterCount())),
                0,
                constructionHandle
        );
        var allocateThenConstruct = MethodHandles.foldArguments(
                constructAndReturn,
                allocateInstanceHandle.asType(allocateInstanceHandle.type().changeReturnType(targetClass))
        );
        var fullHandle = allocateThenConstruct.asType(allocateThenConstruct.type().changeReturnType(subClass));

        var subClassIdentify = MethodHandles.identity(subClass);
        
        for (int i = fields.length - 1; i >= 0; i--) {
            var fieldName = fields[i];
            var fieldType = subClass.getDeclaredField(fieldName).getType();
            var setter = unsafeLookup.findSetter(
                    subClass,
                    fieldName,
                    fieldType
            );
            var setAndReturn = MethodHandles.foldArguments(
                    MethodHandles.dropArguments(subClassIdentify, 1, fieldType),
                    0,
                    setter
            );
            fullHandle = MethodHandles.collectArguments(
                    MethodHandles.permuteArguments(setAndReturn, MethodType.methodType(subClass, fieldType, subClass), 1, 0),
                    1,
                    fullHandle
            );
        }
        return fullHandle;
    }
}
