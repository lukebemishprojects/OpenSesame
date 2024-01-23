package dev.lukebemish.opensesame.runtime;

/**
 * A marker interface that extensions will apply at runtime. Used to ensure that no extension interface extends an
 * extension, as doing so leads to situations which cannot be easily figured out.
 */
public interface Extension {
}
