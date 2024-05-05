package dev.lukebemish.opensesame.annotations.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can be used to mark an otherwise hidden (private or package-private) target as being exposed to implementors on class
 * load via mixin plugin. Additionally, gives OpenSesame a lookup within the target module while accessing the annotated
 * target. Can be used on:
 * <ul>
 *     <li>{@link dev.lukebemish.opensesame.annotations.extend.Extend} interfaces, to mark the target class/interface as public.</li>
 *     <li>{@link dev.lukebemish.opensesame.annotations.extend.Overrides} methods, to mark the method being overridden as protected if it is package private or private.</li>
 * </ul>
 * The files necessary for the mixin plugin to discover the target will be generated at compile time, and discovered via
 * {@link java.util.ServiceLoader} at runtime.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Expose {}
