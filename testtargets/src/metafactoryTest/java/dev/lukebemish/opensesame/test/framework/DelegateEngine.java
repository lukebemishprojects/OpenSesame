package dev.lukebemish.opensesame.test.framework;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.ClasspathRootSelector;
import org.junit.platform.engine.discovery.ModuleSelector;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.support.descriptor.MethodSource;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

public class DelegateEngine implements TestEngine {
    @Override
    public String getId() {
        return "opensesame-layered-framework";
    }

    private static final class ClassTestDescriptor extends AbstractTestDescriptor {
        private final Class<?> clazz;

        private ClassTestDescriptor(UniqueId uniqueId, String displayName, Class<?> clazz) {
            super(uniqueId, displayName);
            this.clazz = clazz;
        }

        @Override
        public Type getType() {
            return Type.CONTAINER;
        }

        @Override
        public boolean mayRegisterTests() {
            return true;
        }

        @Override
        public Optional<TestSource> getSource() {
            return Optional.of(ClassSource.from(clazz));
        }
    }

    private static final class LayerMethodDescriptor extends AbstractTestDescriptor {
        private final Method method;

        private LayerMethodDescriptor(UniqueId uniqueId, String displayName, Method method) {
            super(uniqueId, displayName);
            this.method = method;
        }

        @Override
        public Type getType() {
            return Type.CONTAINER;
        }

        @Override
        public boolean mayRegisterTests() {
            return true;
        }

        @Override
        public Optional<TestSource> getSource() {
            return Optional.of(MethodSource.from(method.getDeclaringClass(), method));
        }
    }

    private static final class GeneratedClassDescriptor extends AbstractTestDescriptor {
        private final Class<?> clazz;

        private GeneratedClassDescriptor(UniqueId uniqueId, String displayName, Class<?> clazz) {
            super(uniqueId, displayName);
            this.clazz = clazz;
        }

        @Override
        public Type getType() {
            return Type.CONTAINER;
        }

        @Override
        public Optional<TestSource> getSource() {
            return Optional.of(ClassSource.from(clazz));
        }
    }

    private static final class TestMethodDescriptor extends AbstractTestDescriptor {
        private final Method method;

        private TestMethodDescriptor(UniqueId uniqueId, String displayName, Method method) {
            super(uniqueId, displayName);
            this.method = method;
        }

        @Override
        public Type getType() {
            return Type.TEST;
        }

        @Override
        public Optional<TestSource> getSource() {
            return Optional.of(MethodSource.from(method.getDeclaringClass(), method));
        }
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
        var descriptor = new EngineDescriptor(uniqueId, "Module Layer Tests");

        discoveryRequest.getSelectorsByType(ClasspathRootSelector.class).forEach(selector -> {
            appendTestsInClasspathRoot(selector.getClasspathRoot(), descriptor);
        });

        discoveryRequest.getSelectorsByType(PackageSelector.class).forEach(selector -> {
            appendTestsInPackage(selector.getPackageName(), descriptor);
        });

        discoveryRequest.getSelectorsByType(ClassSelector.class).forEach(selector -> {
            appendTestsInClass(selector.getJavaClass(), descriptor);
        });

        discoveryRequest.getSelectorsByType(ModuleSelector.class).forEach(selector -> {
            appendTestsInModule(selector.getModuleName(), descriptor);
        });
        
        return descriptor;
    }


    private void appendTestsInClasspathRoot(URI uri, TestDescriptor engineDescriptor) {
        ReflectionSupport.findAllClassesInClasspathRoot(uri, c -> true, name -> true)
                .forEach(aClass -> appendTestsInClass(aClass, engineDescriptor));
    }


    private void appendTestsInModule(String moduleName, TestDescriptor engineDescriptor) {
        ReflectionSupport.findAllClassesInModule(moduleName, c -> true, name -> true)
                .forEach(aClass -> appendTestsInClass(aClass, engineDescriptor));
    }

    private void appendTestsInPackage(String packageName, TestDescriptor engineDescriptor) {
        ReflectionSupport.findAllClassesInPackage(packageName, c -> true, name -> true)
                .forEach(aClass -> appendTestsInClass(aClass, engineDescriptor));
    }

    private void appendTestsInClass(Class<?> clazz, TestDescriptor descriptor) {
        var classUniqueId = descriptor.getUniqueId().append("class", clazz.getName());
        var descriptors = new ArrayList<TestDescriptor>();
        AnnotationSupport.findAnnotatedMethods(clazz, LayerTest.class, HierarchyTraversalMode.TOP_DOWN)
                .forEach(method -> {
                    String methodId = String.format("%s(%s)", method.getName(),
                            nullSafeToString(method.getParameterTypes()));
                    var methodUniqueId = classUniqueId.append("method", methodId);
                    descriptors.add(new LayerMethodDescriptor(
                            methodUniqueId,
                            methodId,
                            method
                    ));
                });
        if (!descriptors.isEmpty()) {
            var classDescriptor = new ClassTestDescriptor(classUniqueId, clazz.getSimpleName(), clazz);
            descriptor.addChild(classDescriptor);
            descriptors.forEach(classDescriptor::addChild);
        }
    }

