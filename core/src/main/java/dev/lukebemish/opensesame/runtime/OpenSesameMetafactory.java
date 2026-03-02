package dev.lukebemish.opensesame.runtime;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class OpenSesameMetafactory {
    public static CallSite invoke(
            MethodHandles.Lookup caller,
            String ignored,
            MethodType factoryType,
            OpenInfo info,
            boolean allowUnsafe
    ) {
        var state = new LookupState(allowUnsafe, info.target(), caller);
        var lookup = state.targetLookup();
        try {
            var handle = switch (info.type()) {
                case STATIC -> lookup.findStatic(info.target(), info.name(), info.targetType());
                case VIRTUAL -> lookup.findVirtual(info.target(), info.name(), info.targetType().dropParameterTypes(0, 1));
                case SPECIAL -> lookup.findSpecial(info.target(), info.name(), info.targetType().dropParameterTypes(0, 1), info.target());
                case GET_STATIC -> lookup.findStaticGetter(info.target(), info.name(), info.targetType().returnType());
                case GET_INSTANCE -> lookup.findGetter(info.target(), info.name(), info.targetType().returnType());
                case SET_STATIC -> lookup.findStaticSetter(info.target(), info.name(), info.targetType().parameterType(0));
                case SET_INSTANCE -> lookup.findSetter(info.target(), info.name(), info.targetType().parameterType(1));
                case CONSTRUCT -> lookup.findConstructor(info.target(), info.targetType().changeReturnType(Void.TYPE));
                case ARRAY -> MethodHandles.arrayConstructor(info.target().arrayType());
            };
            return new ConstantCallSite(handle.asType(factoryType));
        } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException e) {
            throw new OpeningException("Issue creating method handle for `"+info.name()+"`", e);
        }
    }
}
