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
 * <p>As an example of use, consider the following package private class:
 * <blockquote><pre>{@code
 * package xyz;
 *
 * class Foo {
 *     private Foo(int n) {
 *         ...
 *     }
 *
 *     void fizz() {
 *         ...
 *     }
 * }
 * }</pre></blockquote>
 * We want to create a class that extends {@code Foo}, stores some new field {@code x}, and overrides {@code fizz}. To
 * do so, we create the following interface:
 * <blockquote><pre>{@code
 * package abc;
 *
 * @Extend(targetClass = "xyz.Foo", unsafe = true)
 * public interface Bar {
 *     @Constructor
 *     static Bar create(@Field("x") int x, int n) {
 *         throw new UnsupportedOperationException();
 *     }
 *
 *     @Field("x")
 *     int getX();
 *
 *     @Field("x")
 *     void setX(int x);
 *
 *     @Overrides("fizz")
 *     default void buzz() {
 *         ...
 *     }
 * }
 * }</pre></blockquote>
 * Note that this interface is public, so that it will be visible from the nest of {@code Foo}. The
 * {@link dev.lukebemish.opensesame.annotations.extend.Extend} annotation specifies the target class, and that the hidden
 * class should be created unsafely, even if the target is in another module. The
 * {@link dev.lukebemish.opensesame.annotations.extend.Constructor} has its body replaced at compile time, and allows
 * you to retrieve instances of your new subclass. When the constructor is first invoked, a hidden class will be created
 * within the nest of {@code Foo}, with constructors and fields based on the annotated methods in {@code Bar}, and overriding
 * methods of {@code Foo} based on the {@link dev.lukebemish.opensesame.annotations.extend.Overrides} methods in {@code Bar}.
 * The constructor method will then create an instance of this hidden class on invocation.
 */
package dev.lukebemish.opensesame.annotations.extend;