    @Override
    public void execute(ExecutionRequest request) {
        var root = (EngineDescriptor) request.getRootTestDescriptor();
        var listener = request.getEngineExecutionListener();
        var executor = new DelegateExecutor(listener);
        listener.executionStarted(root);
        for (var child : root.getChildren()) {
            executor.execute((ClassTestDescriptor) child);
        }
        listener.executionFinished(root, TestExecutionResult.successful());
    }
    
    private static final class DelegateExecutor {
        private final EngineExecutionListener listener;
        private final Path workingDirectory = Paths.get("opensesame-layered-framework");
        
        public DelegateExecutor(EngineExecutionListener listener) {
            this.listener = listener;
        }

        public void execute(ClassTestDescriptor classTestDescriptor) {
            listener.executionStarted(classTestDescriptor);
            Object instance;
            
            try {
                instance = ReflectionSupport.newInstance(classTestDescriptor.clazz);
            } catch (Throwable t) {
                listener.executionFinished(classTestDescriptor, TestExecutionResult.failed(t));
                return;
            }
            for (var childDescriptor : classTestDescriptor.getChildren()) {
                if (childDescriptor instanceof LayerMethodDescriptor layerMethodDescriptor) {
                    listener.executionStarted(childDescriptor);
                    Throwable throwable = null;
                    try {
                        var layerBuilder = (LayerBuilder) layerMethodDescriptor.method.invoke(instance);
                        var path = workingDirectory.resolve(classTestDescriptor.clazz.getName()).resolve(layerMethodDescriptor.method.getName());
                        try (var info = layerBuilder.build(path)) {
                            for (var clazz : info.classes()) {
                                var annotated = AnnotationSupport.findAnnotatedMethods(clazz, Test.class, HierarchyTraversalMode.TOP_DOWN);
                                if (annotated.isEmpty()) {
                                    continue;
                                }
                                var classDescriptor = new GeneratedClassDescriptor(
                                        childDescriptor.getUniqueId().append("class", clazz.getName()),
                                        clazz.getSimpleName(),
                                        clazz
                                );
                                childDescriptor.addChild(classDescriptor);
                                listener.dynamicTestRegistered(classDescriptor);
                                var tests = new ArrayList<TestMethodDescriptor>();
                                for (var method : annotated) {
                                    String methodId = String.format("%s(%s)", method.getName(),
                                            nullSafeToString(method.getParameterTypes()));
                                    var methodDescriptor = new TestMethodDescriptor(
                                            classDescriptor.getUniqueId().append("method", methodId),
                                            methodId,
                                            method
                                    );
                                    classDescriptor.addChild(methodDescriptor);
                                    tests.add(methodDescriptor);
                                    listener.dynamicTestRegistered(methodDescriptor);
                                }
                                listener.executionStarted(classDescriptor);
                                try {
                                    var lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
                                    Object innerInstance = lookup.findConstructor(clazz, MethodType.methodType(void.class)).invoke();
                                    for (var test : tests) {
                                        listener.executionStarted(test);
                                        try {
                                            lookup.unreflect(test.method).invoke(innerInstance);
                                        } catch (Throwable t) {
                                            info.builder().fillStackTrace(t);
                                            listener.executionFinished(test, TestExecutionResult.failed(t));
                                            continue;
                                        }
                                        listener.executionFinished(test, TestExecutionResult.successful());
                                    }
                                } catch (Throwable t) {
                                    info.builder().fillStackTrace(t);
                                    listener.executionFinished(classDescriptor, TestExecutionResult.failed(t));
                                    continue;
                                }
                                listener.executionFinished(classDescriptor, TestExecutionResult.successful());
                            }
                        }
                    } catch (Throwable t) {
                        throwable = t;
                    }
                    if (throwable != null) {
                        listener.executionFinished(childDescriptor, TestExecutionResult.failed(throwable));
                    } else {
                        listener.executionFinished(childDescriptor, TestExecutionResult.successful());
                    }
                }
            }
            listener.executionFinished(classTestDescriptor, TestExecutionResult.successful());
        }
    }

    private static String nullSafeToString(Class<?>... classes) {
        if (classes == null || classes.length == 0) {
            return "";
        }
        return stream(classes).map(clazz -> clazz == null ? "null" : clazz.getName()).collect(joining(", "));
    }
}
