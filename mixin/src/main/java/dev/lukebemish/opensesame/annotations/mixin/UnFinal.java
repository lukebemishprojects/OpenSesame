package dev.lukebemish.opensesame.annotations.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can be applied alongside various other mixins to mark a target to be made non-final and unsealed on class load via
 * the mixin plugin. Can be used on:
 * <ul>
 *     <li>{@link dev.lukebemish.opensesame.annotations.Open} field setters/getters, to make the field mutable.</li>
 *     <li>{@link dev.lukebemish.opensesame.annotations.extend.Extend} interfaces, to mark the target class/interface as unsealed and extendable.</li>
 *     <li>{@link dev.lukebemish.opensesame.annotations.extend.Overrides} methods, to mark the method being overridden as non-final.</li>
 * </ul>
 * The files necessary for the mixin plugin to discover the target will be generated at compile time, and discovered via
 * {@link java.util.ServiceLoader} at runtime.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface UnFinal {}
