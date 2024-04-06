package dev.lukebemish.opensesame.mixin.plugin;

import java.util.Arrays;

@SuppressWarnings("DeprecatedIsStillUsed")
@Deprecated(forRemoval = true)
public interface UnFinalLineProvider {
    String[] lines();

    default OpenSesameMixinProvider makeToOpen() {
        return new OpenSesameMixinProvider() {
            @Override
            public String[] unFinal() {
                return Arrays.stream(lines()).map(line -> {
                    String[] parts = line.split(" ");
                    parts[0] = parts[0].replace(".", "/");
                    parts[1] = parts[1].replace(".", "/");
                    return String.join(".", parts);
                }).toArray(String[]::new);
            }
        };
    }
}
