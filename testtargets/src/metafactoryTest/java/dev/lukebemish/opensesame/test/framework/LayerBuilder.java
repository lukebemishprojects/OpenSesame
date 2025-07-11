package dev.lukebemish.opensesame.test.framework;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Nullable;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

public final class LayerBuilder {
    private final List<ModuleBuilder> modules = new ArrayList<>();
    private final @Nullable LayerBuilder parent;
    private final List<Path> paths = new ArrayList<>();

    private LayerBuilder(@Nullable LayerBuilder parent) {
        this.parent = parent;
    }
    
    public static LayerBuilder create() {
        return new LayerBuilder(null);
    }
    
    public LayerBuilder withModule(String moduleName, Consumer<ModuleBuilder> builder) {
        var moduleBuilder = new ModuleBuilder(moduleName);
        builder.accept(moduleBuilder);
        this.modules.add(moduleBuilder);
        return this;
    }
    
    public LayerBuilder child() {
        return new LayerBuilder(this);
    }
    
    record LayerInfo(LayerBuilder builder, ModuleLayer layer, List<Class<?>> classes, ClassLoader loader) implements AutoCloseable {
        @Override
        public void close() throws Exception {
            builder.close();
        }
    }
    
    private Stream<Path> getPaths() {
        if (parent == null) {
            var value = System.getProperty("jdk.module.path");
            return Stream.concat(Arrays.stream(value.split(File.pathSeparator)).filter(s -> !s.isEmpty()).map(Paths::get), paths.stream());
        } else {
            return Stream.concat(parent.getPaths(), paths.stream());
        }
    }
    
    synchronized LayerInfo build(Path working) throws IOException {
        var parentInfo = parent != null ? parent.build(working.resolve("parent")) : new LayerInfo(
                null,
                LayerBuilder.class.getModule().getLayer(),
                List.of(),
                LayerBuilder.class.getClassLoader()
        );
        var parentLayer = parentInfo.layer();
        if (!paths.isEmpty()) {
            throw new IllegalStateException("LayerBuilder can only be used once");
        }
        
        var moduleNames = new ArrayList<String>();

        List<Path> compileModulePath = getPaths().toList();

        for (ModuleBuilder moduleBuilder : modules) {
            var path = moduleBuilder.build(working, parentLayer, compileModulePath);
            moduleNames.add(moduleBuilder.name);
            paths.add(path);
        }
        
        var configuration = parentLayer.configuration().resolveAndBind(
                ModuleFinder.of(),
                ModuleFinder.of(working),
                moduleNames
        );
        var controller = ModuleLayer.defineModulesWithOneLoader(
                configuration,
                List.of(parentLayer),
                parentInfo.loader()
        );
        var layer = controller.layer();
        var classLoader = modules.isEmpty() ? parentInfo.loader() : layer.findLoader(modules.get(0).name);
        var classes = new ArrayList<Class<?>>();
        for (var module : modules) {
            var source = controller.layer().findModule(module.name).orElseThrow();
            var target = LayerBuilder.class.getModule();
            for (var className : module.javaSources.keySet()) {
                try {
                    var clazz = Class.forName(className, false, classLoader);
                    classes.add(clazz);
                    controller.addOpens(source, clazz.getPackageName(), target);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
            target.addReads(source);
        }
        return new LayerInfo(
                this,
                layer,
                classes,
                classLoader
        );
    }

    private void close() throws IOException {
        var pending = new ArrayList<IOException>();
        for (var path : paths) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                pending.add(e);
            }
        }
        try {
            if (parent != null) {
                parent.close();
            }
        } catch (IOException e) {
            pending.add(e);
        }
        if (!pending.isEmpty()) {
            if (pending.size() == 1) {
                throw pending.get(0);
            }
            var exception = new IOException("Errors occurred while closing LayerBuilder");
            for (var e : pending) {
                exception.addSuppressed(e);
            }
            throw exception;
        }
    }

    public static final class ModuleBuilder {
        private final List<String> requires = new ArrayList<>(List.of(
                "dev.lukebemish.opensesame.core",
                "org.junit.jupiter.api"
        ));
        private boolean open = false;
        private final List<String> exports = new ArrayList<>();
        private final List<String> opens = new ArrayList<>();
        private final Map<String, String> javaSources = new LinkedHashMap<>();
        private final Map<String, byte[]> resources = new LinkedHashMap<>();
        private final String name;

        public ModuleBuilder(String name) {
            this.name = name;
        }

        public ModuleBuilder requires(String moduleName) {
            this.requires.add(moduleName);
            return this;
        }
        
        public ModuleBuilder open() {
            this.open = true;
            return this;
        }
        
