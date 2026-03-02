package dev.lukebemish.opensesame.runtime;

import org.jetbrains.annotations.ApiStatus;

/**
 * A marker interface that extensions will apply at runtime. Used to ensure that no extension interface extends an
 * extension, as doing so leads to situations which cannot be easily figured out.
 */
@ApiStatus.Internal
public interface Extension {
}
