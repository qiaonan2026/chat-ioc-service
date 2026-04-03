package com.chat.ioc;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IOC Container implementation
 * Advanced dependency injection container for managing object lifecycle
 */
public class IOCContainer {
    private final Map<String, Class<?>> registry;
    private final Map<String, Object> singletonInstances;
    private final Map<String, Boolean> singletonScopes;
    private final ThreadLocal<Set<String>> currentCreationStack = new ThreadLocal<>();

    public IOCContainer() {
        this.registry = new HashMap<>();
        this.singletonInstances = new ConcurrentHashMap<>();
        this.singletonScopes = new HashMap<>();
    }

    /**
     * Register a class in the container as a singleton (default)
     * @param name the bean name
     * @param clazz the class to register
     */
    public void register(String name, Class<?> clazz) {
        register(name, clazz, true); // Default to singleton
    }

    /**
     * Register a class in the container with scope specification
     * @param name the bean name
     * @param clazz the class to register
     * @param singleton whether the bean should be a singleton
     */
    public void register(String name, Class<?> clazz, boolean singleton) {
        registry.put(name, clazz);
        singletonScopes.put(name, singleton);
    }

    /**
     * Get a bean instance by name
     * @param name the bean name
     * @return the bean instance
     */
    public Object getBean(String name) {
        if (!registry.containsKey(name)) {
            throw new RuntimeException("No bean named '" + name + "' is defined");
        }

        Class<?> clazz = registry.get(name);
        boolean isSingleton = singletonScopes.getOrDefault(name, true);

        // If it's a singleton and already instantiated, return cached instance
        if (isSingleton && singletonInstances.containsKey(name)) {
            return singletonInstances.get(name);
        }

        // Check for circular dependencies
        Set<String> creationStack = currentCreationStack.get();
        if (creationStack == null) {
            creationStack = new HashSet<>();
            currentCreationStack.set(creationStack);
        }

        if (creationStack.contains(name)) {
            throw new RuntimeException("Circular dependency detected for bean: " + name);
        }

        // Add to creation stack
        creationStack.add(name);
        try {
            // Create new instance with dependency injection
            Object instance = createInstanceWithDependencies(clazz);

            // Initialize the bean if it has an init method
            initializeBean(instance);

            // Cache singleton instance
            if (isSingleton) {
                singletonInstances.put(name, instance);
            }

            return instance;
        } finally {
            // Remove from creation stack
            creationStack.remove(name);
            if (creationStack.isEmpty()) {
                currentCreationStack.remove();
            }
        }
    }
    
    /**
     * Initialize a bean by calling its initialization methods
     * @param instance the bean instance to initialize
     */
    private void initializeBean(Object instance) {
        Class<?> clazz = instance.getClass();
        try {
            // Look for an 'init' method
            try {
                var initMethod = clazz.getMethod("init");
                initMethod.invoke(instance);
            } catch (NoSuchMethodException e) {
                // If no 'init' method exists, that's fine
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize bean: " + e.getMessage(), e);
        }
    }

    /**
     * Creates an instance with its dependencies injected
     * @param clazz the class to instantiate
     * @return the created instance with dependencies injected
     */
    private Object createInstanceWithDependencies(Class<?> clazz) {
        try {
            // Find constructor with the most parameters to inject dependencies
            Constructor<?>[] constructors = clazz.getConstructors();
            Constructor<?> chosenConstructor = null;
            int maxParamCount = -1;

            for (Constructor<?> constructor : constructors) {
                if (constructor.getParameterCount() > maxParamCount) {
                    maxParamCount = constructor.getParameterCount();
                    chosenConstructor = constructor;
                }
            }

            // If no constructor with parameters, use default constructor
            if (chosenConstructor == null || chosenConstructor.getParameterCount() == 0) {
                Object instance = clazz.getDeclaredConstructor().newInstance();
                injectFieldDependencies(instance);
                return instance;
            }

            // Resolve constructor parameters as beans
            Object[] params = new Object[chosenConstructor.getParameterCount()];
            Class<?>[] paramTypes = chosenConstructor.getParameterTypes();

            for (int i = 0; i < paramTypes.length; i++) {
                // Try to find a bean of the required type
                String paramName = findBeanNameByType(paramTypes[i]);
                if (paramName != null) {
                    params[i] = getBean(paramName);
                } else {
                    // If no matching bean found, try to create one if it's a known type
                    throw new RuntimeException("Could not find suitable bean for type: " + paramTypes[i].getName());
                }
            }

            Object instance = chosenConstructor.newInstance(params);
            injectFieldDependencies(instance);
            return instance;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of " + clazz.getName() + 
                                     " with dependency injection", e);
        }
    }

    /**
     * Find a bean name by its type
     * @param type the type to look for
     * @return the first matching bean name or null if not found
     */
    private String findBeanNameByType(Class<?> type) {
        for (Map.Entry<String, Class<?>> entry : registry.entrySet()) {
            if (type.isAssignableFrom(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Inject dependencies through fields
     * @param instance the instance to inject dependencies into
     */
    private void injectFieldDependencies(Object instance) {
        Class<?> clazz = instance.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Autowired.class)) {
                field.setAccessible(true);
                try {
                    // Try to find by type first (more reliable)
                    String beanName = findBeanNameByType(field.getType());
                    if (beanName != null) {
                        Object dependency = getBean(beanName);
                        field.set(instance, dependency);
                    } else {
                        // Fallback to finding by field name
                        String fieldName = field.getName();
                        if (registry.containsKey(fieldName)) {
                            Object dependency = getBean(fieldName);
                            field.set(instance, dependency);
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Could not inject dependency into field: " + field.getName(), e);
                } catch (Exception e) {
                    // Handle any other exceptions during injection
                    throw new RuntimeException("Error injecting dependency into field: " + field.getName(), e);
                }
            }
        }
    }

    /**
     * Check if a bean is registered
     * @param name the bean name
     * @return true if registered
     */
    public boolean containsBean(String name) {
        return registry.containsKey(name);
    }

    /**
     * Get the type of a bean
     * @param name the bean name
     * @return the class type
     */
    public Class<?> getType(String name) {
        return registry.get(name);
    }

    /**
     * Check if a bean is a singleton
     * @param name the bean name
     * @return true if singleton
     */
    public boolean isSingleton(String name) {
        return singletonScopes.getOrDefault(name, true);
    }
}