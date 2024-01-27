/**
 * Tools for accessing methods, constructors, and fields over encapsulation boundaries.
 * <p>The tools in this package let you access methods, fields, and constructors which are package-private or private,
 * using type-safe, performant accessors. The core annotation is {@link dev.lukebemish.opensesame.annotations.Open};
 * at compile time, the body of any method annotated with {@link dev.lukebemish.opensesame.annotations.Open} will be
 * replaced with an INVOKEDYNAMIC call to {@link dev.lukebemish.opensesame.runtime.OpeningMetafactory}, which builds a
 * call site for the target operation.
 * <p>The signature of the method is used to target the correct method, constructor, or
 * field; if a member of that signature needs to represent a type that is inaccessible,
 * {@link dev.lukebemish.opensesame.annotations.Coerce} may be used to coerce the type of a parameter or return type.
 */
package dev.lukebemish.opensesame.annotations;