package dev.lukebemish.opensesame.test.java.loom;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.jar.Manifest;

class FakeKnot extends FabricLauncherBase {
    private static volatile boolean INITIALIZED = false;
    synchronized static void init() {
        if (!INITIALIZED){
            INITIALIZED = true;
            new FakeKnot();
        }
    }

    @Override
    public void addToClassPath(Path path, String... allowedPrefixes) {}

    @Override
    public void setAllowedPrefixes(Path path, String... prefixes) {}

    @Override
    public void setValidParentClassPath(Collection<Path> paths) {}

    @Override
    public EnvType getEnvironmentType() {
        return EnvType.SERVER;
    }

    @Override
    public boolean isClassLoaded(String name) {
        return false;
    }

    @Override
    public Class<?> loadIntoTarget(String name) throws ClassNotFoundException {
        throw new ClassNotFoundException("FakeKnot is not Knot");
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return null;
    }

    @Override
    public ClassLoader getTargetClassLoader() {
        return null;
    }

    @Override
    public byte[] getClassByteArray(String name, boolean runTransformers) {
        return new byte[0];
    }

    @Override
    public Manifest getManifest(Path originPath) {
        return null;
    }

    @Override
    public boolean isDevelopment() {
        return true;
    }

    @Override
    public String getEntrypoint() {
        return null;
    }

    @Override
    public String getTargetNamespace() {
        return "named";
    }

    @Override
    public List<Path> getClassPath() {
        return List.of();
    }
}
