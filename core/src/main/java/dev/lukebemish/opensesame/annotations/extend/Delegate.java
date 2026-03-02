package dev.lukebemish.opensesame.annotations.extend;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as a delegate field. The (coerced) type of the field must match the extension target or an implemented
 * interface of the extension interface. The generated extension class will implement all otherwise unimplemented
 * methods of the field type by delegating to the field.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.PARAMETER})
public @interface Delegate {
    
}
