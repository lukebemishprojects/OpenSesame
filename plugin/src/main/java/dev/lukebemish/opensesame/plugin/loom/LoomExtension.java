package dev.lukebemish.opensesame.plugin.loom;

public abstract class LoomExtension {
    public LoomExtension() {}

    private void checkForLoom() {
        try {
            Class.forName("net.fabricmc.loom.api.LoomGradleExtensionAPI");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("fabric-loom is not present, but expected by opensesame.loom extension");
        }
    }

    public void apply() {
        checkForLoom();
        // TODO: implement
    }
}
