package dev.lukebemish.opensesame.runtime;

import org.jetbrains.annotations.Nullable;

/**
 * A service used to provide remapping of method and field names to {@link OpeningMetafactory}.
 */
public interface RuntimeRemapper {
    /**
     * Remap a method name.
     * @param parent the name (as in {@link Class#getName()} of the class that contains the method
     * @param name the name of the method
     * @param methodDesc the descriptor of the method
     * @return the new name of the method, or {@code null} to use the original name
     */
    @Nullable String remapMethodName(String parent, String name, String methodDesc);

    /**
     * Remap a field name.
     * @param parent the name (as in {@link Class#getName()} of the class that contains the field
     * @param name the name of the field
     * @param descriptor the descriptor of the field
     * @return the new name of the field, or {@code null} to use the original name
     */
    @Nullable String remapFieldName(String parent, String name, String descriptor);

    /**
     * Remap a class internal name
     * @param className the internal name of the class
     * @return the new name of the class, or {@code null} to use the original name
     */
    @Nullable String remapClassName(String className);
}
