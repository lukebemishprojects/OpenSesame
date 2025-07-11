package dev.lukebemish.opensesame.test.transformation.java;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

@DisplayNameGeneration(TransformerAwareTest.NameGenerator.class)
public interface TransformerAwareTest {
    class NameGenerator implements DisplayNameGenerator {
        private static final String TRANSFORMER_NAME = Objects.requireNonNull(System.getProperty("opensesame.transformer.name"));
        private static final DisplayNameGenerator DELEGATE = DisplayNameGenerator.getDisplayNameGenerator(Standard.class);
        
        @Override
        public String generateDisplayNameForClass(Class<?> aClass) {
            return DELEGATE.generateDisplayNameForClass(aClass) + " (" + TRANSFORMER_NAME + ")";
        }

        @Override
        public String generateDisplayNameForMethod(List<Class<?>> enclosingInstanceTypes, Class<?> testClass, Method testMethod) {
            return DELEGATE.generateDisplayNameForMethod(enclosingInstanceTypes, testClass, testMethod);
        }

        @Override
        public String generateDisplayNameForNestedClass(List<Class<?>> enclosingInstanceTypes, Class<?> nestedClass) {
            return DELEGATE.generateDisplayNameForNestedClass(enclosingInstanceTypes, nestedClass);
        }
    }
}