        public ModuleBuilder exports(String packageName) {
            this.exports.add(packageName);
            return this;
        }

        public void opens(String packageName) {
            this.opens.add(packageName);
        }
        
        public ModuleBuilder resource(String name, byte[] content) {
            this.resources.put(name, content);
            return this;
        }

        public ModuleBuilder test(
                String className,
                @Language(
                        value = "JAVA",
                        prefix = "import dev.lukebemish.opensesame.annotations.*;import dev.lukebemish.opensesame.annotations.extend.*;import org.junit.jupiter.api.*;import static org.junit.jupiter.api.Assertions.*; public class X {",
                        suffix = "}"
                )
                String contents
        ) {
            var lastDot = className.lastIndexOf('.');
            var packageName = lastDot == -1 ? "" : className.substring(0, lastDot);
            var simpleName = lastDot == -1 ? className : className.substring(lastDot + 1);
            var source = "package "+packageName+";\n" +
                    "import dev.lukebemish.opensesame.annotations.*;\n" +
                    "import dev.lukebemish.opensesame.annotations.extend.*;\n" +
                    "import org.junit.jupiter.api.*;\n" +
                    "import static org.junit.jupiter.api.Assertions.*;\n" +
                    "public class "+simpleName+" {\n"+contents+"}";
            this.javaSources.put(className, source);
            return this;
        }

        public ModuleBuilder java(
                String className,
                @Language(
                        value = "JAVA"
                )
                String contents
        ) {
            var lastDot = className.lastIndexOf('.');
            var packageName = lastDot == -1 ? "" : className.substring(0, lastDot);
            var source = "package "+packageName+";\n" + contents;
            this.javaSources.put(className, source);
            return this;
        }
        
        Path build(Path working, ModuleLayer parent, List<Path> compileModulePath) throws IOException {
            Files.createDirectories(working);
            var modulePath = working.resolve(name + ".jar");
            //noinspection EmptyTryBlock
            if (Files.exists(modulePath)) {
                Files.delete(modulePath);
            }
            try (
                    var os = Files.newOutputStream(modulePath);
                    var ignored = new JarOutputStream(os)
            ) {}
            StringBuilder moduleInfoBuilder = new StringBuilder();
            if (open) {
                moduleInfoBuilder.append("open ");
            }
            moduleInfoBuilder.append("module ").append(name).append(" {\n");
            for (String require : requires) {
                moduleInfoBuilder.append("    requires ").append(require).append(";\n");
            }
            for (String export : exports) {
                moduleInfoBuilder.append("    exports ").append(export).append(";\n");
            }
            for (String open : opens) {
                moduleInfoBuilder.append("    opens ").append(open).append(";\n");
            }
            moduleInfoBuilder.append("}\n");
            String moduleInfo = moduleInfoBuilder.toString();
            
            try (var jarFileSystem = FileSystems.newFileSystem(modulePath)) {
                var compiler = ToolProvider.getSystemJavaCompiler();
                List<String> options = new ArrayList<>();
                options.add("-proc:none");

                var files = new ArrayList<JavaFileObject>();
                
                files.add(new JavaSourceFromString("module-info", moduleInfo));
                for (var entry : javaSources.entrySet()) {
                    String className = entry.getKey();
                    String sourceCode = entry.getValue();
                    files.add(new JavaSourceFromString(className, sourceCode));
                }
                
                var diagnostics = new DiagnosticCollector<>();
                var manager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8);

                manager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(jarFileSystem.getPath("/")));
                manager.setLocationFromPaths(StandardLocation.MODULE_PATH, compileModulePath);
                
                var task = compiler.getTask(null, manager, diagnostics, options, null, files);
                if (!task.call()) {
                    var firstError = diagnostics.getDiagnostics().stream().filter(d -> d.getKind() == Diagnostic.Kind.ERROR).findFirst();
                    firstError.ifPresent(error -> {
                        throw new RuntimeException("Failed to compile: " + error.getMessage(Locale.ROOT));
                    });
                    throw new RuntimeException("Failed to compile");
                }
                
                for (var entry : resources.entrySet()) {
                    var resourcePath = jarFileSystem.getPath("/" + entry.getKey());
                    Files.createDirectories(resourcePath.getParent());
                    Files.write(resourcePath, entry.getValue());
                }
            }
            return modulePath;
        }

        public static class JavaSourceFromString extends SimpleJavaFileObject {
            private final String sourceCode;

            public JavaSourceFromString(String name, String sourceCode) {
                super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
                this.sourceCode = sourceCode;
            }

            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return sourceCode;
            }
        }
    }
}
