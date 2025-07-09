package dev.lukebemish.opensesame.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Added to generated extension classes on ctors that cannot be properly invoked, and must be created manually via
 * allocation and invocation. Should not be added to user code.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
public @interface ManualAllocation {
    Class<?>[] superConstructor();
    Class<?> superClass();
    String[] fields();
}
