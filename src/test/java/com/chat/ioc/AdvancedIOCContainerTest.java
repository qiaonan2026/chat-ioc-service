package com.chat.ioc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive IOC Container Tests - Following TDD approach
 * Testing advanced features of the IOC container
 */
public class AdvancedIOCContainerTest {

    private IOCContainer container;

    @BeforeEach
    void setUp() {
        container = new IOCContainer();
    }

    @Test
    void testDependencyInjection() {
        // Given: A container with services registered
        container.register("notificationService", NotificationService.class);
        container.register("userService", UserService.class);
        
        // When: Getting a service that depends on others
        Object userServiceObj = container.getBean("userService");
        
        // Then: The dependency should be properly injected
        assertNotNull(userServiceObj);
        assertTrue(userServiceObj instanceof UserService);
        
        UserService userService = (UserService) userServiceObj;
        assertNotNull(userService.getNotificationService());
    }
    
    @Test
    void testSimpleBeanCreation() {
        // Given: A simple service without dependencies
        container.register("simpleService", SimpleService.class);
        
        // When: Getting the bean
        Object service = container.getBean("simpleService");
        
        // Then: The bean should be created successfully
        assertNotNull(service);
        assertTrue(service instanceof SimpleService);
    }
    
    @Test
    void testPrototypeScope() {
        // Given: A container with prototype scoped beans
        container.register("transientService", TransientService.class, false); // Not singleton
        
        // When: Getting the same bean twice
        Object bean1 = container.getBean("transientService");
        Object bean2 = container.getBean("transientService");
        
        // Then: Each call should return a new instance
        assertNotNull(bean1);
        assertNotNull(bean2);
        assertNotSame(bean1, bean2);
    }
    
    @Test
    void testSingletonScope() {
        // Given: A container with singleton scoped beans (default)
        container.register("singletonService", SingletonService.class); // Singleton by default
        
        // When: Getting the same bean twice
        Object bean1 = container.getBean("singletonService");
        Object bean2 = container.getBean("singletonService");
        
        // Then: Both calls should return the same instance
        assertNotNull(bean1);
        assertNotNull(bean2);
        assertSame(bean1, bean2);
    }
    
    @Test
    void testBeanInitialization() {
        // Given: A service with initialization method
        container.register("initService", InitService.class);
        
        // When: Getting the bean
        Object service = container.getBean("initService");
        
        // Then: Initialization should be completed
        assertNotNull(service);
        assertTrue(((InitService) service).isInitialized());
    }
    
    @Test
    void testTypeSafety() {
        // Given: A container with registered beans
        container.register("stringService", StringService.class);
        container.register("integerService", IntegerService.class);
        
        // When: Getting the types
        Class<?> stringType = container.getType("stringService");
        Class<?> integerType = container.getType("integerService");
        
        // Then: Types should match expected classes
        assertEquals(StringService.class, stringType);
        assertEquals(IntegerService.class, integerType);
    }
    
    // Supporting classes for testing
    
    static class NotificationService {
        public void sendNotification(String message) {
            System.out.println("Notification: " + message);
        }
    }
    
    static class UserService {
        @Autowired
        private NotificationService notificationService;
        
        public NotificationService getNotificationService() {
            return notificationService;
        }
    }
    
    static class ServiceA {
        @Autowired
        private ServiceB serviceB;
        
        public ServiceB getServiceB() {
            return serviceB;
        }
    }
    
    static class ServiceB {
        @Autowired
        private ServiceA serviceA;
        
        public ServiceA getServiceA() {
            return serviceA;
        }
    }
    
    static class SimpleService {
        // Simple service with no dependencies
    }
    
    static class TransientService {
        private final String id = java.util.UUID.randomUUID().toString();
        
        public String getId() {
            return id;
        }
    }
    
    static class SingletonService {
        private final String id = java.util.UUID.randomUUID().toString();
        
        public String getId() {
            return id;
        }
    }
    
    static class InitService {
        private boolean initialized = false;
        
        public void init() {
            this.initialized = true;
        }
        
        public boolean isInitialized() {
            return initialized;
        }
    }
    
    static class StringService {}
    static class IntegerService {}
}