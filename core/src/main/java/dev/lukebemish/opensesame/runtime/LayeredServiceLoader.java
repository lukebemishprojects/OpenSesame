package dev.lukebemish.opensesame.runtime;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.WeakHashMap;

final class LayeredServiceLoader<T> {
    private final Class<T> service;

    private final WeakHashMap<ClassLoader, WeakReference<SingleImplementation<T>>> cache = new WeakHashMap<>();
    private final ClassValue<SingleImplementation<T>> providersValue = new ClassValue<>() {
        @Override
        protected SingleImplementation<T> computeValue(Class<?> type) {
            var existingReference = cache.get(type.getClassLoader());
            if (existingReference != null) {
                var existing = existingReference.get();
                if (existing != null) {
                    return existing;
                }
            }
            var singleImplementation = new SingleImplementation<T>();
            if (type.getModule().getLayer() == null) {
                ServiceLoader.load(service, type.getClassLoader()).stream().forEach(provider -> singleImplementation.implementations.put(provider.getClass(), provider));
            } else {
                ServiceLoader.load(type.getModule().getLayer(), service).stream().forEach(provider -> singleImplementation.implementations.put(provider.getClass(), provider));
            }
            cache.put(type.getClassLoader(), new WeakReference<>(singleImplementation));
            return singleImplementation;
        }
    };

    private LayeredServiceLoader(Class<T> service) {
        this.service = service;
    }

    public SingleImplementation<T> at(Class<?> type) {
        return providersValue.get(type);
    }

    @SafeVarargs
    public static <T> List<T> unique(SingleImplementation<T>... implementations) {
        var out = new LinkedHashMap<Class<?>, T>();
        for (var impl : implementations) {
            for (var entry : impl.implementations.entrySet()) {
                out.putIfAbsent(entry.getKey(), entry.getValue().get());
            }
        }
        return List.copyOf(out.values());
    }

    public static <T> LayeredServiceLoader<T> of(Class<T> service) {
        return new LayeredServiceLoader<>(service);
    }

    public static final class SingleImplementation<T> {
        private final Map<Class<?>, ServiceLoader.Provider<T>> implementations = new LinkedHashMap<>();
    }
}
