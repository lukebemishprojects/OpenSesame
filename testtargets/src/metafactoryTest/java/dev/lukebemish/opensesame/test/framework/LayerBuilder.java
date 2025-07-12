package dev.lukebemish.opensesame.test.framework;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Nullable;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
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
    
    private final Map<String, StackTraceElement> javaInitialLocations = new HashMap<>();
    
    public <T extends Throwable> void fillStackTrace(T throwable) {
        var stackTrace = new ArrayList<>(List.of(throwable.getStackTrace()));
        var iterator = stackTrace.listIterator();
        while (iterator.hasNext()) {
            var element = iterator.next();
            if (element.getModuleName() != null) {
                var identifyingString = element.getModuleName() + "/" + element.getClassName();
                var initialLocation = javaInitialLocations.get(identifyingString);
                if (initialLocation != null) {
                    iterator.add(initialLocation);
                }
            }
        }
        throwable.setStackTrace(stackTrace.toArray(StackTraceElement[]::new));
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

        paths.addAll(ModuleBuilder.build(modules, working, compileModulePath));
        for (ModuleBuilder moduleBuilder : modules) {
            moduleNames.add(moduleBuilder.name);
        }

        var configuration = parentLayer.configuration().resolveAndBind(
                ModuleFinder.of(),
                ModuleFinder.of(working.resolve("out")),
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
                    var initialLocation = module.javaInitialLocations.get(className);
                    if (initialLocation != null) {
                        this.javaInitialLocations.put(module.name + "/" + className, initialLocation);
                    }
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
                Files.walkFileTree(path, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException {
                        if (exception == null) {
                            Files.delete(directory);
                            return FileVisitResult.CONTINUE;
                        } else {
                            throw exception;
                        }
                    }

                });
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
        private final Map<String, StackTraceElement> javaInitialLocations = new LinkedHashMap<>();
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

        public ModuleBuilder opens(String packageName) {
            this.opens.add(packageName);
            return this;
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
            findLineNumber(className);
            var lastDot = className.lastIndexOf('.');
            var packageName = lastDot == -1 ? "" : className.substring(0, lastDot);
            var simpleName = lastDot == -1 ? className : className.substring(lastDot + 1);
            var source = "package "+packageName+";" +
                    "import dev.lukebemish.opensesame.annotations.*;" +
                    "import dev.lukebemish.opensesame.annotations.extend.*;" +
                    "import org.junit.jupiter.api.*;" +
                    "import static org.junit.jupiter.api.Assertions.*;" +
                    "public class "+simpleName+" {"+contents+"}";
            this.javaSources.put(className, source);
            return this;
        }

        private void findLineNumber(String className) {
            var stackTrace = new RuntimeException().getStackTrace();
            if (stackTrace.length >= 3) {
                this.javaInitialLocations.put(className, stackTrace[2]);
            }
        }

        public ModuleBuilder java(
                String className,
                @Language(
                        value = "JAVA"
                )
                String contents
        ) {
            findLineNumber(className);
            var lastDot = className.lastIndexOf('.');
            var packageName = lastDot == -1 ? "" : className.substring(0, lastDot);
            var source = "package "+packageName+";" + contents;
            this.javaSources.put(className, source);
            return this;
        }
        
        static List<Path> build(List<ModuleBuilder> modules, Path working, List<Path> compileModulePath) throws IOException {
            var paths = new ArrayList<Path>();
            var compiler = ToolProvider.getSystemJavaCompiler();

            List<String> options = new ArrayList<>();
            options.add("-Xplugin:dev.lukebemish.javac-post-processor dev.lukebemish.opensesame");

            var diagnostics = new DiagnosticCollector<>();
            var manager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8);

            var files = new ArrayList<JavaFileObject>();
            for (var module : modules) {
                var modulePath = working.resolve("out").resolve(module.name);
                paths.add(modulePath);
                Files.createDirectories(modulePath);
                StringBuilder moduleInfoBuilder = new StringBuilder();
                if (module.open) {
                    moduleInfoBuilder.append("open ");
                }
                moduleInfoBuilder.append("module ").append(module.name).append(" {\n");
                for (String require : module.requires) {
                    moduleInfoBuilder.append("    requires ").append(require).append(";\n");
                }
                for (String export : module.exports) {
                    moduleInfoBuilder.append("    exports ").append(export).append(";\n");
                }
                for (String open : module.opens) {
                    moduleInfoBuilder.append("    opens ").append(open).append(";\n");
                }
                moduleInfoBuilder.append("}\n");
                String moduleInfo = moduleInfoBuilder.toString();

                Files.createDirectories(working.resolve("src").resolve(module.name));
                manager.setLocationForModule(StandardLocation.MODULE_SOURCE_PATH, module.name, List.of(working.resolve("src").resolve(module.name)));
                
                var fullSources = new HashMap<>(module.javaSources);
                fullSources.put("module-info", moduleInfo);
                
                for (var entry : fullSources.entrySet()) {
                    var sourcePath = working.resolve("src").resolve(module.name).resolve(entry.getKey().replace('.', '/')+".java");
                    Files.createDirectories(sourcePath.getParent());
                    Files.writeString(sourcePath, entry.getValue());
                    var location = manager.getLocationForModule(StandardLocation.MODULE_SOURCE_PATH, module.name);
                    files.add(manager.getJavaFileForInput(
                            location,
                            entry.getKey(),
                            JavaFileObject.Kind.SOURCE
                    ));
                }

                manager.setLocationForModule(StandardLocation.CLASS_OUTPUT, module.name, List.of(modulePath));
            }

            manager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(working.resolve("out")));
            manager.setLocationFromPaths(StandardLocation.MODULE_PATH, compileModulePath);

            var task = compiler.getTask(null, manager, diagnostics, options, null, files);
            for (var module : modules) {
                task.addModules(List.of(module.name));
            }
            if (!task.call()) {
                var firstError = diagnostics.getDiagnostics().stream().filter(d -> d.getKind() == Diagnostic.Kind.ERROR).findFirst();
                firstError.ifPresent(error -> {
                    throw new RuntimeException("Failed to compile: " + error.getMessage(Locale.ROOT));
                });
                throw new RuntimeException("Failed to compile");
            }

            for (var module : modules){
                for (var entry : module.resources.entrySet()) {
                    var resourcePath = working.resolve("out").resolve(module.name).resolve(entry.getKey());
                    Files.createDirectories(resourcePath.getParent());
                    Files.write(resourcePath, entry.getValue());
                }
            }
            return paths;
        }
    }
}
