/**
 * Tools for extending classes over encapsulation boundaries.
 * <p>The tools in this package let you extend classes which are package-private or private, through the use of hidden
 * classes, created at runtime in the nest of the class you are extending. The core annotation is
 * {@link dev.lukebemish.opensesame.annotations.extend.Extend}, which when applied to an interface marks it as an
 * "extension" interface. You should not implement an extension interface directly, and extension interfaces cannot
 * extend other extension interfaces. Extension interfaces allow you to subclass classes or implement interfaces.
 * <p>An extension interface will be implemented by the hidden class generated at runtime - thus, it should be public.
 * To actually create instances of your subclass, you should use the {@link dev.lukebemish.opensesame.annotations.extend.Constructor}
 * annotation on a static method in your extension interface, whose contents will be replaced. The method should have
 * the same signature as the constructor of the class you are extending, and should return an instance of your extension.
 * If the constructor takes inaccessible types, {@link dev.lukebemish.opensesame.annotations.Coerce} may be used.
 * Optionally, the constructor may take field types as parameters, which will be set after the super constructor is
 * called - for more information, see {@link dev.lukebemish.opensesame.annotations.extend.Field}. Superclass methods
 * may be overridden with {@link dev.lukebemish.opensesame.annotations.extend.Overrides}.
 */
package dev.lukebemish.opensesame.annotations.extend;