package com.chat.ioc;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to verify basic IOC container functionality
 */
public class SimpleIOCContainerTest {

    @Test
    public void testBasicBeanRegistrationAndRetrieval() {
        // Given: An IOC container
        IOCContainer container = new IOCContainer();
        
        // When: Registering a simple bean
        container.register("simpleBean", SimpleBean.class);
        
        // Then: Should be able to retrieve it
        Object bean = container.getBean("simpleBean");
        assertNotNull(bean);
        assertTrue(bean instanceof SimpleBean);
    }
    
    @Test
    public void testSingletonBehavior() {
        // Given: An IOC container with a singleton bean
        IOCContainer container = new IOCContainer();
        container.register("singletonBean", SimpleBean.class);
        
        // When: Getting the same bean twice
        Object bean1 = container.getBean("singletonBean");
        Object bean2 = container.getBean("singletonBean");
        
        // Then: Both should be the same instance
        assertSame(bean1, bean2);
    }
    
    @Test
    public void testPrototypeBehavior() {
        // Given: An IOC container with a prototype bean
        IOCContainer container = new IOCContainer();
        container.register("prototypeBean", SimpleBean.class, false); // Not singleton
        
        // When: Getting the same bean twice
        Object bean1 = container.getBean("prototypeBean");
        Object bean2 = container.getBean("prototypeBean");
        
        // Then: Both should be different instances
        assertNotSame(bean1, bean2);
    }
    
    // Simple test bean
    static class SimpleBean {
        private String id = java.util.UUID.randomUUID().toString();
        
        public String getId() {
            return id;
        }
    }
}